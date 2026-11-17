import os
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from sklearn.cluster import KMeans
from ucimlrepo import fetch_ucirepo

# python LLM\task6\main.py

# fetch dataset 
online_retail = fetch_ucirepo(id=352)

# data (as pandas dataframes) 
X = online_retail.data.features
y = online_retail.data.targets

# metadata 
print(online_retail.metadata)

# variable information 
print(online_retail.variables)

# targets
print(y)

raw = X.copy()
raw["InvoiceNo"] = online_retail.data.ids["InvoiceNo"]
raw["InvoiceDate"] = pd.to_datetime(raw["InvoiceDate"])
print(raw.head())
print("строк в файле:", len(raw))

raw = raw.dropna(subset=["CustomerID"])
raw = raw[(raw["Quantity"] > 0) & (raw["UnitPrice"] > 0)]
raw["sum"] = raw["Quantity"] * raw["UnitPrice"]

orders = raw.groupby(["CustomerID", "InvoiceNo"], as_index=False).agg(
    date=("InvoiceDate", "max"),
    check=("sum", "sum"),
)
snap = orders["date"].max() + pd.Timedelta(days=1)
rfm = pd.DataFrame({
    "recency": (snap - orders.groupby("CustomerID")["date"].max()).dt.days,
    "frequency": orders.groupby("CustomerID")["InvoiceNo"].nunique(),
    "check": orders.groupby("CustomerID")["check"].mean(),
}).dropna()

rfm["recency"] = rfm["recency"].clip(upper=rfm["recency"].quantile(0.99))
rfm["frequency"] = rfm["frequency"].clip(upper=rfm["frequency"].quantile(0.99))
rfm["check"] = rfm["check"].clip(upper=rfm["check"].quantile(0.99))
print(rfm.head())
print("клиентов:", len(rfm))

x = np.log1p(rfm[["recency", "frequency", "check"]].to_numpy())
x = (x - x.mean(axis=0)) / x.std(axis=0)
rfm["cluster"] = KMeans(n_clusters=4, random_state=42, n_init=10).fit_predict(x)

base = rfm[["recency", "frequency", "check"]].mean()
centers = rfm.groupby("cluster")[["recency", "frequency", "check"]].mean().sort_index()
print("средние по базе: давность", round(float(base["recency"]), 1), "частота", round(float(base["frequency"]), 2), "чек", round(float(base["check"]), 1))

for c in centers.index:
    row = centers.loc[c]
    recent = row["recency"] <= base["recency"]
    often = row["frequency"] >= base["frequency"]
    rich = row["check"] >= base["check"]
    if recent and often and rich:
        text = "Чемпионы: удерживать, бонусы за повторные заказы"
    elif recent and often:
        text = "Постоянные с небольшим чеком: комплекты и допродажа"
    elif recent and rich:
        text = "Редкие крупные заказы: напоминание и подборки"
    elif recent:
        text = "Новые и редкие: довести до второго заказа"
    elif often and rich:
        text = "Ценные, но остывшие: персональное предложение"
    elif often:
        text = "Раньше частые: реактивация по прошлым товарам"
    elif rich:
        text = "Давно и крупный чек: win-back с сильным оффером"
    else:
        text = "Спящие: дешёвое напоминание"
    n = int((rfm["cluster"] == c).sum())
    print(
        "Сегмент", int(c), "—", text + ".",
        n, "клиентов (" + format(100 * n / len(rfm), ".1f") + "%).",
        "Давность", round(float(row["recency"]), 1), "дн., частота", round(float(row["frequency"]), 2), ", средний чек", round(float(row["check"]), 1),
    )

fig, ax = plt.subplots(1, 2, figsize=(12, 5))
for c in range(4):
    m = rfm["cluster"].to_numpy() == c
    ax[0].scatter(rfm["recency"].to_numpy()[m], rfm["frequency"].to_numpy()[m], s=12, alpha=0.6, label=str(c))
ax[0].set_xlabel("давность, дни")
ax[0].set_ylabel("частота, заказы")
ax[0].set_title("клиенты в 2D, k=4")
ax[0].legend()

rel = centers / base
for i, col in enumerate(["recency", "frequency", "check"]):
    ax[1].bar(np.arange(4) + (i - 1) * 0.25, rel[col].to_numpy(), 0.25, label=col)
ax[1].axhline(1, color="gray", lw=0.8)
ax[1].set_xticks(np.arange(4))
ax[1].set_ylabel("к среднему клиенту, раз")
ax[1].set_title("профиль сегментов")
ax[1].legend()
plt.tight_layout()
out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "charts.png")
plt.savefig(out, dpi=150)
print("Chart saved:", out)
plt.show()