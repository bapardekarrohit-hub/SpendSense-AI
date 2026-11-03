# SpendSense AI

An Android personal-finance app (Kotlin, Jetpack Compose, Room) that tracks spending, forecasts the next days, flags unusual transactions and explains its insights. The analysis runs **on the phone**. A Python backend (FastAPI, pandas, scikit-learn) is included as the research and testing version of the same models.

Built as a learning and portfolio project. Your data never leaves the phone.

## Screenshots
Add your phone screenshots in `docs/screenshots` and link them here.

## Features

**Android app**
- Dashboard: expenses, budget usage, savings rate, daily average, weekly chart, "Safe to spend today"
- Transactions: add, delete, validation, stored locally with Room
- Budgets: monthly budget, category totals, comparison with last month, alerts at 80% and 100%
- Analytics: period filters, trend line, category donut, income vs expenses, payment methods
- AI tab (works offline, no server): 7- or 30-day forecast, unusual spending with a Dismiss button, insights that show their calculation, and a forecast report card that checks past forecasts against real spending
- Settings: light/dark theme, currency symbol, profile, daily reminder, backup and restore (CSV), CSV import with row-by-row validation, screenshot blocking, delete all data
- Languages: English, Hindi and Marathi (navigation and Settings are translated; other screens are in progress)
- Only expenses count toward budgets. Income is never treated as an expense.

**Python backend** (`backend/`, optional)
- `POST /analyze` validates input and returns category summary, forecast, anomalies and insights
- Forecast: two baselines (last-7-day average, same-weekday average) against a Ridge regression, scored on held-out weeks with MAE and RMSE; the method with the lowest MAE is used
- Anomalies: IQR fence per category, plus Isolation Forest when there are 60 or more expenses
- Insights are produced only when the data supports them, each with its calculation
- 14 automated tests (`pytest`)

## Honest limitations
- The forecast needs at least 35 days of history.
- Metrics are measured on 7-day windows. A 30-day forecast is less reliable.
- The on-device version uses the same forecast methods as the backend but does not include the Isolation Forest.
- The sample data (`backend/sample_transactions.csv`) is synthetic, so its scores are not real-world accuracy.
- The forecast range is an approximate 80% band that assumes errors are roughly normal and independent.
- There are no accounts and no cloud sync. "Backup" means a CSV file you save yourself.
- Hindi and Marathi text was written by the author and has not been reviewed by a professional translator.
- Forecasts are estimates, not financial advice or guaranteed outcomes.

## Privacy
- Transactions, budget and settings are stored only on the phone.
- The app has no internet permission, no analytics, no ads and no embedded API keys.
- You can export everything to CSV and delete everything from Settings.

## Download
Get the latest APK from the [Releases](../../releases) page. Android will ask you to allow installing from your browser, because the app is not on the Play Store.

## Run the Android app
1. Open the project folder in Android Studio and let Gradle sync.
2. Connect a phone with USB debugging (or use an emulator).
3. Click Run.
4. To try the AI tab, import `backend/sample_transactions.csv` from Settings > Import from CSV.

## Run the backend (optional)
```
cd backend
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
python make_sample_data.py
python run_demo.py
python -m pytest -v
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

## Tech
Kotlin, Jetpack Compose, Material 3, Room, WorkManager · Python, FastAPI, pandas, NumPy, scikit-learn · pytest

## Roadmap
Edit transaction, savings goals, category budgets, recurring-bill detection, translating the remaining screens, accounts and cloud sync.

## License
MIT. See the [LICENSE](LICENSE) file.

## Author
Rohit Bapardekar · [GitHub](https://github.com/bapardekarrohit-hub)
