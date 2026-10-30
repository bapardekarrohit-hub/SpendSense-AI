# SpendSense AI - Python backend (Step 14)

## Run on Windows (PowerShell, inside this folder)
    python -m venv .venv
    .venv\Scripts\activate
    pip install -r requirements.txt
    python make_sample_data.py      # creates sample_transactions.csv
    python run_demo.py              # prints the full analysis report
    python -m pytest -v             # runs the tests
    uvicorn app.main:app --reload   # starts the API; open http://127.0.0.1:8000/docs

## What is inside
- app/forecast.py  : 2 baselines vs Ridge regression, rolling backtest, MAE/RMSE, ~80% range
- app/anomalies.py : IQR per category + Isolation Forest (60+ expenses), dismiss via keys
- app/insights.py  : insights shown only when data supports them, each with its calculation
- app/analysis.py  : category totals, fair same-days month comparison

## Honest limitations
- Needs >= 35 days of history for a forecast.
- Metrics are measured on 7-day windows; 30-day forecasts are less reliable.
- The sample data is synthetic (rent on the 1st), so its scores are NOT real-world accuracy.
- The ~80% range assumes errors are roughly normal and independent.
