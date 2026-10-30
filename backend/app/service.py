"""One function that runs the whole analysis. Used by the API and by the tests."""
import pandas as pd
from .data import to_frame, expenses_only
from .analysis import category_summary, month_to_date_compare
from .forecast import forecast_spending
from .anomalies import detect_anomalies
from .insights import build_insights


def analyze(rows, monthly_budget=None, horizon=7, dismissed=None) -> dict:
    df = to_frame(rows)
    exp = expenses_only(df)
    if exp.empty:
        raise ValueError("There are no expense transactions to analyse.")
    today = df["date"].max()  # "now" = the latest date in the data
    return {
        "as_of": f"{today:%Y-%m-%d}",
        "transactions_used": int(len(df)),
        "categories": category_summary(exp),
        "month_comparison": month_to_date_compare(exp, today),
        "forecast": forecast_spending(exp, today, horizon),
        "anomalies": detect_anomalies(exp, dismissed),
        "insights": build_insights(exp, today, monthly_budget),
    }
