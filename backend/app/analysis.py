"""Spending analysis with pandas: category totals and month-to-date comparison."""
import pandas as pd


def category_summary(exp: pd.DataFrame) -> list[dict]:
    if exp.empty:
        return []
    g = exp.groupby("category")["amount"].agg(total="sum", count="count", average="mean")
    g["share_pct"] = g["total"] / g["total"].sum() * 100
    g = g.sort_values("total", ascending=False).round(2)
    return [{"category": c, **row} for c, row in g.to_dict("index").items()]


def month_to_date_compare(exp: pd.DataFrame, today: pd.Timestamp) -> dict:
    """Compare this month so far with the SAME number of days of last month.

    Comparing a half month with a full month would be unfair, so both
    periods cover days 1..N.
    """
    start = today.replace(day=1)
    prev_last = start - pd.Timedelta(days=1)
    prev_start = prev_last.replace(day=1)
    day = min(today.day, prev_last.day)
    prev_end = prev_start.replace(day=day)

    cur = exp[(exp["date"] >= start) & (exp["date"] <= today)]
    prev = exp[(exp["date"] >= prev_start) & (exp["date"] <= prev_end)]
    cur_by = cur.groupby("category")["amount"].sum()
    prev_by = prev.groupby("category")["amount"].sum()

    rows = []
    for cat in sorted(set(cur_by.index) | set(prev_by.index)):
        c, p = float(cur_by.get(cat, 0.0)), float(prev_by.get(cat, 0.0))
        pct = round((c - p) / p * 100, 1) if p > 0 else None
        rows.append({"category": cat, "this_month": round(c, 2),
                     "last_month_same_days": round(p, 2), "change_pct": pct})
    total_c, total_p = float(cur["amount"].sum()), float(prev["amount"].sum())
    return {
        "days_compared": int(day),
        "this_month_total": round(total_c, 2),
        "last_month_same_days_total": round(total_p, 2),
        "total_change_pct": round((total_c - total_p) / total_p * 100, 1) if total_p > 0 else None,
        "categories": rows,
    }
