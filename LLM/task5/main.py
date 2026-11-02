import json
import urllib.request
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from pathlib import Path
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler

# python LLM\task5\main.py

url = "https://query1.finance.yahoo.com/v8/finance/chart/BTC-USD?interval=1d&range=5y"
req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
page = urllib.request.urlopen(req)
stuff = json.loads(page.read())
r = stuff["chart"]["result"][0]
q = r["indicators"]["quote"][0]
data = pd.DataFrame({
    "date": r["timestamp"],
    "open": q["open"],
    "high": q["high"],
    "low": q["low"],
    "close": q["close"],
    "volume": q["volume"],
})
page.close()

url2 = "https://api.alternative.me/fng/?limit=0&format=json"
req2 = urllib.request.Request(url2, headers={"User-Agent": "Mozilla/5.0"})
page2 = urllib.request.urlopen(req2)
stuff2 = json.loads(page2.read())
news = pd.DataFrame(stuff2["data"])
page2.close()

data["date"] = pd.to_datetime(data["date"], unit="s").dt.normalize()
news["date"] = pd.to_datetime(news["timestamp"], unit="s").dt.normalize()
news = news[["date", "value"]]

df = data.merge(news, on="date", how="left")
df = df.dropna(subset=["open", "high", "low", "close", "volume"])
df["value"] = df["value"].fillna(0)
df["izm"] = (df["high"] - df["low"]) / df["open"]

print(df.head())
print("строк:", len(df))

x = df[["open", "high", "low", "volume", "value", "izm"]]
y = df["close"]
x_s = StandardScaler().fit_transform(x)

x_train, x_test, y_train, y_test = train_test_split(x_s, y, test_size=0.3, random_state=42)
print("train", len(y_train), "test", len(y_test))

model = LinearRegression()
model.fit(x_train, y_train)

pred_tr = model.predict(x_train)
pred = model.predict(x_test)

print("train MAE", mean_absolute_error(y_train, pred_tr), "RMSE", mean_squared_error(y_train, pred_tr) ** 0.5, "R2", r2_score(y_train, pred_tr))
print("test MAE", mean_absolute_error(y_test, pred), "RMSE", mean_squared_error(y_test, pred) ** 0.5, "R2", r2_score(y_test, pred))

print("коэфы:", list(zip(x.columns, model.coef_)))
print("intercept:", model.intercept_)
print("open high low volume value izm")
while True:
    line = input(" > ").strip()
    if not line:
        break
    try:
        nums = [float(t.replace(",", ".")) for t in line.split()]
    except ValueError:
        continue
    if len(nums) == 1 and nums[0].is_integer() and 0 <= int(nums[0]) < len(df):
        i = int(nums[0])
        row = StandardScaler().transform([x.iloc[i]])
        print("настоящий close", float(y.iloc[i]))
        print("прогноз", float(model.predict(row)[0]))
    elif len(nums) == 6:
        print("прогноз", float(model.predict(StandardScaler().transform([nums]))[0]))
    else:
        print("надо 6 чисел или номер")

out = Path(__file__).resolve().parent
fig, ax = plt.subplots(2, 2, figsize=(12, 8))
ax[0, 0].plot(y_test.to_numpy(), label="факт")
ax[0, 0].plot(pred, label="прогноз")
ax[0, 0].set_title("test close")
ax[0, 0].legend()
ax[0, 1].scatter(y_test, pred)
ax[0, 1].set_title("факт vs прогноз")
ax[1, 0].scatter(pred, y_test.to_numpy() - pred)
ax[1, 0].axhline(0)
ax[1, 0].set_title("остатки")
ax[1, 1].barh(list(x.columns), model.coef_)
ax[1, 1].set_title("коэфы")
plt.tight_layout()
plt.savefig(out / "charts.png", dpi=150)
print("Chart saved:", out / "charts.png")
plt.show()