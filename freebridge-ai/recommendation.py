import os
from fastapi import APIRouter, HTTPException
from database import get_vectorstore
from models import EmployerRecommendationResponse, FreelancerMatch
from langchain_upstage import ChatUpstage
from langchain_core.prompts import ChatPromptTemplate
from operator import itemgetter

router = APIRouter(prefix="/api/v1/employer", tags=["Recommendation"])

@router.get("/jobs/{jobId}/recommendations", response_model=EmployerRecommendationResponse)
async def get_job_recommendations(jobId: int):
    try:
        # 1. 벡터 스토어 및 LLM 설정
        vectorstore = get_vectorstore()
        llm = ChatUpstage(api_key=os.getenv("UPSTAGE_API_KEY"))
        
        # [핵심] Structured Output 설정 (미사여구 차단)
        # 추천 사유는 빼고 FreelancerMatch 리스트만 받도록 강제함
        structured_llm = llm.with_structured_output(List[FreelancerMatch])

        # 2. RAG 프롬프트 (사유는 생략하고 JSON 형식에만 집중하도록 지시)
        prompt = ChatPromptTemplate.from_template("""
        당신은 채용 전문가입니다. 제공된 <context> 내의 프리랜서 경력 데이터를 분석하여 
        현재 공고에 가장 적합한 사람 3명을 선정하세요.
        설명은 절대 하지 말고, 오직 각 프리랜서의 ID, 이름, 적합도 점수만 데이터로 추출하세요.

        <context>
        {context}
        </context>
        
        선정 기준: 기술 스택 일치도 및 유사 프로젝트 수행 경험
        """)

        # 3. 체인 생성 (Spring에서 넘겨준 jobId를 기반으로 검색한다고 가정)
        # 실제로는 Spring에서 공고 텍스트를 파라미터로 던져주는 게 더 편함
        # 여기서는 예시로 "Java 백엔드 개발"이라는 쿼리를 쓴다고 가정할게
        search_query = "Java Spring Boot 백엔드 개발자" # 실제론 DB에서 jobId로 조회한 제목/내용이 들어감
        
        retriever = vectorstore.as_retriever(search_kwargs={"k": 5})
        docs = retriever.invoke(search_query)
        context = "\n\n".join(doc.page_content for doc in docs)
        
        # 4. LLM 호출
        formatted_prompt = prompt.format(context=context)
        recommendations = structured_llm.invoke(formatted_prompt)

        return {
            "success": True,
            "data": recommendations
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))