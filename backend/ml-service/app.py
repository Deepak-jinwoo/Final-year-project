"""
=============================================================
AquaNexus — FastAPI ML Prediction Service
Serves XGBoost predictions and Isolation Forest anomaly
detection via REST endpoints.
Runs on port 5000 alongside the Spring Boot backend.
=============================================================
"""
import os
import numpy as np
import pandas as pd
import joblib
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime

# =========================================================
# LOAD PRE-TRAINED MODELS
# =========================================================
MODEL_DIR = "models"

def load_model(name):
    path = os.path.join(MODEL_DIR, name)
    if not os.path.exists(path):
        raise FileNotFoundError(
            f"Model '{name}' not found. Run 'python train_models.py' first."
        )
    return joblib.load(path)

try:
    model_fresh = load_model("xgb_fresh_water.pkl")
    model_reused = load_model("xgb_reused_water.pkl")
    iso_forest = load_model("isolation_forest.pkl")
    le_dept = load_model("label_encoder_dept.pkl")
    feature_cols = load_model("feature_columns.pkl")
    anomaly_features = load_model("anomaly_features.pkl")
    print("[✓] All models loaded successfully.")
except FileNotFoundError as e:
    print(f"[!] {e}")
    print("[!] Starting without models. Train first with: python train_models.py")
    model_fresh = model_reused = iso_forest = le_dept = None
    feature_cols = anomaly_features = []

# =========================================================
# FASTAPI APP
# =========================================================
app = FastAPI(
    title="AquaNexus ML Service",
    description="Water consumption prediction and anomaly detection API",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# =========================================================
# REQUEST / RESPONSE MODELS
# =========================================================
class DailyRecord(BaseModel):
    industry_id: int
    department: str
    date: str                        # "YYYY-MM-DD"
    fresh_water_consumed: float
    wastewater_generated: float
    reused_water: float
    cto_limit: float

class PredictionRequest(BaseModel):
    records: List[DailyRecord]       # At least 7 recent days recommended

class PredictionResponse(BaseModel):
    predicted_fresh_water: float
    predicted_reused_water: float
    predicted_reuse_percentage: float
    cto_limit: float
    cto_utilization_predicted: float
    cto_compliance: str              # "COMPLIANT" or "NON-COMPLIANT"
    model_confidence: str            # "HIGH", "MEDIUM", "LOW"

class AnomalyRecord(BaseModel):
    date: str
    fresh_water_consumed: float
    wastewater_generated: float
    reused_water: float
    is_anomaly: bool
    anomaly_score: float
    severity: str                    # "NORMAL", "WARNING", "CRITICAL"
    reason: Optional[str] = None

class AnomalyRequest(BaseModel):
    records: List[DailyRecord]

class AnomalyResponse(BaseModel):
    total_records: int
    anomalies_detected: int
    results: List[AnomalyRecord]

# =========================================================
# FEATURE ENGINEERING HELPER
# =========================================================
def prepare_features(records: List[DailyRecord]) -> pd.DataFrame:
    """Convert incoming records to a feature DataFrame."""
    data = [r.dict() for r in records]
    df = pd.DataFrame(data)
    df["date"] = pd.to_datetime(df["date"])

    # Time features
    df["day_of_week"] = df["date"].dt.dayofweek
    df["month"] = df["date"].dt.month
    df["day_of_month"] = df["date"].dt.day
    df["is_weekend"] = (df["day_of_week"] >= 5).astype(int)

    # Derived ratios
    df["reuse_percentage"] = (df["reused_water"] / df["fresh_water_consumed"].replace(0, 1)) * 100
    df["cto_utilization"] = (df["fresh_water_consumed"] / df["cto_limit"].replace(0, 1)) * 100
    df["waste_ratio"] = df["wastewater_generated"] / df["fresh_water_consumed"].replace(0, 1)

    # Encode department
    try:
        df["department_encoded"] = le_dept.transform(df["department"])
    except (ValueError, AttributeError):
        df["department_encoded"] = 0

    # Sort by date
    df = df.sort_values("date").reset_index(drop=True)

    # Lag features
    for lag in [1, 3, 7]:
        df[f"fresh_lag_{lag}"] = df["fresh_water_consumed"].shift(lag)
        df[f"reused_lag_{lag}"] = df["reused_water"].shift(lag)

    # Rolling averages
    for window in [7, 14]:
        df[f"fresh_rolling_{window}d"] = df["fresh_water_consumed"].rolling(window, min_periods=1).mean()
        df[f"reused_rolling_{window}d"] = df["reused_water"].rolling(window, min_periods=1).mean()

    # Fill any remaining NaN with forward fill then 0
    df = df.fillna(method="ffill").fillna(0)

    return df

# =========================================================
# ENDPOINTS
# =========================================================
@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {
        "status": "healthy",
        "models_loaded": model_fresh is not None,
        "timestamp": datetime.now().isoformat()
    }

@app.post("/predict", response_model=PredictionResponse)
async def predict(request: PredictionRequest):
    """
    Predict next-day fresh water consumption and reused water.
    Send at least 7 recent daily records for best accuracy.
    """
    if model_fresh is None or model_reused is None:
        raise HTTPException(status_code=503, detail="Models not loaded. Run train_models.py first.")

    if len(request.records) < 1:
        raise HTTPException(status_code=400, detail="At least 1 daily record is required.")

    df = prepare_features(request.records)

    # Use the last row (most recent day) for prediction
    last_row = df.iloc[-1:]
    X = last_row[feature_cols]

    pred_fresh = float(model_fresh.predict(X)[0])
    pred_reused = float(model_reused.predict(X)[0])

    # Ensure non-negative
    pred_fresh = max(0, round(pred_fresh, 1))
    pred_reused = max(0, round(pred_reused, 1))

    cto_limit = float(last_row["cto_limit"].values[0])
    cto_util = round((pred_fresh / cto_limit) * 100, 1) if cto_limit > 0 else 0
    reuse_pct = round((pred_reused / pred_fresh) * 100, 1) if pred_fresh > 0 else 0

    compliance = "COMPLIANT" if pred_fresh <= cto_limit else "NON-COMPLIANT"

    # Confidence based on data quantity
    confidence = "HIGH" if len(request.records) >= 14 else "MEDIUM" if len(request.records) >= 7 else "LOW"

    return PredictionResponse(
        predicted_fresh_water=pred_fresh,
        predicted_reused_water=pred_reused,
        predicted_reuse_percentage=reuse_pct,
        cto_limit=cto_limit,
        cto_utilization_predicted=cto_util,
        cto_compliance=compliance,
        model_confidence=confidence
    )

@app.post("/detect-anomalies", response_model=AnomalyResponse)
async def detect_anomalies(request: AnomalyRequest):
    """
    Detect anomalous water usage patterns (possible leaks/inefficiencies).
    """
    if iso_forest is None:
        raise HTTPException(status_code=503, detail="Anomaly model not loaded. Run train_models.py first.")

    if len(request.records) < 1:
        raise HTTPException(status_code=400, detail="At least 1 daily record is required.")

    data = [r.dict() for r in request.records]
    df = pd.DataFrame(data)

    # Compute features needed for anomaly detection
    df["reuse_percentage"] = (df["reused_water"] / df["fresh_water_consumed"].replace(0, 1)) * 100
    df["cto_utilization"] = (df["fresh_water_consumed"] / df["cto_limit"].replace(0, 1)) * 100
    df["waste_ratio"] = df["wastewater_generated"] / df["fresh_water_consumed"].replace(0, 1)

    X_anom = df[anomaly_features].fillna(0)

    labels = iso_forest.predict(X_anom)          # 1 = normal, -1 = anomaly
    scores = iso_forest.decision_function(X_anom) # lower = more anomalous

    results = []
    for i, row in df.iterrows():
        is_anomaly = bool(labels[i] == -1)
        score = float(scores[i])

        # Determine severity
        if not is_anomaly:
            severity = "NORMAL"
        elif score < -0.2:
            severity = "CRITICAL"
        else:
            severity = "WARNING"

        # Generate reason for anomalies
        reason = None
        if is_anomaly:
            reasons = []
            if row["cto_utilization"] > 90:
                reasons.append(f"CTO utilization at {row['cto_utilization']:.0f}% (near/over limit)")
            if row["waste_ratio"] > 0.85:
                reasons.append(f"High waste ratio ({row['waste_ratio']:.2f})")
            if row["reuse_percentage"] < 15:
                reasons.append(f"Low reuse rate ({row['reuse_percentage']:.1f}%)")
            if row["fresh_water_consumed"] > row["cto_limit"] * 0.9:
                reasons.append("Fresh water consumption near CTO limit")
            reason = "; ".join(reasons) if reasons else "Unusual usage pattern detected"

        results.append(AnomalyRecord(
            date=str(row["date"]),
            fresh_water_consumed=float(row["fresh_water_consumed"]),
            wastewater_generated=float(row["wastewater_generated"]),
            reused_water=float(row["reused_water"]),
            is_anomaly=is_anomaly,
            anomaly_score=round(score, 4),
            severity=severity,
            reason=reason
        ))

    anomaly_count = sum(1 for r in results if r.is_anomaly)

    return AnomalyResponse(
        total_records=len(results),
        anomalies_detected=anomaly_count,
        results=results
    )

# =========================================================
# RUN SERVER
# =========================================================
if __name__ == "__main__":
    import uvicorn
    print("\n[→] Starting AquaNexus ML Service on http://localhost:5000")
    print("[→] API docs available at http://localhost:5000/docs\n")
    uvicorn.run(app, host="0.0.0.0", port=5000)
