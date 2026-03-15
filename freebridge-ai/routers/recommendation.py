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
        for token in re.split(r"[,/|\\s]+", skills_text)
        if token and token.strip()
    ]


def _count_skill_overlap(doc, skill_tokens):
    if not skill_tokens:
        return 0
    page_content = (doc.page_content or "").lower()
    return sum(1 for token in skill_tokens if token in page_content)


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
        search_query = f"{req.title} {description}"

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
            <context>{context}</context>
            """
        )

        result = await structured_llm.ainvoke(
            prompt.format(
                job_title=req.title,
                job_description=description,
                context=all_context,
            )
        )
        filtered_matches = _filter_matches_by_allowed_ids(result.matches, allowed_ids)
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

        experience = req.experience.strip() if req.experience and req.experience.strip() else "(경력 정보 없음)"
        search_query = f"{req.skills} {experience}"
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

        skill_tokens = _extract_skill_tokens(req.skills)
        if skill_tokens:
            scored_docs = [
                (doc, _count_skill_overlap(doc, skill_tokens))
                for doc in docs
            ]
            overlapping_docs = [doc for doc, score in scored_docs if score > 0]
            if overlapping_docs:
                docs = [
                    doc
                    for doc, _ in sorted(
                        scored_docs,
                        key=lambda item: item[1],
                        reverse=True,
                    )
                    if _ > 0
                ]

        allowed_ids = {
            ref_id
            for ref_id in (_parse_ref_id(doc.metadata.get("ref_id")) for doc in docs)
            if ref_id is not None
        }
        context = "\n\n".join(
            f"JOB_ID: {doc.metadata.get('ref_id', 'N/A')}\n{doc.page_content}"
            for doc in docs
        )

        formatted_prompt = prompt.format(skills=req.skills, experience=experience, context=context)
        result = await structured_llm.ainvoke(formatted_prompt)
        filtered_matches = _filter_matches_by_allowed_ids(result.matches, allowed_ids)

        return {"success": True, "data": filtered_matches[:5]}
    except HTTPException:
        raise
    except Exception as e:
        logger.exception("Freelancer Recommendation Internal Error")
        raise HTTPException(status_code=500, detail="Internal server error") from e
