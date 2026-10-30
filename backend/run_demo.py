"""Runs the full analysis on sample_transactions.csv and prints a readable report."""
import json
import pandas as pd
from app.service import analyze

df = pd.read_csv("sample_transactions.csv")
res = analyze(df.to_dict("records"), monthly_budget=15000, horizon=7)

print("As of:", res["as_of"], "| rows:", res["transactions_used"])
print("\nTOP CATEGORIES")
for c in res["categories"][:4]:
    print(f"  {c['category']:<14}{c['total']:>10,.0f}  {c['share_pct']:.0f}%")
f = res["forecast"]
print("\nFORECAST (next", f["horizon_days"], "days)")
for m, v in f["metrics"].items():
    print(f"  {m:<32} MAE {v['mae']:>7}  RMSE {v['rmse']:>7}")
print("  method used:", f["method_used"])
print(f"  total {f['total_predicted']:,.0f}  (range {f['total_low']:,.0f} - {f['total_high']:,.0f})")
print("\nANOMALIES:", res["anomalies"]["method_summary"])
for a in res["anomalies"]["anomalies"]:
    print("  -", a["date"], a["title"], a["amount"], "|", a["why_flagged"])
print("\nINSIGHTS")
for i in res["insights"]:
    print("  -", i["text"], "\n      how:", i["calculation"])
