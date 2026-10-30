import unittest
import pandas as pd
from app.data import to_frame, expenses_only
from app.analysis import month_to_date_compare
from app.anomalies import detect_anomalies
from app.forecast import forecast_spending
from app.service import analyze


def make_rows(days=60, spike=False):
    rows = []
    for i, d in enumerate(pd.date_range("2026-08-01", periods=days)):
        rows.append({"title": "Meals", "amount": 150 + (i % 7) * 10, "type": "expense",
                     "category": "Food", "date": f"{d:%Y-%m-%d}"})
    if spike:
        rows.append({"title": "Big dinner", "amount": 3000, "type": "expense",
                     "category": "Food", "date": "2026-09-15"})
    return rows


class DataTests(unittest.TestCase):
    def test_rejects_bad_amount(self):
        with self.assertRaises(ValueError):
            to_frame([{"title": "x", "amount": -5, "type": "expense", "category": "Food", "date": "2026-01-01"}])

    def test_rejects_bad_date(self):
        with self.assertRaises(ValueError):
            to_frame([{"title": "x", "amount": 5, "type": "expense", "category": "Food", "date": "31/02/2026"}])

    def test_income_is_not_expense(self):
        rows = make_rows(3) + [{"title": "Pay", "amount": 9999, "type": "income", "category": "Other", "date": "2026-08-01"}]
        self.assertEqual(len(expenses_only(to_frame(rows))), 3)


class AnalysisTests(unittest.TestCase):
    def test_same_day_comparison(self):
        rows = [
            {"title": "a", "amount": 100, "type": "expense", "category": "Food", "date": "2026-09-03"},
            {"title": "b", "amount": 900, "type": "expense", "category": "Food", "date": "2026-09-28"},
            {"title": "c", "amount": 150, "type": "expense", "category": "Food", "date": "2026-10-03"},
        ]
        exp = expenses_only(to_frame(rows))
        res = month_to_date_compare(exp, pd.Timestamp("2026-10-05"))
        self.assertEqual(res["last_month_same_days_total"], 100.0)  # the 28 Sep row must be excluded
        self.assertEqual(res["total_change_pct"], 50.0)


class ForecastTests(unittest.TestCase):
    def test_too_little_data(self):
        exp = expenses_only(to_frame(make_rows(10)))
        self.assertFalse(forecast_spending(exp, exp["date"].max())["available"])

    def test_forecast_shape_and_metrics(self):
        exp = expenses_only(to_frame(make_rows(70)))
        res = forecast_spending(exp, exp["date"].max(), 7)
        self.assertTrue(res["available"])
        self.assertEqual(len(res["forecast_days"]), 7)
        self.assertEqual(len(res["metrics"]), 3)
        for d in res["forecast_days"]:
            self.assertGreaterEqual(d["low"], 0)
            self.assertLessEqual(d["low"], d["predicted"])
            self.assertLessEqual(d["predicted"], d["high"])

    def test_30_day_horizon(self):
        exp = expenses_only(to_frame(make_rows(70)))
        self.assertEqual(len(forecast_spending(exp, exp["date"].max(), 30)["forecast_days"]), 30)


class AnomalyTests(unittest.TestCase):
    def test_spike_flagged_and_dismissable(self):
        exp = expenses_only(to_frame(make_rows(60, spike=True)))
        res = detect_anomalies(exp)
        self.assertEqual([a["title"] for a in res["anomalies"]], ["Big dinner"])
        key = res["anomalies"][0]["key"]
        self.assertEqual(detect_anomalies(exp, [key])["anomalies"], [])

    def test_no_false_alarm_on_steady_spending(self):
        exp = expenses_only(to_frame(make_rows(60)))
        self.assertEqual(detect_anomalies(exp)["anomalies"], [])


class ServiceTests(unittest.TestCase):
    def test_full_run(self):
        res = analyze(make_rows(70, spike=True), monthly_budget=1000)
        for k in ("categories", "forecast", "anomalies", "insights", "month_comparison"):
            self.assertIn(k, res)

    def test_only_income_raises(self):
        with self.assertRaises(ValueError):
            analyze([{"title": "Pay", "amount": 5, "type": "income", "category": "Other", "date": "2026-01-01"}])


if __name__ == "__main__":
    unittest.main()
