from pydantic import BaseModel

class DetectRequest(BaseModel):
    taskId: int

class IpScore(BaseModel):
    ip: str
    score: float
    isAbnormal: bool
    lastAccessTime: str | None = None

class DetectResponse(BaseModel):
    taskId: int
    ipCount: int
    abnormalIpCount: int
    scores: list[IpScore]