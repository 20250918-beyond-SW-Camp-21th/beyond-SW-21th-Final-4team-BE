from pydantic import BaseModel, Field
from typing import List, Optional

# 1. 단일 프리랜서 추천 정보 (UI에 띄울 핵심 데이터만)
class FreelancerMatch(BaseModel):
    id: int = Field(description="프리랜서의 고유 ID (DB PK)")
    nameOrTitle: str = Field(description="프리랜서 이름")
    matchScore: float = Field(description="공고와의 적합도 점수 (0.0 ~ 1.0)")

# 2. 기업용 추천 API 최종 응답 형식
class EmployerRecommendationResponse(BaseModel):
    success: bool
    data: List[FreelancerMatch]