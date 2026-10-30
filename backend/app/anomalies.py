"""Unusual-transaction detection.

Rule 1 (always): per-category IQR fence. Needs >= 8 expenses in the category.
Rule 2 (only with >= 60 expenses): Isolation Forest as a second opinion.
A transaction is flagged only if it is also at least 2x the category median,
so tiny amounts are never flagged as "unusual".
"""
import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest

MIN_PER_CATEGORY = 8
MIN_FOR_FOREST = 60


def _key(row) -> str:
    return f"{row['date']:%Y-%m-%d}|{row['title']}|{row['amount']:.2f}"


def detect_anomalies(exp: pd.DataFrame, dismissed: list[str] | None = None) -> dict:
    dismissed = set(dismissed or [])
    exp = exp.reset_index(drop=True)
    stats = {}
    for cat, g in exp.groupby("category"):
        if len(g) >= MIN_PER_CATEGORY:
            q1, q3 = g["amount"].quantile([0.25, 0.75])
            stats[cat] = {"n": len(g), "median": float(g["amount"].median()),
                          "q1": float(q1), "q3": float(q3),
                          "upper": float(q3 + 1.5 * (q3 - q1))}

    flagged = {}  # row index -> method
    for i, r in exp.iterrows():
        s = stats.get(r["category"])
        if s and r["amount"] > s["upper"] and r["amount"] >= 2 * s["median"]:
            flagged[i] = "iqr"

    used_forest = len(exp) >= MIN_FOR_FOREST
    if used_forest:
        med = exp["category"].map(lambda c: stats[c]["median"] if c in stats
                                  else float(exp["amount"].median()))
        ratio = exp["amount"] / med
        X = np.column_stack([np.log1p(exp["amount"]), np.log(ratio)])
        labels = IsolationForest(n_estimators=200, contamination=0.03,
                                 random_state=42).fit_predict(X)
        # payments that repeat (same title and amount 3+ times, e.g. rent) are routine, not unusual
        repeats = exp.groupby(["title", "amount"])["amount"].transform("count").to_numpy()
        for i in np.where((labels == -1) & (ratio.to_numpy() >= 2.0) & (repeats < 3))[0]:
            flagged[i] = "iqr+isolation_forest" if i in flagged else "isolation_forest"

    items = []
    for i, method in flagged.items():
        r = exp.loc[i]
        key = _key(r)
        if key in dismissed:
            continue
        s = stats.get(r["category"])
        if s:
            baseline = {"category_median": round(s["median"], 2),
                        "typical_range": [round(s["q1"], 2), round(s["q3"], 2)],
                        "upper_limit": round(s["upper"], 2),
                        "transactions_used": s["n"]}
            why = (f"{r['amount']:,.2f} is above the upper limit of {s['upper']:,.2f} for "
                   f"{r['category']} (median {s['median']:,.2f}, based on {s['n']} transactions).")
        else:
            m = float(exp["amount"].median())
            baseline = {"overall_median": round(m, 2), "transactions_used": len(exp)}
            why = (f"{r['amount']:,.2f} is unusual compared with the overall median "
                   f"of {m:,.2f} (Isolation Forest).")
        items.append({"key": key, "date": f"{r['date']:%Y-%m-%d}", "title": r["title"],
                      "category": r["category"], "amount": round(float(r["amount"]), 2),
                      "method": method, "why_flagged": why, "baseline": baseline})
    items.sort(key=lambda x: x["amount"], reverse=True)
    return {"method_summary": "IQR per category" + (" + Isolation Forest" if used_forest else ""),
            "isolation_forest_used": used_forest,
            "anomalies": items,
            "note": "Flags are suggestions. Dismiss any that are normal for you."}
