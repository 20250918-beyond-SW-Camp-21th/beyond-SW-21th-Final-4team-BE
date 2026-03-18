# models.py
from typing import List

from pydantic import BaseModel, Field


class RecommendationRequest(BaseModel):
    jobId: int = Field(gt=0, description="Job posting ID")
    title: str = Field(min_length=1, max_length=200, description="Job title")
    description: str = Field(default="", description="Job description")
    skills: str = Field(default="", description="Required job skills")


class FreelancerMatch(BaseModel):
    id: int = Field(gt=0, description="Freelancer ID")
    nameOrTitle: str = Field(min_length=1, description="Freelancer name")
    matchScore: float = Field(ge=0.0, le=1.0, description="Match score")


class FreelancerMatchList(BaseModel):
    matches: List[FreelancerMatch] = Field(description="Recommended freelancer list")


class EmployerRecommendationResponse(BaseModel):
    success: bool
    data: List[FreelancerMatch]


class FreelancerRecommendRequest(BaseModel):
    freelancerId: int = Field(gt=0, description="Freelancer ID")
    skills: str = Field(default="", description="Freelancer skills")
    experience: str = Field(default="", description="Freelancer experience summary")


class JobMatch(BaseModel):
    id: int = Field(gt=0, description="Job posting ID")
    title: str = Field(min_length=1, description="Job title")
    matchScore: float = Field(ge=0.0, le=1.0, description="Match score")


class JobMatchList(BaseModel):
    matches: List[JobMatch] = Field(description="Recommended job list")


class FreelancerRecommendationResponse(BaseModel):
    success: bool
    data: List[JobMatch]
