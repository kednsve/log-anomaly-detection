"""
    模型推理接口
"""
import time

from fastapi import APIRouter, HTTPException

from api.schema import DetectRequest, DetectResponse, IpScore
from core.inference import score_logs
from db.database import load_logs

router = APIRouter()


@router.post("/detect", response_model=DetectResponse)


def detect(req: DetectRequest):
    if req.taskId <= 0:
        raise HTTPException(status_code=400, detail="taskId 不合法")

    start = time.perf_counter()
    logs = load_logs(req.taskId)
    if logs.empty:
        raise HTTPException(status_code=404, detail="该任务没有可检测的访问日志")

    scored = score_logs(logs)

    scores = [
        IpScore(
            ip=row.ip,
            score=float(row.score),
            isAbnormal=bool(row.is_abnormal),
            lastAccessTime=row.last_access_time.strftime("%Y-%m-%d %H:%M:%S"),
        )
        for row in scored.itertuples()
    ]
    return DetectResponse(
        taskId=req.taskId,
        ipCount=len(scores),
        abnormalIpCount=int(scored["is_abnormal"].sum()),
        elapsedMs=int((time.perf_counter() - start) * 1000),
        scores=scores,
    )
