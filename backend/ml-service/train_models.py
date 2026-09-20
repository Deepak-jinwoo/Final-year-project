"""
=============================================================
AquaNexus — ML Model Training Script
Trains XGBoost regressors for water consumption forecasting
and Isolation Forest for anomaly detection.
=============================================================
"""
import os
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.preprocessing import LabelEncoder
from xgboost import XGBRegressor
from sklearn.ensemble import IsolationForest
import joblib
import warnings
warnings.filterwarnings("ignore")

# =========================================================
# 1. LOAD DATA
# =========================================================
print("=" * 60)
print("AquaNexus ML Training Pipeline")
print("=" * 60)

DATA_PATH = "sample_data.csv"
MODEL_DIR = "models"
os.makedirs(MODEL_DIR, exist_ok=True)

if not os.path.exists(DATA_PATH):
    print(f"[!] {DATA_PATH} not found. Running generate_data.py first...")
    exec(open("generate_data.py").read())

df = pd.read_csv(DATA_PATH, parse_dates=["date"])
print(f"\n[✓] Loaded {len(df)} records from {DATA_PATH}")
print(f"    Industries: {df['industry_id'].nunique()}")
print(f"    Date range: {df['date'].min().date()} to {df['date'].max().date()}")

# =========================================================
# 2. FEATURE ENGINEERING
# =========================================================
print("\n[→] Feature Engineering...")

# Time-based features
df["day_of_week"] = df["date"].dt.dayofweek       # 0=Mon, 6=Sun
df["month"] = df["date"].dt.month
df["day_of_month"] = df["date"].dt.day
df["is_weekend"] = (df["day_of_week"] >= 5).astype(int)

# Derived ratios
df["reuse_percentage"] = (df["reused_water"] / df["fresh_water_consumed"].replace(0, 1)) * 100
df["cto_utilization"] = (df["fresh_water_consumed"] / df["cto_limit"].replace(0, 1)) * 100
df["waste_ratio"] = df["wastewater_generated"] / df["fresh_water_consumed"].replace(0, 1)

# Encode department
le_dept = LabelEncoder()
df["department_encoded"] = le_dept.fit_transform(df["department"])
joblib.dump(le_dept, os.path.join(MODEL_DIR, "label_encoder_dept.pkl"))

# Sort by industry and date for lag features
df = df.sort_values(["industry_id", "date"]).reset_index(drop=True)

# Lag features (per industry)
for lag in [1, 3, 7]:
    df[f"fresh_lag_{lag}"] = df.groupby("industry_id")["fresh_water_consumed"].shift(lag)
    df[f"reused_lag_{lag}"] = df.groupby("industry_id")["reused_water"].shift(lag)

# Rolling averages (per industry)
for window in [7, 14]:
    df[f"fresh_rolling_{window}d"] = (
        df.groupby("industry_id")["fresh_water_consumed"]
        .transform(lambda x: x.rolling(window, min_periods=1).mean())
    )
    df[f"reused_rolling_{window}d"] = (
        df.groupby("industry_id")["reused_water"]
        .transform(lambda x: x.rolling(window, min_periods=1).mean())
    )

# Target: next-day values (shift -1)
df["target_fresh"] = df.groupby("industry_id")["fresh_water_consumed"].shift(-1)
df["target_reused"] = df.groupby("industry_id")["reused_water"].shift(-1)

# Drop rows with NaN targets (last day per industry)
df_train = df.dropna(subset=["target_fresh", "target_reused"]).copy()
# Also drop rows where lag features are NaN (first 7 days per industry)
df_train = df_train.dropna().copy()

print(f"[✓] Training samples after feature engineering: {len(df_train)}")

# =========================================================
# 3. DEFINE FEATURES
# =========================================================
FEATURE_COLS = [
    "industry_id", "department_encoded",
    "day_of_week", "month", "day_of_month", "is_weekend",
    "fresh_water_consumed", "wastewater_generated", "reused_water", "cto_limit",
    "reuse_percentage", "cto_utilization", "waste_ratio",
    "fresh_lag_1", "fresh_lag_3", "fresh_lag_7",
    "reused_lag_1", "reused_lag_3", "reused_lag_7",
    "fresh_rolling_7d", "fresh_rolling_14d",
    "reused_rolling_7d", "reused_rolling_14d"
]

# Save feature list for serving
joblib.dump(FEATURE_COLS, os.path.join(MODEL_DIR, "feature_columns.pkl"))

X = df_train[FEATURE_COLS]
y_fresh = df_train["target_fresh"]
y_reused = df_train["target_reused"]

# =========================================================
# 4. TRAIN XGBOOST — Fresh Water Prediction
# =========================================================
print("\n" + "=" * 60)
print("Training XGBoost — Fresh Water Consumption Predictor")
print("=" * 60)

X_train, X_test, y_train, y_test = train_test_split(X, y_fresh, test_size=0.2, random_state=42)

model_fresh = XGBRegressor(
    n_estimators=200,
    max_depth=6,
    learning_rate=0.1,
    subsample=0.8,
    colsample_bytree=0.8,
    random_state=42,
    verbosity=0
)
model_fresh.fit(X_train, y_train)

preds_fresh = model_fresh.predict(X_test)
mae = mean_absolute_error(y_test, preds_fresh)
rmse = np.sqrt(mean_squared_error(y_test, preds_fresh))
r2 = r2_score(y_test, preds_fresh)

print(f"\n  Results (Fresh Water):")
print(f"  ├── MAE:  {mae:.2f} liters")
print(f"  ├── RMSE: {rmse:.2f} liters")
print(f"  └── R²:   {r2:.4f}")

joblib.dump(model_fresh, os.path.join(MODEL_DIR, "xgb_fresh_water.pkl"))
print(f"  [✓] Saved → models/xgb_fresh_water.pkl")

# =========================================================
# 5. TRAIN XGBOOST — Reused Water Prediction
# =========================================================
print("\n" + "=" * 60)
print("Training XGBoost — Reused Water Predictor")
print("=" * 60)

X_train_r, X_test_r, y_train_r, y_test_r = train_test_split(X, y_reused, test_size=0.2, random_state=42)

model_reused = XGBRegressor(
    n_estimators=200,
    max_depth=6,
    learning_rate=0.1,
    subsample=0.8,
    colsample_bytree=0.8,
    random_state=42,
    verbosity=0
)
model_reused.fit(X_train_r, y_train_r)

preds_reused = model_reused.predict(X_test_r)
mae_r = mean_absolute_error(y_test_r, preds_reused)
rmse_r = np.sqrt(mean_squared_error(y_test_r, preds_reused))
r2_r = r2_score(y_test_r, preds_reused)

print(f"\n  Results (Reused Water):")
print(f"  ├── MAE:  {mae_r:.2f} liters")
print(f"  ├── RMSE: {rmse_r:.2f} liters")
print(f"  └── R²:   {r2_r:.4f}")

joblib.dump(model_reused, os.path.join(MODEL_DIR, "xgb_reused_water.pkl"))
print(f"  [✓] Saved → models/xgb_reused_water.pkl")

# =========================================================
# 6. TRAIN ISOLATION FOREST — Anomaly Detection
# =========================================================
print("\n" + "=" * 60)
print("Training Isolation Forest — Anomaly Detector")
print("=" * 60)

ANOMALY_FEATURES = [
    "fresh_water_consumed", "wastewater_generated", "reused_water",
    "reuse_percentage", "cto_utilization", "waste_ratio"
]

X_anomaly = df[ANOMALY_FEATURES].dropna()

iso_forest = IsolationForest(
    n_estimators=100,
    contamination=0.05,  # Expect ~5% anomalies
    random_state=42,
    verbose=0
)
iso_forest.fit(X_anomaly)

# Predict on full dataset
anomaly_labels = iso_forest.predict(X_anomaly)
anomaly_scores = iso_forest.decision_function(X_anomaly)

n_anomalies = (anomaly_labels == -1).sum()
n_normal = (anomaly_labels == 1).sum()

print(f"\n  Results:")
print(f"  ├── Total records:   {len(X_anomaly)}")
print(f"  ├── Normal:          {n_normal} ({n_normal/len(X_anomaly)*100:.1f}%)")
print(f"  └── Anomalies:       {n_anomalies} ({n_anomalies/len(X_anomaly)*100:.1f}%)")

joblib.dump(iso_forest, os.path.join(MODEL_DIR, "isolation_forest.pkl"))
joblib.dump(ANOMALY_FEATURES, os.path.join(MODEL_DIR, "anomaly_features.pkl"))
print(f"  [✓] Saved → models/isolation_forest.pkl")

# =========================================================
# 7. SUMMARY
# =========================================================
print("\n" + "=" * 60)
print("Training Complete!")
print("=" * 60)
print(f"""
  Models saved in '{MODEL_DIR}/' directory:
  ├── xgb_fresh_water.pkl      (XGBoost — fresh water predictor)
  ├── xgb_reused_water.pkl     (XGBoost — reused water predictor)
  ├── isolation_forest.pkl     (Isolation Forest — anomaly detector)
  ├── label_encoder_dept.pkl   (Department label encoder)
  ├── feature_columns.pkl      (Feature column list)
  └── anomaly_features.pkl     (Anomaly feature list)

  Next: Run 'python app.py' to start the FastAPI ML service.
""")
