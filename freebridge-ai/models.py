from pydantic import BaseModel, Field
from typing import List, Optional

# Spring에서 FastAPI로 보낼 데이터 형식
class RecommendationRequest(BaseModel):
    jobId: int = Field(description="공고 ID")
    title: str = Field(description="공고 제목")
    description: str = Field(description="공고 상세 내용")

# 단일 프리랜서 추천 정보 (사유 제외)
class FreelancerMatch(BaseModel):
    id: int = Field(description="프리랜서 고유 ID")
    nameOrTitle: str = Field(description="프리랜서 이름")
    matchScore: float = Field(description="적합도 점수")

# 최종 응답 형식
class EmployerRecommendationResponse(BaseModel):
    success: bool
    data: List[FreelancerMatch]