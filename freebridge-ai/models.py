# models.py
from pydantic import BaseModel, Field
from typing import List 

class RecommendationRequest(BaseModel):
    jobId: int = Field(description="공고 ID")
    title: str = Field(description="공고 제목")
    description: str = Field(description="공고 상세 내용")

class FreelancerMatch(BaseModel):
    id: int = Field(description="프리랜서 고유 ID")
    nameOrTitle: str = Field(description="프리랜서 이름")
    matchScore: float = Field(ge=0.0, le=1.0, description="적합도 점수 (0.0~1.0)")

class FreelancerMatchList(BaseModel):
    matches: List[FreelancerMatch] = Field(description="추천된 프리랜서 리스트")

class EmployerRecommendationResponse(BaseModel):
    success: bool
    data: List[FreelancerMatch]