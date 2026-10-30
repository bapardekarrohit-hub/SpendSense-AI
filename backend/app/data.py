"""Turns raw transaction dictionaries into a clean, validated pandas DataFrame."""
import pandas as pd

REQUIRED = ["title", "amount", "type", "category", "date"]


def to_frame(rows: list[dict]) -> pd.DataFrame:
    """Validate rows and return a DataFrame. Raises ValueError with a clear message."""
    if not rows:
        raise ValueError("No transactions were provided.")
    df = pd.DataFrame(rows)
    missing = [c for c in REQUIRED if c not in df.columns]
    if missing:
        raise ValueError(f"Missing columns: {', '.join(missing)}")

    df["amount"] = pd.to_numeric(df["amount"], errors="coerce")
    if df["amount"].isna().any() or (df["amount"] <= 0).any():
        raise ValueError("Every amount must be a number greater than 0.")

    df["date"] = pd.to_datetime(df["date"], errors="coerce")
    if df["date"].isna().any():
        raise ValueError("Every date must look like 2026-10-25.")
    df["date"] = df["date"].dt.normalize()

    df["type"] = df["type"].astype(str).str.strip().str.lower()
    if not df["type"].isin(["income", "expense"]).all():
        raise ValueError("type must be 'income' or 'expense'.")

    df["category"] = df["category"].astype(str).str.strip()
    df["title"] = df["title"].astype(str).str.strip()
    return df.reset_index(drop=True)


def expenses_only(df: pd.DataFrame) -> pd.DataFrame:
    """Income is never an expense, so it is removed before any spending maths."""
    return df[df["type"] == "expense"].copy()
