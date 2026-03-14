import logging
import os
from typing import List

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


def _parse_ref_id(value):
    try:
        if value is None:
            return None
        return int(value)
    except (TypeError, ValueError):
        return None


def _filter_matches_by_allowed_ids(matches, allowed_ids):
    return [match for match in matches if getattr(match, "id", None) in allowed_ids]


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
            You are a recruiter selecting the best freelancer candidates for a job posting.
            Use only candidates that appear in the context.
            Never invent IDs, names, or placeholder people.
            The returned id must exactly match one of the CANDIDATE_ID values in the context.
            If a candidate is not in the context, do not return it.
            Return up to 7 candidates ordered by match quality.
            <context>{context}</context>
            """
        )

        result = await structured_llm.ainvoke(prompt.format(context=all_context))
        filtered_matches = _filter_matches_by_allowed_ids(result.matches, allowed_ids)
        return {"success": True, "data": filtered_matches[:7]}
    except Exception:
        logger.exception("Employer Recommendation Error")
        raise HTTPException(status_code=500, detail="Internal error")


@router.post("/sync/data")
async def sync_single_data(data: dict):
    try:
        vs = get_vs()
        ref_id = data.get("refId", data.get("ref_id", data.get("id")))

        doc = Document(
            page_content=data.get("content", ""),
            metadata={
                "id": data.get("id"),
                "type": data.get("type"),
                "ref_id": ref_id,
                "status": data.get("status", "POTENTIAL"),
            },
        )

        if data.get("type") == "experience":
            prefix = "exp"
        elif data.get("type") == "job_posting":
            prefix = "job"
        else:
            prefix = "user"

        vs.add_documents([doc], ids=[f"{prefix}:{data['id']}"])

        logger.info("Sync Success: %s:%s | ref_id=%s | Status=%s", prefix, data["id"], ref_id, data.get("status"))
        return {"success": True}
    except Exception:
        logger.exception("Sync Error")
        raise HTTPException(status_code=500, detail="Sync failed")


@router.post("/freelancer/recommendations", response_model=FreelancerRecommendationResponse)
async def get_freelancer_recommendations(req: FreelancerRecommendRequest):
    try:
        llm = get_llm()
        vs = get_vs()
        structured_llm = llm.with_structured_output(JobMatchList)

        prompt = ChatPromptTemplate.from_template(
            """
            You are an IT career coach recommending job postings for a freelancer.
            Use only job postings that appear in the context.
            Never invent IDs or titles.
            The returned id must exactly match one of the JOB_ID values in the context.
            Return up to 5 jobs ordered by match quality.

            Freelancer skills: {skills}
            Freelancer experience: {experience}
            <context>{context}</context>
            """
        )

        experience = req.experience.strip() if req.experience and req.experience.strip() else "(경력 정보 없음)"
        search_query = f"{req.skills} {experience}"
        retriever = vs.as_retriever(search_kwargs={"k": 15, "filter": {"type": "job_posting"}})

        docs = await retriever.ainvoke(search_query)
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
