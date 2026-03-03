import os
import logging
from typing import List, Optional
from fastapi import APIRouter, HTTPException
from database import get_vectorstore
from models import EmployerRecommendationResponse, FreelancerMatch, FreelancerMatchList, RecommendationRequest
from langchain_upstage import ChatUpstage
from langchain_core.prompts import ChatPromptTemplate

router = APIRouter(prefix="/api/v1/employer", tags=["Recommendation"])
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

@router.post("/recommendations", response_model=EmployerRecommendationResponse)
async def get_job_recommendations(req: RecommendationRequest):
    try:
        llm = get_llm()
        vs = get_vs()
        
        structured_llm = llm.with_structured_output(FreelancerMatchList)

        prompt = ChatPromptTemplate.from_template("""
        당신은 IT 전문 헤드헌터입니다. <context> 내의 프리랜서 데이터를 분석하여 공고에 적합한 인재를 추천하세요.
        - 최대 7명 추천 / 사유 금지 / 데이터만 추출
        
        공고 제목: {title}
        공고 내용: {description}
        <context>{context}</context>
        """)

        search_query = f"{req.title} {req.description}"
        retriever = vs.as_retriever(search_kwargs={"k": 15}) 
        
        docs = await retriever.ainvoke(search_query) 
        context = "\n\n".join(doc.page_content for doc in docs)
        
        formatted_prompt = prompt.format(title=req.title, description=req.description, context=context)
        result = await structured_llm.ainvoke(formatted_prompt)

        return {"success": True, "data": result.matches[:7]}

    except HTTPException:
        raise
    except Exception as e:
        logger.exception("AI Recommendation Internal Error")
        raise HTTPException(status_code=500, detail="Internal server error") from e