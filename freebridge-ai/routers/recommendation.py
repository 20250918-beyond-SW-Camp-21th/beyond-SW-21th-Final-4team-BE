import logging
import os
import re

from fastapi import APIRouter, HTTPException
from langchain_core.documents import Document
from langchain_core.prompts import ChatPromptTemplate
from langchain_upstage import ChatUpstage

from database import get_vectorstore
from models import (
    EmployerRecommendationResponse,
    FreelancerMatchList,
    FreelancerRecommendationResponse,
    FreelancerRecommendRequest,
    JobMatchList,
    RecommendationRequest,
)

router = APIRouter(prefix="/api/v1", tags=["Recommendation"])
logger = logging.getLogger(__name__)

_vectorstore = None
_llm = None
_SYNC_TYPE_PREFIX = {
    "experience": "exp",
    "job_posting": "job",
    "new_profile": "profile",
}


def _parse_ref_id(value):
    try:
        if value is None:
            return None
        return int(value)
    except (TypeError, ValueError):
        return None


def _filter_matches_by_allowed_ids(matches, allowed_ids):
    return [match for match in matches if getattr(match, "id", None) in allowed_ids]


def _extract_skill_tokens(skills_text):
    if not skills_text:
        return []
    return [
        token.lower()
        for token in re.split(r"[,/|\s]+", skills_text)
        if token and token.strip()
    ]


def _count_skill_overlap(doc, skill_tokens):
    if not skill_tokens:
        return 0
    document_tokens = {
        token.lower()
        for token in re.split(r"[^0-9A-Za-z가-힣+#.]+", doc.page_content or "")
        if token and token.strip()
    }
    return sum(1 for token in skill_tokens if token in document_tokens)


def _describe_docs(docs):
    return [
        {
            "id": doc.metadata.get("id"),
            "ref_id": doc.metadata.get("ref_id"),
            "type": doc.metadata.get("type"),
            "status": doc.metadata.get("status"),
        }
        for doc in docs
    ]


def _describe_matches(matches):
    return [
        {
            "id": getattr(match, "id", None),
            "score": getattr(match, "matchScore", None),
        }
        for match in matches
    ]


def _match_score_or_zero(match):
    score = getattr(match, "matchScore", None)
    try:
        return float(score) if score is not None else 0.0
    except (TypeError, ValueError):
        return 0.0


def _sort_matches_by_score(matches):
    return sorted(matches, key=_match_score_or_zero, reverse=True)


def mask_profile(value, visible=2, limit=24):
    if value is None:
        return None
    normalized = re.sub(r"\s+", " ", str(value)).strip()
    if not normalized:
        return normalized
    visible_prefix = min(visible, max(0, len(normalized) - 1))
    visible_suffix = min(visible, max(0, len(normalized) - visible_prefix - 1))
    desired_mask_count = min(8, max(0, len(normalized) - visible_prefix - visible_suffix))
    masked_count = max(1, desired_mask_count)

    masked = (
        normalized[:visible_prefix]
        + "*" * masked_count
        + (normalized[-visible_suffix:] if visible_suffix > 0 else "")
    )
    return masked[:limit] + ("..." if len(masked) > limit else "")


def mask_tokens(tokens, limit=10):
    return [mask_profile(token, visible=1, limit=12) for token in tokens[:limit]]


def get_llm():
    global _llm
    if _llm is None:
        api_key = os.getenv("UPSTAGE_API_KEY")
        if not api_key:
            raise HTTPException(status_code=500, detail="UPSTAGE_API_KEY is missing")
        _llm = ChatUpstage(api_key=api_key)
    return _llm


def get_vs():
    global _vectorstore
    if _vectorstore is None:
        _vectorstore = get_vectorstore()
    return _vectorstore


@router.post("/employer/recommendations", response_model=EmployerRecommendationResponse)
async def get_job_recommendations(req: RecommendationRequest):
    try:
        vs = get_vs()
        llm = get_llm()
        structured_llm = llm.with_structured_output(FreelancerMatchList)

        description = req.description.strip() if req.description and req.description.strip() else "(상세 내용 없음)"
        skills = req.skills.strip() if req.skills and req.skills.strip() else ""
        job_skills = skills if skills else "(skills missing)"
        search_query = f"{req.title} {description} {skills}".strip()
        logger.info(
            "Employer recommendation request. job_id=%s",
            req.jobId,
        )
        logger.debug(
            "Employer recommendation request detail. job_id=%s title=%s skills=%s query=%s",
            req.jobId,
            mask_profile(req.title),
            mask_profile(skills, limit=30),
            mask_profile(search_query, limit=40),
        )

        experienced_docs = await vs.as_retriever(
            search_kwargs={
                "k": 10,
                "filter": {
                    "$and": [
                        {"type": "experience"},
                        {"status": {"$ne": "CONTRACTING"}},
                    ]
                },
            }
        ).ainvoke(search_query)

        newbie_docs = await vs.as_retriever(
            search_kwargs={
                "k": 5,
                "filter": {
                    "$and": [
                        {"type": "new_profile"},
                        {"status": {"$ne": "CONTRACTING"}},
                    ]
                },
            }
        ).ainvoke(search_query)

        candidate_docs = experienced_docs[:5] + newbie_docs[:2]
        if not candidate_docs:
            return {"success": True, "data": []}
        logger.info(
            "Employer recommendation candidates. job_id=%s experienced_count=%s newbie_count=%s merged_count=%s",
            req.jobId,
            len(experienced_docs[:5]),
            len(newbie_docs[:2]),
            len(candidate_docs),
        )
        logger.debug(
            "Employer recommendation candidate detail. job_id=%s experienced=%s newbie=%s merged=%s",
            req.jobId,
            _describe_docs(experienced_docs[:5]),
            _describe_docs(newbie_docs[:2]),
            _describe_docs(candidate_docs),
        )
        allowed_ids = {
            ref_id
            for ref_id in (_parse_ref_id(doc.metadata.get("ref_id")) for doc in candidate_docs)
            if ref_id is not None
        }
        all_context = "\n\n".join(
            f"CANDIDATE_ID: {doc.metadata.get('ref_id', 'N/A')}\n{doc.page_content}"
            for doc in candidate_docs
        )

        prompt = ChatPromptTemplate.from_template(
            """
            당신은 채용 공고에 가장 적합한 프리랜서를 추천하는 전문 헤드헌터입니다.
            반드시 context에 포함된 후보자만 사용하세요.
            ID, 이름, 가상의 인물을 새로 만들지 마세요.
            반환하는 id는 반드시 context 안의 CANDIDATE_ID 값 중 하나와 정확히 일치해야 합니다.
            context에 없는 후보자는 절대 반환하지 마세요.
            적합도 순으로 최대 7명을 추천하세요.
            현재 공고 제목: {job_title}
            현재 공고 상세 내용: {job_description}
            Current job skills: {job_skills}
            <context>{context}</context>
            """
        )

        result = await structured_llm.ainvoke(
            prompt.format(
                job_title=req.title,
                job_description=description,
                job_skills=job_skills,
                context=all_context,
            )
        )
        filtered_matches = _sort_matches_by_score(
            _filter_matches_by_allowed_ids(result.matches, allowed_ids)
        )
        logger.info(
            "Employer recommendation result. job_id=%s allowed_count=%s raw_count=%s filtered_count=%s",
            req.jobId,
            len(allowed_ids),
            len(result.matches),
            len(filtered_matches[:7]),
        )
        logger.debug(
            "Employer recommendation result detail. job_id=%s allowed_ids=%s raw=%s filtered=%s",
            req.jobId,
            sorted(allowed_ids),
            _describe_matches(result.matches),
            _describe_matches(filtered_matches[:7]),
        )
        return {"success": True, "data": filtered_matches[:7]}
    except Exception as e:
        logger.exception("Employer Recommendation Error")
        raise HTTPException(status_code=500, detail="Internal error") from e


@router.post("/sync/data")
async def sync_single_data(data: dict):
    try:
        vs = get_vs()
        id_val = data.get("id")
        if not id_val or (isinstance(id_val, str) and not id_val.strip()):
            logger.error(
                "Sync skipped because id is missing. type=%s, ref_id=%s",
                data.get("type"),
                data.get("refId", data.get("ref_id")),
            )
            raise HTTPException(status_code=400, detail="id is required")

        ref_id = _parse_ref_id(data.get("refId", data.get("ref_id")))
        if ref_id is None:
            logger.error(
                "Sync skipped because refId is missing or invalid. type=%s, id=%s",
                data.get("type"),
                id_val,
            )
            raise HTTPException(status_code=400, detail="refId is required")

        doc = Document(
            page_content=data.get("content", ""),
            metadata={
                "id": id_val,
                "type": data.get("type"),
                "ref_id": ref_id,
                "status": data.get("status", "POTENTIAL"),
            },
        )

        data_type = data.get("type")
        prefix = _SYNC_TYPE_PREFIX.get(data_type)
        if prefix is None:
            logger.error("Sync skipped because type is missing or invalid. type=%s, id=%s", data_type, id_val)
            raise HTTPException(status_code=400, detail="type is invalid")

        await vs.aadd_documents([doc], ids=[f"{prefix}:{id_val}"])

        logger.info(
            "Sync Success: %s:%s | ref_id=%s | Status=%s",
            prefix,
            id_val,
            ref_id,
            data.get("status"),
        )
        return {"success": True}
    except Exception as e:
        logger.exception("Sync Error")
        if isinstance(e, HTTPException):
            raise
        raise HTTPException(status_code=500, detail="Sync failed") from e


@router.post("/freelancer/recommendations", response_model=FreelancerRecommendationResponse)
async def get_freelancer_recommendations(req: FreelancerRecommendRequest):
    try:
        llm = get_llm()
        vs = get_vs()
        structured_llm = llm.with_structured_output(JobMatchList)

        prompt = ChatPromptTemplate.from_template(
            """
            당신은 프리랜서에게 적합한 공고를 추천하는 IT 커리어 코치입니다.
            반드시 context에 포함된 공고만 사용하세요.
            ID나 제목을 새로 만들지 마세요.
            반환하는 id는 반드시 context 안의 JOB_ID 값 중 하나와 정확히 일치해야 합니다.
            적합도 순으로 최대 5개의 공고를 추천하세요.

            프리랜서 보유 기술: {skills}
            프리랜서 경력: {experience}
            <context>{context}</context>
            """
        )

        skills = req.skills.strip() if req.skills and req.skills.strip() else ""
        experience = req.experience.strip() if req.experience and req.experience.strip() else ""
        prompt_skills = skills if skills else "(skills missing)"
        prompt_experience = experience if experience else "(experience missing)"
        search_query = " ".join(part for part in [skills, experience] if part).strip()
        if not search_query:
            search_query = "IT freelancer project"

        logger.info(
            "Freelancer recommendation request. freelancer_id=%s",
            req.freelancerId,
        )
        logger.debug(
            "Freelancer recommendation request detail. freelancer_id=%s raw_skills=%s raw_experience=%s query=%s",
            req.freelancerId,
            mask_profile(skills, limit=40),
            mask_profile(experience, limit=40),
            mask_profile(search_query, limit=50),
        )
        retriever = vs.as_retriever(
            search_kwargs={
                "k": 15,
                "filter": {
                    "$and": [
                        {"type": "job_posting"},
                        {"status": "ACTIVE"},
                    ]
                },
            }
        )

        docs = await retriever.ainvoke(search_query)
        if not docs:
            return {"success": True, "data": []}
        logger.info(
            "Freelancer recommendation initial docs. freelancer_id=%s doc_count=%s",
            req.freelancerId,
            len(docs),
        )
        logger.debug(
            "Freelancer recommendation initial docs detail. freelancer_id=%s docs=%s",
            req.freelancerId,
            _describe_docs(docs),
        )

        skill_tokens = _extract_skill_tokens(skills)
        logger.debug(
            "Freelancer recommendation skill tokens. freelancer_id=%s token_count=%s tokens=%s",
            req.freelancerId,
            len(skill_tokens),
            mask_tokens(skill_tokens),
        )
        if skill_tokens:
            scored_docs = [
                (doc, _count_skill_overlap(doc, skill_tokens))
                for doc in docs
            ]
            overlapping_docs = [doc for doc, score in scored_docs if score > 0]
            logger.info(
                "Freelancer recommendation overlap summary. freelancer_id=%s overlapping_count=%s",
                req.freelancerId,
                len(overlapping_docs),
            )
            logger.debug(
                "Freelancer recommendation overlap scores. freelancer_id=%s scores=%s",
                req.freelancerId,
                [
                    {
                        "ref_id": doc.metadata.get("ref_id"),
                        "score": score,
                    }
                    for doc, score in scored_docs
                ],
            )
            if not overlapping_docs:
                logger.info(
                    "프리랜서 추천 스킬 overlap 결과가 0건이라 빈 결과를 반환합니다. freelancer_id=%s",
                    req.freelancerId,
                )
                return {"success": True, "data": []}

            docs = [
                doc
                for doc, overlap_score in sorted(
                    scored_docs,
                    key=lambda item: item[1],
                    reverse=True,
                )
                if overlap_score > 0
            ]
            logger.debug(
                "Freelancer recommendation overlap-filtered docs. freelancer_id=%s docs=%s",
                req.freelancerId,
                _describe_docs(docs),
            )

        allowed_ids = {
            ref_id
            for ref_id in (_parse_ref_id(doc.metadata.get("ref_id")) for doc in docs)
            if ref_id is not None
        }
        context = "\n\n".join(
            f"JOB_ID: {doc.metadata.get('ref_id', 'N/A')}\n{doc.page_content}"
            for doc in docs
        )

        formatted_prompt = prompt.format(
            skills=prompt_skills,
            experience=prompt_experience,
            context=context,
        )
        result = await structured_llm.ainvoke(formatted_prompt)
        filtered_matches = _sort_matches_by_score(
            _filter_matches_by_allowed_ids(result.matches, allowed_ids)
        )
        logger.info(
            "Freelancer recommendation result. freelancer_id=%s allowed_count=%s raw_count=%s filtered_count=%s",
            req.freelancerId,
            len(allowed_ids),
            len(result.matches),
            len(filtered_matches[:5]),
        )
        logger.debug(
            "Freelancer recommendation result detail. freelancer_id=%s allowed_ids=%s raw=%s filtered=%s",
            req.freelancerId,
            sorted(allowed_ids),
            _describe_matches(result.matches),
            _describe_matches(filtered_matches[:5]),
        )

        return {"success": True, "data": filtered_matches[:5]}
    except HTTPException:
        raise
    except Exception as e:
        logger.exception("Freelancer Recommendation Internal Error")
        raise HTTPException(status_code=500, detail="Internal server error") from e
