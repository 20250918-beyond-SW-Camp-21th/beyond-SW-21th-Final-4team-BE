import os
import logging
from typing import List
from fastapi import APIRouter, HTTPException
from database import get_vectorstore
from models import (
    EmployerRecommendationResponse, FreelancerMatch, FreelancerMatchList, RecommendationRequest,
    FreelancerRecommendRequest, JobMatch, JobMatchList, FreelancerRecommendationResponse
)
from langchain_upstage import ChatUpstage
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.documents import Document 
router = APIRouter(prefix="/api/v1", tags=["Recommendation"]) 
logger = logging.getLogger(__name__)

_vectorstore = None
_llm = None

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


        experienced_docs = await vs.as_retriever(search_kwargs={
            "k": 10, 
            "filter": {
                "$and": [
                    {"type": "experience"},
                    {"status": {"$ne": "CONTRACTING"}} 
                ]
            }
        }).ainvoke(search_query)

        newbie_docs = await vs.as_retriever(search_kwargs={
            "k": 5, 
            "filter": {
                "$and": [
                    {"type": "new_profile"},
                    {"status": {"$ne": "CONTRACTING"}}
                ]
            }
        }).ainvoke(search_query)

        all_context = "\n\n".join([
            f"ID: {d.metadata.get('ref_id', 'N/A')}\n{d.page_content}" 
            for d in (experienced_docs[:5] + newbie_docs[:2])
        ])

        prompt = ChatPromptTemplate.from_template("""
        전문 헤드헌터로서 다음 유저 중 공고에 가장 적합한 7명을 추천하세요.
        (숙련자 5명, 신입/신규 2명을 가급적 포함하되 적합도가 중요함)
        <context>{context}</context>
        """)
        
        result = await structured_llm.ainvoke(prompt.format(context=all_context))
        return {"success": True, "data": result.matches[:7]}
    except Exception as e:
        logger.exception("Employer Recommendation Error")
        raise HTTPException(status_code=500, detail="Internal error")

@router.post("/sync/data")
async def sync_single_data(data: dict):
    try:
        vs = get_vs()
        
        doc = Document(
            page_content=data.get('content', ''),
            metadata={
                "id": data.get('id'), 
                "type": data.get('type'), 
                "ref_id": data.get('id'),
                "status": data.get('status', 'POTENTIAL')
            }
        )
        
        if data.get('type') == "experience":
            prefix = "exp"
        elif data.get('type') == "job_posting":
            prefix = "job"
        else:
            prefix = "user"
            
        vs.add_documents([doc], ids=[f"{prefix}:{data['id']}"])
        
        logger.info(f"Sync Success: {prefix}:{data['id']} | Status: {data.get('status')}")
        return {"success": True}
    except Exception as e:
        logger.exception("Sync Error")
        raise HTTPException(status_code=500, detail="Sync failed")
    

@router.post("/freelancer/recommendations", response_model=FreelancerRecommendationResponse)
async def get_freelancer_recommendations(req: FreelancerRecommendRequest):
    try:
        llm = get_llm()
        vs = get_vs()
        
        structured_llm = llm.with_structured_output(JobMatchList)

        prompt = ChatPromptTemplate.from_template("""
        당신은 IT 전문 커리어 코치입니다. 프리랜서의 역량을 분석하여 가장 적합한 프로젝트 공고를 추천하세요.
        - 최대 5개 추천 / 사유 금지 / 데이터만 추출 (ID, 제목, 점수)
        
        프리랜서 기술: {skills}
        프리랜서 경력: {experience}
        <context>{context}</context>
        """)

        experience = req.experience.strip() if req.experience and req.experience.strip() else "(경력 및 소개 없음)"
        search_query = f"{req.skills} {experience}"
        retriever = vs.as_retriever(search_kwargs={
            "k": 15,
            "filter": {"type": "job_posting"}
        }) 
        
        docs = await retriever.ainvoke(search_query) 
        context = "\n\n".join(
            f"ID: {d.metadata.get('ref_id', 'N/A')}\n{d.page_content}" 
            for d in docs
        )
        
        formatted_prompt = prompt.format(skills=req.skills, experience=experience, context=context)
        result = await structured_llm.ainvoke(formatted_prompt)

        return {"success": True, "data": result.matches[:5]}

    except HTTPException:
        raise
    except Exception as e:
        logger.exception("Freelancer Recommendation Internal Error")
        raise HTTPException(status_code=500, detail="Internal server error") from e