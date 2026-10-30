"""Spending forecast: two simple baselines versus a Ridge regression model.

Every method is scored on the SAME held-out weeks (rolling backtest), using
MAE and RMSE. The method with the lowest MAE is used for the forecast. If a
baseline wins, the baseline is used and the result says so.
"""
import numpy as np
import pandas as pd
from sklearn.linear_model import Ridge
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

MIN_DAYS = 35          # 4 weeks of history + at least 1 test week
BACKTEST_HORIZON = 7   # every backtest window forecasts 7 days
Z_80 = 1.28            # ~80% band if errors are roughly normal


def daily_series(exp: pd.DataFrame, end: pd.Timestamp) -> pd.Series:
    """Total expense per calendar day; days with no spending are 0."""
    s = exp.groupby("date")["amount"].sum()
    idx = pd.date_range(s.index.min(), end, freq="D")
    return s.reindex(idx, fill_value=0.0)


def _features(hist: np.ndarray, day: pd.Timestamp) -> list[float]:
    dow = [1.0 if day.day_of_week == d else 0.0 for d in range(7)]
    month_start = 1.0 if day.day == 1 else 0.0  # rent/salary-type payments often fall on the 1st
    return [hist[-1], hist[-7], hist[-7:].mean(), hist[-14:].mean(), month_start] + dow


def _training_set(values: np.ndarray, dates: pd.DatetimeIndex):
    X, y = [], []
    for t in range(14, len(values)):
        X.append(_features(values[:t], dates[t]))
        y.append(values[t])
    return np.array(X), np.array(y)


def _new_model() -> Pipeline:
    return Pipeline([("scale", StandardScaler()), ("ridge", Ridge(alpha=10.0))])


def _predict_mean7(hist, start, horizon, model=None):
    return [float(hist[-7:].mean())] * horizon


def _predict_weekday(hist, start, horizon, model=None):
    h, out = list(hist), []
    for _ in range(horizon):
        lags = [h[-k] for k in (7, 14, 21, 28) if len(h) >= k]
        p = float(np.mean(lags))
        out.append(p)
        h.append(p)
    return out


def _predict_model(hist, start, horizon, model):
    h, out = list(hist), []
    for i in range(horizon):
        day = start + pd.Timedelta(days=i)
        p = max(0.0, float(model.predict([_features(np.array(h), day)])[0]))
        out.append(p)
        h.append(p)
    return out


METHODS = {
    "baseline_last_7_day_average": _predict_mean7,
    "baseline_same_weekday_average": _predict_weekday,
    "ridge_regression": _predict_model,
}


def _fit(values, dates):
    X, y = _training_set(values, dates)
    return _new_model().fit(X, y)


def forecast_spending(exp: pd.DataFrame, end: pd.Timestamp, horizon: int = 7) -> dict:
    series = daily_series(exp, end)
    values, dates = series.to_numpy(dtype=float), series.index
    n = len(values)
    if n < MIN_DAYS:
        return {"available": False,
                "reason": f"Need at least {MIN_DAYS} days of history, found {n}."}

    windows = min(4, (n - 28) // BACKTEST_HORIZON)
    errors = {m: [] for m in METHODS}
    last_window = None
    for k in range(windows, 0, -1):
        o = n - k * BACKTEST_HORIZON
        model = _fit(values[:o], dates[:o])
        actual = values[o:o + BACKTEST_HORIZON]
        start = dates[o]
        preds = {m: np.array(fn(values[:o], start, BACKTEST_HORIZON, model))
                 for m, fn in METHODS.items()}
        for m, p in preds.items():
            errors[m].extend((actual - p).tolist())
        last_window = (dates[o:o + BACKTEST_HORIZON], actual, preds)

    metrics = {}
    for m, e in errors.items():
        e = np.array(e)
        metrics[m] = {"mae": round(float(np.abs(e).mean()), 2),
                      "rmse": round(float(np.sqrt((e ** 2).mean())), 2)}
    chosen = min(metrics, key=lambda m: metrics[m]["mae"])
    best_baseline = min((m for m in metrics if m.startswith("baseline")),
                        key=lambda m: metrics[m]["mae"])
    base_mae = metrics[best_baseline]["mae"]
    gain = round((base_mae - metrics["ridge_regression"]["mae"]) / base_mae * 100, 1) if base_mae else 0.0

    # final forecast uses ALL available history
    model = _fit(values, dates)
    start = end + pd.Timedelta(days=1)
    preds = METHODS[chosen](values, start, horizon, model)
    spread = Z_80 * float(np.std(errors[chosen]))
    days = []
    for i, p in enumerate(preds):
        days.append({"date": (start + pd.Timedelta(days=i)).strftime("%Y-%m-%d"),
                     "predicted": round(p, 2),
                     "low": round(max(0.0, p - spread), 2),
                     "high": round(p + spread, 2)})
    total = float(sum(preds))
    total_spread = spread * np.sqrt(horizon)  # assumes day errors are independent (approximation)

    d, a, p = last_window
    actual_vs_pred = [{"date": x.strftime("%Y-%m-%d"), "actual": round(float(av), 2),
                       "predicted": round(float(pv), 2)}
                      for x, av, pv in zip(d, a, p[chosen])]
    return {
        "available": True,
        "horizon_days": horizon,
        "method_used": chosen,
        "ridge_improvement_over_best_baseline_pct": gain,
        "metrics": metrics,
        "metrics_note": (f"Scored on {windows} held-out week(s) ({windows * 7} days) the model "
                         "had not seen. Lower is better. Metrics are for 7-day windows; "
                         "a 30-day forecast is less reliable."),
        "forecast_days": days,
        "total_predicted": round(total, 2),
        "total_low": round(max(0.0, total - total_spread), 2),
        "total_high": round(total + total_spread, 2),
        "actual_vs_predicted_last_week": actual_vs_pred,
        "disclaimer": "Forecasts are statistical estimates, not guaranteed outcomes. "
                      "The range is an approximate 80% band based on past forecast errors.",
    }
