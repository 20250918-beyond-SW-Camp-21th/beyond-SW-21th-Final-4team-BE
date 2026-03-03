# models.py
from pydantic import BaseModel, Field
from typing import List

class RecommendationRequest(BaseModel):
    jobId: int = Field(gt=0, description="공고 ID (양수)")
    title: str = Field(min_length=1, max_length=200, description="공고 제목 (필수)")
    description: str = Field(min_length=5, description="공고 상세 내용 (최소 5자 이상)")

class FreelancerMatch(BaseModel):
    id: int = Field(gt=0, description="프리랜서 고유 ID (양수)")
    nameOrTitle: str = Field(min_length=1, description="프리랜서 이름 (필수)")
    matchScore: float = Field(ge=0.0, le=1.0, description="적합도 점수 (0.0~1.0)")

class FreelancerMatchList(BaseModel):
    matches: List[FreelancerMatch] = Field(description="추천된 프리랜서 리스트")

class EmployerRecommendationResponse(BaseModel):
    success: bool
    data: List[FreelancerMatch]