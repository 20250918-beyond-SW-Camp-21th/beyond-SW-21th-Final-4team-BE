import os
import ssl
import time
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
MAX_PDF_BYTES = 10 * 1024 * 1024


class ScoreDto(BaseModel):
    name: str = Field(description="항목 이름 (예: 전문성, 의사소통, 일정준수 등)")
    score: int = Field(ge=1, le=5, description="해당 항목의 평가 점수 (1~5점)")

class RawScoreDto(BaseModel):
    name: str
    score: float = Field(ge=1, le=5)

class FreelancerAiReputationReportDto(BaseModel):
    grade: str = Field(description="종합 평판 등급 (예: 'S', 'A', 'B', 'C', 'D')")
    positivityScore: int = Field(ge=0, le=100, description="리뷰 긍정 지수 (0~100점)")
    summary: str = Field(description="전체 평가를 종합한 2~3줄 요약 평판")
    strengths: List[str] = Field(description="리뷰에서 두드러지는 주요 강점 (최대 3개)")
    weaknesses: List[str] = Field(description="리뷰에서 두드러지는 주요 약점 또는 개선점 (최대 3개)")
    technicalScores: List[ScoreDto] = Field(description="기술적 역량(개발 실력, 버그 해결 등) 관련 세부 점수 평가")
    softSkills: List[ScoreDto] = Field(description="소프트스킬(의사소통, 분위기 등) 관련 세부 점수 평가")

class RawFreelancerAiReputationReportDto(BaseModel):
    grade: str
    positivityScore: float = Field(ge=0, le=100)
    summary: str
    strengths: List[str]
    weaknesses: List[str]
    technicalScores: List[RawScoreDto]
    softSkills: List[RawScoreDto]

class ReputationAnalysisRequest(BaseModel):
    scores: List[int]
    reviews: List[str]

class ReputationAnalysisResponse(BaseModel):
    summary: str = Field(description="전체 리뷰의 단순 요약")
    positive_keywords: List[str] = Field(description="리뷰에서 추출된 긍정 키워드 리스트")
    negative_keywords: List[str] = Field(description="리뷰에서 추출된 부정 키워드 리스트")
 

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

def round_score(value: float) -> int:
    rounded = int(value + 0.5)
    return max(1, min(5, rounded))

def round_percent(value: float) -> int:
    rounded = int(value + 0.5)
    return max(0, min(100, rounded))

def normalize_scores(scores: List[RawScoreDto]) -> List[ScoreDto]:
    return [ScoreDto(name=score.name, score=round_score(score.score)) for score in scores]

def normalize_freelancer_report(raw_result: RawFreelancerAiReputationReportDto) -> FreelancerAiReputationReportDto:
    return FreelancerAiReputationReportDto(
        grade=raw_result.grade,
        positivityScore=round_percent(raw_result.positivityScore),
        summary=raw_result.summary,
        strengths=raw_result.strengths,
        weaknesses=raw_result.weaknesses,
        technicalScores=normalize_scores(raw_result.technicalScores),
        softSkills=normalize_scores(raw_result.softSkills),
    )

def get_db_connection():
    db_host = os.getenv("DB_HOST", "localhost")
    db_port = int(os.getenv("DB_PORT", 3306))
    db_user = os.getenv("DB_USER")
    db_password = os.getenv("DB_PASSWORD")
    db_name = os.getenv("DB_NAME")
    ssl_enabled = os.getenv("DB_SSL_ENABLED", "false").lower() in ("true", "1", "yes", "on")
    ssl_options = None

    if ssl_enabled:
        ssl_context = ssl.create_default_context()
        db_ssl_ca = os.getenv("DB_SSL_CA")
        db_ssl_cert = os.getenv("DB_SSL_CERT")
        db_ssl_key = os.getenv("DB_SSL_KEY")
        db_ssl_verify_cert = os.getenv("DB_SSL_VERIFY_CERT", "true").lower() in ("true", "1", "yes", "on")

        if db_ssl_ca:
            ssl_context.load_verify_locations(cafile=db_ssl_ca)
        if db_ssl_cert and db_ssl_key:
            ssl_context.load_cert_chain(certfile=db_ssl_cert, keyfile=db_ssl_key)

        ssl_context.check_hostname = db_ssl_verify_cert
        ssl_context.verify_mode = ssl.CERT_REQUIRED if db_ssl_verify_cert else ssl.CERT_NONE
        ssl_options = ssl_context

    return pymysql.connect(
        host=db_host,
        port=db_port,
        user=db_user,
        password=db_password,
        db=db_name,
        charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor,
        ssl=ssl_options
    )

def fetch_freelancer_reviews(freelancer_id: int):
    conn = get_db_connection()
    try:
        started_at = time.perf_counter()
        with conn.cursor() as cursor:
            sql = """
                SELECT description, language, framework, debugging, communication, schedule, dispute 
                FROM employer_freelancer_reviews 
                WHERE status = 'ACTIVE'
                  AND deleted = false
                  AND freelancer_id = %s
                LIMIT 50
            """
            cursor.execute(sql, (freelancer_id,))
            rows = cursor.fetchall()
            elapsed = time.perf_counter() - started_at
            logger.info(
                "DB에서 프리랜서 리뷰를 조회했습니다. freelancer_id=%s row_count=%s elapsed_ms=%s",
                freelancer_id,
                len(rows),
                round(elapsed * 1000),
            )
            return rows
    finally:
        conn.close()


@router.get("/api/v1/analysis/freelancer/{freelancer_id}", response_model=FreelancerAiReputationReportDto)
async def analyze_freelancer_reputation(freelancer_id: int):
    """(Feature 3) 특정 프리랜서의 DB 리뷰를 긁어 평판 분석"""
    try:
        logger.info("Freelancer analysis requested. freelancer_id=%s", freelancer_id)
        rows = await run_in_threadpool(fetch_freelancer_reviews, freelancer_id)
        logger.info(
            "Freelancer analysis loaded rows. freelancer_id=%s row_count=%s",
            freelancer_id,
            len(rows),
        )

        if not rows:
            logger.info(
                "Freelancer analysis returning empty report because no rows were found. freelancer_id=%s",
                freelancer_id,
            )
            return FreelancerAiReputationReportDto(
                grade="D",
                positivityScore=0,
                summary="아직 충분한 리뷰가 등록되지 않았습니다.",
                strengths=[],
                weaknesses=[],
                technicalScores=[],
                softSkills=[]
            )

        review_texts = []
        detailed_scores = {"language": [], "framework": [], "debugging": [], "communication": [], "schedule": [], "dispute": []}
        
        for row in rows:
            if row.get('description'):
                review_texts.append(row['description'])
            for key in detailed_scores.keys():
                if row.get(key) is not None:
                    detailed_scores[key].append(row[key])
        logger.info(
            "Freelancer analysis prepared review payload. freelancer_id=%s review_text_count=%s score_counts=%s",
            freelancer_id,
            len(review_texts),
            {key: len(scores) for key, scores in detailed_scores.items()},
        )
        
        all_reviews = "\n- ".join(review_texts)
        all_reviews = f"- {all_reviews}"
        truncated_reviews = truncate_text(all_reviews, max_length=5000)

        avg_scores_context = "저장된 항목별 평균 원본 데이터 (1~5점):\n"
        for key, scores in detailed_scores.items():
            if scores:
                avg = sum(scores) / len(scores)
                avg_scores_context += f"- {key}: {avg:.1f} / 5\n"

        llm = get_llm()
        structured_llm = llm.with_structured_output(RawFreelancerAiReputationReportDto)
        
        prompt = ChatPromptTemplate.from_template("""
        당신은 IT 프리랜서 커리어 코치 및 전문 헤드헌터입니다.
        아래는 특정 프리랜서가 과거 클라이언트들(기업)로부터 받은 평가 텍스트와 실제 DB에 저장된 각 항목별 정량적 통계 수치입니다.

        [지시사항]
        1. [저장된 평균 원본 데이터]를 기반으로 `technicalScores` 및 `softSkills` 배열을 채워주세요. 절대 여기에 없는 가상의 평가 지표나 임의의 점수를 만들어내지 마세요.
        2. 제공된 리뷰 텍스트를 종합하여, 프리랜서의 인성, 태도, 실력 등을 2~3줄로 명확하게 요약(`summary`)해 주세요.
        3. `positivityScore`는 리뷰의 전체적인 긍정/부정 비율을 고려하여 0~100점 사이로 평가하세요.
        4. `grade`는 `positivityScore`에 기반해 다음 기준을 엄격히 따르세요 (S: 95~100, A: 85~94, B: 70~84, C: 50~69, D: 0~49).
        5. 리뷰에서 반복적으로 언급되는 칭찬 사항을 `strengths`에, 아쉬운 점이나 개선 요망 사항을 `weaknesses`에 최대 3개씩 간결하게(명사형 종결 등) 추출하세요.

        [저장된 평균 원본 데이터]
        {avg_scores}

        <리뷰>
        {reviews}
        </리뷰>
        """)

        raw_result = await structured_llm.ainvoke(prompt.format(reviews=truncated_reviews, avg_scores=avg_scores_context))
        result = normalize_freelancer_report(raw_result)
        logger.info(
            "Freelancer analysis completed. freelancer_id=%s positivity_score=%s strengths_count=%s weaknesses_count=%s",
            freelancer_id,
            result.positivityScore,
            len(result.strengths),
            len(result.weaknesses),
        )
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

        reviews_to_analyze = request.reviews[:50]
        combined_text = "\n- ".join(reviews_to_analyze)
        truncated_text = truncate_text(f"- {combined_text}", max_length=5000)

        avg_score = sum(request.scores) / len(request.scores) if request.scores else 0.0

        llm = get_llm()
        structured_llm = llm.with_structured_output(ReputationAnalysisResponse)

        prompt = ChatPromptTemplate.from_template("""
        아래는 특정 유저(또는 기업)에 대한 리뷰 내역입니다. 
        제공된 리뷰를 바탕으로 종합적인 요약을 제공하고, 주된 긍정 키워드와 부정 키워드를 추출해주세요.
        전체 평균 점수(요약에 참고): {avg_score:.1f}/5.0
        
        <리뷰>
        {reviews}
        </리뷰>
        """)

        result = await structured_llm.ainvoke(prompt.format(avg_score=avg_score, reviews=truncated_text))
        return result

    except Exception as e:
        logger.exception("Error analyzing general reputation")
        raise HTTPException(status_code=500, detail="Internal server error")

class ContractAnalysisResponse(BaseModel):
    summary: str = Field(description="계약서의 전반적인 요약")
    toxic_clauses: List[str] = Field(description="프리랜서에게 불리할 수 있는 독소 조항 또는 위법 의심 사항 (없으면 빈 배열)")
    recommendations: List[str] = Field(description="계약 체결 전 확인해야 할 권장 사항 또는 조언")

import httpx
from fastapi import UploadFile, File

@router.post("/api/v1/analysis/contract", response_model=ContractAnalysisResponse)
async def analyze_contract(file: UploadFile = File(...)):
    """(Feature 5) 계약서 PDF 파일을 받아 Upstage Document Parse API를 거쳐 법률 위반/독소 조항 분석"""
    try:
        started_at = time.perf_counter()
        api_key = os.getenv("UPSTAGE_API_KEY")
        if not api_key:
            raise HTTPException(status_code=500, detail="UPSTAGE_API_KEY is missing")

        if not file.filename.lower().endswith('.pdf'):
            raise HTTPException(status_code=400, detail="Only PDF files are supported")

        file.file.seek(0, 2)
        file_size = file.file.tell()
        if file_size <= 0:
            raise HTTPException(status_code=400, detail="Empty PDF files are not supported")
        if file_size > MAX_PDF_BYTES:
            raise HTTPException(status_code=413, detail="PDF file is too large")

        file.file.seek(0)
        header = file.file.read(4)
        if header != b"%PDF":
            raise HTTPException(status_code=400, detail="Invalid PDF file")

        file.file.seek(0)
        file_content = await file.read()
        logger.info(
            "계약 분석 요청을 수신했습니다. filename=%s size=%s content_type=%s",
            file.filename,
            len(file_content),
            file.content_type,
        )

        parse_url = "https://api.upstage.ai/v1/document-ai/document-parse"
        headers = {"Authorization": f"Bearer {api_key}"}
        files = {"document": (file.filename, file_content, file.content_type)}
        
        parse_started_at = time.perf_counter()
        async with httpx.AsyncClient(timeout=60.0) as client:
            response = await client.post(parse_url, headers=headers, files=files)
        logger.info(
            "Upstage 계약서 파싱이 완료되었습니다. filename=%s status=%s elapsed_ms=%s",
            file.filename,
            response.status_code,
            round((time.perf_counter() - parse_started_at) * 1000),
        )
            
        if response.status_code != 200:
            logger.error(f"Upstage Document Parse API failed: {response.text}")
            raise HTTPException(status_code=500, detail="Failed to parse document via Upstage API")

        parse_result = response.json()
        
        parsed_text = ""
        if "elements" in parse_result:
            for element in parse_result["elements"]:
                parsed_text += element.get("content", {}).get("html", "") or element.get("text", "") or ""
                parsed_text += "\n"
        elif "text" in parse_result:
            parsed_text = parse_result["text"]
        else:
            parsed_text = str(parse_result)
        
        truncated_text = truncate_text(parsed_text, max_length=15000)
        llm = get_llm()
        structured_llm = llm.with_structured_output(ContractAnalysisResponse)

        prompt = ChatPromptTemplate.from_template("""
        당신은 노무사 및 IT 계약 법률 전문가입니다.
        아래는 OCR/Document Parse 를 통해 추출된 프리랜서-기업 간 계약서 내용입니다.

        [지시사항]
        1. 계약서의 핵심 내용(계약금액, 기간, 주요 업무 등)을 2~3줄로 명확하고 전문적으로 요약(`summary`)해주세요.
        2. 계약서 내용을 바탕으로 프리랜서 입장에서 불리할 수 있는 '독소 조항(위약금 과다, 지적재산권 일방 귀속, 대금 지급 지연 등)'이나 '근로기준법/하도급법 위반 의심 사항'을 찾아내어 `toxic_clauses`에 간결히 나열하세요. 만약 문제가 될 만한 조항이 전혀 없다면, 반드시 빈 배열(`[]`)을 반환하세요.
        3. 체결 전 프리랜서가 추가로 협의하거나 확인하면 좋을 법적 조언을 `recommendations`에 최대 3개 작성하세요.

        <계약서_내용>
        {contract_content}
        </계약서_내용>
        """)

        llm_started_at = time.perf_counter()
        result = await structured_llm.ainvoke(prompt.format(contract_content=truncated_text))
        logger.info(
            "계약 분석 LLM 처리가 완료되었습니다. filename=%s parsed_text_length=%s truncated_text_length=%s elapsed_ms=%s total_elapsed_ms=%s",
            file.filename,
            len(parsed_text),
            len(truncated_text),
            round((time.perf_counter() - llm_started_at) * 1000),
            round((time.perf_counter() - started_at) * 1000),
        )
        return result

    except HTTPException:
        raise
    except Exception as e:
        logger.exception("Error parsing and analyzing contract PDF")
        raise HTTPException(status_code=500, detail="Internal server error")

