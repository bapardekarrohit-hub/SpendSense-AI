"""Creates sample_transactions.csv (the SAME format the Android app imports)."""
import csv
import numpy as np
import pandas as pd

rng = np.random.default_rng(42)
end = pd.Timestamp("2026-10-25")
days = pd.date_range(end - pd.Timedelta(days=149), end)
rows = []


def add(d, title, amt, typ, cat, pay):
    rows.append([title, f"{amt:.2f}", typ, cat, f"{d:%Y-%m-%d}", pay, ""])


for d in days:
    weekend = d.dayofweek >= 5
    add(d, "Meals", max(60, rng.normal(260 if weekend else 170, 40)), "expense", "Food", "UPI")
    if rng.random() < 0.6 and not weekend:
        add(d, "Commute", max(30, rng.normal(90, 25)), "expense", "Transport", "UPI")
    if weekend and rng.random() < 0.35:
        add(d, "Shopping trip", max(250, rng.normal(900, 300)), "expense", "Shopping", "Card")
    if rng.random() < 0.10:
        add(d, "Movie / outing", max(150, rng.normal(450, 120)), "expense", "Entertainment", "Card")
    if rng.random() < 0.03:
        add(d, "Pharmacy", max(100, rng.normal(350, 100)), "expense", "Healthcare", "Cash")
    if d.day == 1:
        add(d, "House rent", 12000, "expense", "Rent", "Bank transfer")
        add(d, "Salary", 42000, "income", "Other", "Bank transfer")
    if d.day == 5:
        add(d, "Electricity bill", rng.normal(1700, 200), "expense", "Bills", "UPI")
    if d.day == 8:
        add(d, "Internet bill", 799, "expense", "Bills", "UPI")

# a few deliberate outliers so anomaly detection has something to find
add(pd.Timestamp("2026-10-14"), "Phone repair", 6200, "expense", "Healthcare", "Card")
add(pd.Timestamp("2026-09-20"), "New headphones", 9500, "expense", "Shopping", "Card")
add(pd.Timestamp("2026-10-18"), "Family dinner", 2400, "expense", "Food", "Card")

rows.sort(key=lambda r: r[4])
with open("sample_transactions.csv", "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["title", "amount", "type", "category", "date", "payment_method", "notes"])
    w.writerows(rows)
print(f"Wrote {len(rows)} rows")
