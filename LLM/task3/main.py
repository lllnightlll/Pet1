import matplotlib.pyplot as plt
import pandas as pd
from pathlib import Path

#   /home/night/Downloads/Pet1/LLM/.venv/bin/python /home/night/Downloads/Pet1/LLM/task3/main.py

DATA_DIR = Path(__file__).resolve().parent.parent / "task2" / "data"

tr_mcc_codes = pd.read_csv(DATA_DIR / "tr_mcc_codes.csv", sep=";")
tr_types = pd.read_csv(DATA_DIR / "tr_types.csv", sep=";")
transactions = pd.read_csv(DATA_DIR / "transactions.csv", sep=",", nrows=1_000_000)
customers_gender_train = pd.read_csv(DATA_DIR / "gender_train.csv", sep=",")

x = (
    transactions.merge(customers_gender_train, on="customer_id", how="left")
    .merge(tr_mcc_codes, on="mcc_code", how="inner")
    .merge(tr_types, on="tr_type", how="inner")
)
print(f"Size: {x.size}")
print(x.head(3).to_string(index=False, max_colwidth=40)) 

# x["mcc_code"].astype(str) + x["tr_type"].astype(str)
x["mcc_code+tr_type"] = x.apply(
    lambda row: str(row["mcc_code"]) + str(row["tr_type"]), axis=1
)
print(x.head().to_string(index=False, max_colwidth=40))
print(f"Size: {x['mcc_code+tr_type'].size}")

new_x = x[x["amount"] < 0]
print(f"New size: {new_x.size}")

stats = new_x.groupby("mcc_code+tr_type")["amount"].agg(["count", "var"])

filt = stats[stats["count"] >= 5]

disp = round(filt["var"].max() / filt["var"].min())
print(disp)

TOP_MCC_COUNT = 10
CHART_SIZE = (14, 8)
CHART_PATH = Path(__file__).resolve().parent / "mcc_gender_stacked_barh.png"
LABEL_MAX_LEN = 55

gender = x.dropna(subset=["gender"]).copy()
top_mcc = gender["mcc_description"].value_counts().head(TOP_MCC_COUNT).index
top_rows = gender[gender["mcc_description"].isin(top_mcc)]

chart_df = pd.crosstab(top_rows["mcc_description"], top_rows["gender"]).reindex(top_mcc)
chart_df.columns = [f"Gender {int(gender)}" for gender in chart_df.columns]
chart_df.index = [
    description if len(description) <= LABEL_MAX_LEN else description[: LABEL_MAX_LEN - 1] + "…"
    for description in chart_df.index
]

axes = chart_df.plot.barh(stacked=True, figsize=CHART_SIZE)
axes.set_title("Composition of transactions by gender for popular MCC categories")
axes.set_xlabel("Number of transactions")
axes.set_ylabel("MCC category")
axes.invert_yaxis()
axes.legend(title="Series")
plt.tight_layout()
plt.savefig(CHART_PATH, dpi=150)
print(f"Chart saved: {CHART_PATH}")
plt.show()

