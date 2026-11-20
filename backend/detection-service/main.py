import uvicorn
from fastapi import FastAPI
from api.detect import router as detect_router

app = FastAPI()

app.include_router(detect_router)

@app.get("/online")
def health():
    return {"status": "ok"}

if __name__ == '__main__':
    uvicorn.run(app, host="0.0.0.0", port=8000)
