"""
=============================================================
AquaNexus — Sample Data Generator
Generates 365 days of realistic water usage records for
multiple industries/departments for ML training.
=============================================================
"""
import csv
import random
import math
from datetime import datetime, timedelta

random.seed(42)

INDUSTRIES = [
    {"id": 1, "name": "North Assembly", "base_fresh": 1200, "base_reuse": 800, "cto_limit": 2000},
    {"id": 2, "name": "South Processing", "base_fresh": 3000, "base_reuse": 1000, "cto_limit": 4500},
    {"id": 3, "name": "East Cooling", "base_fresh": 850, "base_reuse": 700, "cto_limit": 1500},
    {"id": 4, "name": "West Packaging", "base_fresh": 1500, "base_reuse": 200, "cto_limit": 2500},
]

DEPARTMENTS = ["Production", "Cooling", "Cleaning", "Utilities"]

def generate_data():
    rows = []
    start_date = datetime(2025, 1, 1)

    for industry in INDUSTRIES:
        for day_offset in range(365):
            current_date = start_date + timedelta(days=day_offset)
            dow = current_date.weekday()  # 0=Mon, 6=Sun

            dept = random.choice(DEPARTMENTS)

            # Seasonal pattern (higher in summer months May-Aug)
            month = current_date.month
            seasonal = 1.0 + 0.15 * math.sin((month - 1) * math.pi / 6)

            # Weekend reduction
            weekend_factor = 0.6 if dow >= 5 else 1.0

            # Daily noise
            noise = random.uniform(0.85, 1.15)

            fresh = round(industry["base_fresh"] * seasonal * weekend_factor * noise, 1)
            wastewater = round(fresh * random.uniform(0.6, 0.85), 1)
            reused = round(industry["base_reuse"] * seasonal * weekend_factor * random.uniform(0.8, 1.2), 1)

            # Inject ~3% anomalies (leaks/spikes)
            if random.random() < 0.03:
                fresh = round(fresh * random.uniform(1.8, 2.5), 1)
                wastewater = round(wastewater * random.uniform(1.5, 2.0), 1)

            rows.append({
                "id": len(rows) + 1,
                "industry_id": industry["id"],
                "department": dept,
                "date": current_date.strftime("%Y-%m-%d"),
                "fresh_water_consumed": fresh,
                "wastewater_generated": wastewater,
                "reused_water": reused,
                "cto_limit": industry["cto_limit"]
            })

    return rows

if __name__ == "__main__":
    data = generate_data()
    filepath = "sample_data.csv"
    fieldnames = ["id", "industry_id", "department", "date",
                  "fresh_water_consumed", "wastewater_generated",
                  "reused_water", "cto_limit"]

    with open(filepath, "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(data)

    print(f"Generated {len(data)} records -> {filepath}")
