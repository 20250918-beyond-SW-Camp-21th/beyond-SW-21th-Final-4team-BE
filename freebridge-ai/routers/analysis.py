import os
from typing import List
from pydantic import BaseModel, Field
import pymysql
import logging
from fastapi import APIRouter, HTTPException
from fastapi.concurrency import run_in_threadpool
from langchain_upstage import ChatUpstage
from langchain_core.prompts import ChatPromptTemplate

router = APIRouter(prefix="", tags=["Analysis"])
logger = logging.getLogger(__name__)

# --- Pydantic Models ---

class ScoreDto(BaseModel):
    name: str = Field(description="항목 이름 (예: 전문성, 의사소통, 일정준수 등)")
    score: int = Field(ge=1, le=5, description="해당 항목의 평가 점수 (1~5점)")

class FreelancerAiReputationReportDto(BaseModel):
    summary: str = Field(description="전체 평가를 종합한 2~3줄 요약 평판")
    strengths: List[str] = Field(description="리뷰에서 두드러지는 주요 강점 (최대 3개)")
    weaknesses: List[str] = Field(description="리뷰에서 두드러지는 주요 약점 또는 개선점 (최대 3개)")
    technicalScores: List[ScoreDto] = Field(description="기술적 역량(개발 실력, 버그 해결 등) 관련 세부 점수 평가")
    softSkills: List[ScoreDto] = Field(description="소프트스킬(의사소통, 분위기 등) 관련 세부 점수 평가")

class ReputationAnalysisRequest(BaseModel):
    scores: List[int]
    reviews: List[str]

class ReputationAnalysisResponse(BaseModel):
    summary: str = Field(description="전체 리뷰의 단순 요약")
    positive_keywords: List[str] = Field(description="리뷰에서 추출된 긍정 키워드 리스트")
    negative_keywords: List[str] = Field(description="리뷰에서 추출된 부정 키워드 리스트")

# --- Helper Functions ---

_llm = None

def get_llm():
    global _llm
    if _llm is None:
        api_key = os.getenv("UPSTAGE_API_KEY")
        if not api_key:
            raise HTTPException(status_code=500, detail="UPSTAGE_API_KEY is missing")
        _llm = ChatUpstage(api_key=api_key)
    return _llm

def truncate_text(text: str, max_length: int = 5000) -> str:
    """긴 텍스트를 LLM context limit에 맞춰 자릅니다."""
    if len(text) > max_length:
        return text[:max_length] + "...(중략)"
    return text

def get_db_connection():
    db_host = os.getenv("DB_HOST", "localhost")
    db_port = int(os.getenv("DB_PORT", 3306))
    db_user = os.getenv("DB_USER")
    db_password = os.getenv("DB_PASSWORD")
    db_name = os.getenv("DB_NAME")
    
    return pymysql.connect(
        host=db_host,
        port=db_port,
        user=db_user,
        password=db_password,
        db=db_name,
        charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor
    )

def fetch_freelancer_reviews(freelancer_id: int):
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            # 기업이 프리랜서에게 남긴 리뷰 및 정량 점수 조회
            sql = """
                SELECT description, language, framework, debugging, communication, schedule, dispute 
                FROM employer_freelancer_reviews 
                WHERE freelancer_id = %s AND status = 'ACTIVE' LIMIT 50
            """
            cursor.execute(sql, (freelancer_id,))
            return cursor.fetchall()
    finally:
        conn.close()

# --- Endpoints ---

@router.get("/api/v1/analysis/freelancer/{freelancer_id}", response_model=FreelancerAiReputationReportDto)
async def analyze_freelancer_reputation(freelancer_id: int):
    """(Feature 3) 특정 프리랜서의 DB 리뷰를 긁어 평판 분석"""
    try:
        # non-blocking DB call
        rows = await run_in_threadpool(fetch_freelancer_reviews, freelancer_id)

        if not rows:
            return FreelancerAiReputationReportDto(
                summary="아직 충분한 리뷰가 등록되지 않았습니다.",
                strengths=[],
                weaknesses=[],
                technicalScores=[],
                softSkills=[]
            )

        # 텍스트 합치기 및 제한
        review_texts = []
        detailed_scores = {"language": [], "framework": [], "debugging": [], "communication": [], "schedule": [], "dispute": []}
        
        for row in rows:
            if row.get('description'):
                review_texts.append(row['description'])
            for key in detailed_scores.keys():
                if row.get(key) is not None:
                    detailed_scores[key].append(row[key])
        
        all_reviews = "\n- ".join(review_texts)
        all_reviews = f"- {all_reviews}"
        truncated_reviews = truncate_text(all_reviews, max_length=5000)

        # Calculate averages for context
        avg_scores_context = "저장된 항목별 평균 원본 데이터 (1~5점):\n"
        for key, scores in detailed_scores.items():
            if scores:
                avg = sum(scores) / len(scores)
                avg_scores_context += f"- {key}: {avg:.1f} / 5\n"

        llm = get_llm()
        structured_llm = llm.with_structured_output(FreelancerAiReputationReportDto)
        
        prompt = ChatPromptTemplate.from_template("""
        당신은 IT 프리랜서 커리어 코치 및 전문 헤드헌터입니다.
        아래는 특정 프리랜서가 과거 클라이언트들(기업)로부터 받은 리뷰 텍스트와 실제 DB에 저장된 각 항목별 정량적 통계 수치입니다.
        AI는 1~5점의 세부 역량(technicalScores, softSkills)의 점수를 임의로 지어내지 말고, 주어진 [저장된 평균 원본 데이터]를 기반으로 반올림하여 ScoreDto를 출력해야 합니다 (모든 항목이 포함되지 않아도 되며, 대표 항목만 뽑아도 됩니다. 단, score 값은 1에서 5 사이여야 합니다).
        또한 리뷰 텍스트들을 바탕으로 종합적인 평판을 요약해주고, 강점과 약점을 추출해 주세요.
        
        [저장된 평균 원본 데이터]
        {avg_scores}

        <reviews>
        {reviews}
        </reviews>
        """)

        result = await structured_llm.ainvoke(prompt.format(reviews=truncated_reviews, avg_scores=avg_scores_context))
        return result

    except Exception as e:
        logger.exception(f"Error analyzing freelancer {freelancer_id} reputation")
        raise HTTPException(status_code=500, detail="Internal server error")

@router.post("/ai/analyze-reputation", response_model=ReputationAnalysisResponse)
async def analyze_general_reputation(request: ReputationAnalysisRequest):
    """(Feature 4) 넘겨받은 리뷰 배열을 단순 요약 (기업용/공통용)"""
    try:
        if not request.reviews:
            return ReputationAnalysisResponse(
                summary="분석할 리뷰가 없습니다.",
                positive_keywords=[],
                negative_keywords=[]
            )

        # 리뷰 텍스트 전처리 (최대 개수 50개 / 길이 5000자 제한)
        reviews_to_analyze = request.reviews[:50]
        combined_text = "\n- ".join(reviews_to_analyze)
        truncated_text = truncate_text(f"- {combined_text}", max_length=5000)

        # 평균 점수 계산 (컨텍스트로 제공)
        avg_score = sum(request.scores) / len(request.scores) if request.scores else 0.0

        llm = get_llm()
        structured_llm = llm.with_structured_output(ReputationAnalysisResponse)

        prompt = ChatPromptTemplate.from_template("""
        아래는 특정 유저(또는 기업)에 대한 리뷰 내역입니다. 
        제공된 리뷰를 바탕으로 종합적인 요약을 제공하고, 주된 긍정 키워드와 부정 키워드를 추출해주세요.
        전체 평균 점수(요약에 참고): {avg_score:.1f}/5.0
        
        <reviews>
        {reviews}
        </reviews>
        """)

        result = await structured_llm.ainvoke(prompt.format(avg_score=avg_score, reviews=truncated_text))
        return result

    except Exception as e:
        logger.exception("Error analyzing general reputation")
        raise HTTPException(status_code=500, detail="Internal server error")
