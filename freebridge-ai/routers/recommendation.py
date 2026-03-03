import os
import logging
from typing import List
from fastapi import APIRouter, HTTPException
from database import get_vectorstore
from models import EmployerRecommendationResponse, FreelancerMatch, FreelancerMatchList, RecommendationRequest
from langchain_upstage import ChatUpstage
from langchain_core.prompts import ChatPromptTemplate

router = APIRouter(prefix="/api/v1/employer", tags=["Recommendation"])
logger = logging.getLogger(__name__)

_vectorstore = get_vectorstore()
_llm = ChatUpstage(api_key=os.getenv("UPSTAGE_API_KEY"))

@router.post("/recommendations", response_model=EmployerRecommendationResponse)
async def get_job_recommendations(req: RecommendationRequest):
    try:
        
        structured_llm = _llm.with_structured_output(FreelancerMatchList)

        # 1. 프롬프트 설정
        prompt = ChatPromptTemplate.from_template("""
        당신은 IT 전문 헤드헌터입니다. <context> 내의 프리랜서 데이터를 분석하여 공고에 적합한 인재를 추천하세요.
        
        [지시 사항]
        - 최대 7명까지 추천하되, 공고의 기술 스택이나 요구사항과 맞지 않는 인재는 억지로 포함하지 마세요.
        - 적합한 인재가 없다면 빈 리스트([])를 반환해도 됩니다.
        - 인사말, 설명, 사유는 절대 쓰지 말고 오직 데이터(ID, 이름, 점수)만 추출하세요.

        공고 제목: {title}
        공고 내용: {description}

        <context>
        {context}
        </context>
        """)

        search_query = f"{req.title} {req.description}"
        retriever = _vectorstore.as_retriever(search_kwargs={"k": 15}) 
        
        docs = await retriever.ainvoke(search_query) 
        context = "\n\n".join(doc.page_content for doc in docs)
        
        formatted_prompt = prompt.format(
            title=req.title, 
            description=req.description, 
            context=context
        )
        
        result = await structured_llm.ainvoke(formatted_prompt)

        return {
            "success": True,
            "data": result.matches[:7] # 최종적으로 최대 7명 반환
        }

    except HTTPException:
        raise
    except Exception as e:
        logger.exception("AI 추천 처리 중 서버 내부 오류 발생")
        raise HTTPException(status_code=500, detail="Internal server error") from e