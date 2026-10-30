"""FastAPI server. Run:  uvicorn app.main:app --reload"""
from datetime import date as Date
from typing import Literal

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from .service import analyze

app = FastAPI(title="SpendSense AI backend", version="0.1.0")


class TransactionIn(BaseModel):
    title: str = Field(min_length=1, max_length=120)
    amount: float = Field(gt=0, lt=100_000_000)
    type: Literal["income", "expense"]
    category: str = Field(min_length=1, max_length=40)
    date: Date
    payment_method: str = ""
    notes: str = ""


class AnalyzeRequest(BaseModel):
    transactions: list[TransactionIn] = Field(min_length=1, max_length=5000)
    monthly_budget: float | None = Field(default=None, gt=0)
    horizon_days: Literal[7, 30] = 7
    dismissed_keys: list[str] = []


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/analyze")
def analyze_endpoint(req: AnalyzeRequest):
    rows = [t.model_dump(mode="json") for t in req.transactions]
    try:
        return analyze(rows, req.monthly_budget, req.horizon_days, req.dismissed_keys)
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))
