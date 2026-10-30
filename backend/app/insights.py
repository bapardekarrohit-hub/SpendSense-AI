"""Data-backed insights. Each insight is created only when the numbers support it,
and carries the calculation that produced it."""
import calendar
import pandas as pd
from .analysis import month_to_date_compare


def build_insights(exp: pd.DataFrame, today: pd.Timestamp, monthly_budget: float | None) -> list[dict]:
    out = []
    start = today.replace(day=1)
    cur = exp[(exp["date"] >= start) & (exp["date"] <= today)]

    if len(cur) >= 5:
        by = cur.groupby("category")["amount"].sum().sort_values(ascending=False)
        top, total = by.index[0], by.sum()
        out.append({"id": "top_category",
                    "text": f"{top} is your highest-spending category this month "
                            f"({by.iloc[0] / total * 100:.0f}% of spending).",
                    "calculation": f"{by.iloc[0]:,.2f} / {total:,.2f} total expenses since {start:%d %b}."})

    cmp = month_to_date_compare(exp, today)
    for row in cmp["categories"]:
        p, c, pct = row["last_month_same_days"], row["this_month"], row["change_pct"]
        if pct is not None and p >= 500 and abs(pct) >= 15:
            word = "increased" if pct > 0 else "decreased"
            out.append({"id": f"change_{row['category']}",
                        "text": f"Your {row['category']} spending {word} by {abs(pct):.0f}% "
                                f"compared with the same days last month.",
                        "calculation": f"Days 1-{cmp['days_compared']}: {c:,.2f} this month vs "
                                       f"{p:,.2f} last month = {pct:+.1f}%."})

    if monthly_budget and monthly_budget > 0 and today.day >= 7:
        spent = float(cur["amount"].sum())
        dim = calendar.monthrange(today.year, today.month)[1]
        projected = spent / today.day * dim
        if projected > monthly_budget:
            out.append({"id": "budget_projection",
                        "text": f"At this pace you may spend about {projected:,.0f} this month, "
                                f"above your budget of {monthly_budget:,.0f}.",
                        "calculation": f"{spent:,.2f} spent in {today.day} days / {today.day} x "
                                       f"{dim} days = {projected:,.2f}. This is an estimate."})
    return out
