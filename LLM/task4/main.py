import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from pathlib import Path
from sklearn.datasets import load_iris
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix, precision_recall_fscore_support
from sklearn.model_selection import train_test_split
from sklearn.tree import DecisionTreeRegressor

# python LLM\task4\main.py

iris = load_iris()
names = list(iris.target_names)
x_train, x_test, y_train, y_test = train_test_split(
    iris.data, iris.target, test_size=0.3, random_state=42, stratify=iris.target
)
print(pd.DataFrame(iris.data, columns=iris.feature_names).head().to_string(index=False))
print(f"train {len(y_train)}, test {len(y_test)}, classes: {', '.join(names)}")

results = {}
for loss in ("deviance", "exponential"):
    classes, y = np.unique(y_train, return_inverse=True)
    k, n = len(classes), len(y)
    p = np.bincount(y, minlength=k) / n
    init = np.log(np.clip(p, 1e-12, None)) if loss == "deviance" else 0.5 * np.log((p + 1e-12) / (1 - p + 1e-12))
    scores = np.repeat(init[None, :], n, 0)
    test_scores = np.repeat(init[None, :], len(x_test), 0)
    gains = np.zeros(4)
    stages, tr, te = [], [], []
    for _ in range(60):
        tr.append(np.mean(classes[scores.argmax(1)] == y_train))
        te.append(np.mean(classes[test_scores.argmax(1)] == y_test))
        if loss == "deviance":
            z = np.exp(scores - scores.max(1, keepdims=True))
            aux = z / z.sum(1, keepdims=True)
            g = -aux.copy()
            g[np.arange(n), y] += 1
        else:
            ypm = np.where(y[:, None] == np.arange(k), 1.0, -1.0)
            aux = np.exp(-np.clip(ypm * scores, -8, 8))
            g = ypm * aux
        stage = []
        for c in range(k):
            tree = DecisionTreeRegressor(max_depth=1, min_samples_leaf=5).fit(x_train, g[:, c])
            feat, thr = int(tree.tree_.feature[0]), float(tree.tree_.threshold[0])
            left = np.ones(n, bool) if feat < 0 else x_train[:, feat] <= thr
            leafs = []
            for mask in (left, ~left):
                if not mask.any():
                    leafs.append(0.0)
                    continue
                if loss == "deviance":
                    num = float(((y == c).astype(float) - aux[:, c])[mask].sum())
                    den = float((aux[mask, c] * (1 - aux[mask, c])).sum())
                    raw = 0 if den < 1e-12 else (k - 1) / k * num / den
                else:
                    yc = np.where(y == c, 1.0, -1.0)
                    w = aux[:, c]
                    raw = 0.5 * np.log((w[(yc > 0) & mask].sum() + 1e-12) / (w[(yc < 0) & mask].sum() + 1e-12))
                leafs.append(0.0 if not np.isfinite(raw) else float(np.clip(raw, -2, 2)))
            st = {"f": max(feat, 0), "t": thr if feat >= 0 else np.inf, "l": leafs[0], "r": leafs[1]}
            stage.append(st)
            scores[:, c] += 0.25 * np.where(left, st["l"], st["r"])
            test_scores[:, c] += 0.25 * np.where(x_test[:, st["f"]] <= st["t"], st["l"], st["r"])
            if feat >= 0:
                gains[feat] += max(tree.tree_.n_node_samples[0] * tree.tree_.impurity[0]
                                   - tree.tree_.n_node_samples[1] * tree.tree_.impurity[1]
                                   - tree.tree_.n_node_samples[2] * tree.tree_.impurity[2], 0)
        stages.append(stage)
    results[loss] = {
        "init": init, "stages": stages, "classes": classes,
        "yp_tr": classes[scores.argmax(1)], "yp_te": classes[test_scores.argmax(1)],
        "tr": tr, "te": te, "imp": gains / gains.sum() if gains.sum() else np.full(4, 0.25),
    }

rows = []
for loss, m in results.items():
    print(f"\n=== {loss} ===")
    for split, yt, yp in (("train", y_train, m["yp_tr"]), ("test", y_test, m["yp_te"])):
        print(split, "\n", classification_report(yt, yp, target_names=names, digits=4, zero_division=0), sep="")
        print(confusion_matrix(yt, yp, labels=range(3)))
        prf = precision_recall_fscore_support(yt, yp, average="macro", zero_division=0)
        rows.append({"loss": loss, "split": split, "accuracy": accuracy_score(yt, yp),
                     "precision": prf[0], "recall": prf[1], "f1-score": prf[2]})
metrics = pd.DataFrame(rows)
print(metrics.to_string(index=False, float_format=lambda v: f"{v:.4f}"))
print("\nЛучше deviance. Хуже virginica (пересекается с versicolor по лепестку).")
print("Важнее petal length/width. Train=1, test ниже — умеренное переобучение.")

print("\n4 числа: чашелистик д/ш, лепесток д/ш  |  номер 0..149  |  Enter — дальше")
while True:
    line = input("> ").strip()
    if not line:
        break
    try:
        nums = [float(t.replace(",", ".")) for t in line.split()]
    except ValueError:
        print("пример: 5.1 3.5 1.4 0.2")
        continue
    if len(nums) == 1 and nums[0].is_integer() and 0 <= int(nums[0]) < 150:
        i = int(nums[0])
        row = iris.data[i]
        print(f"#{i} настоящий вид: {names[iris.target[i]]}")
    elif len(nums) == 4:
        row = np.array(nums)
    else:
        print("нужно 4 признака или номер 0..149")
        continue
    print(dict(zip(iris.feature_names, [round(float(v), 2) for v in row])))
    for loss, m in results.items():
        s = m["init"].copy()
        for stage in m["stages"]:
            for c, st in enumerate(stage):
                s[c] += 0.25 * (st["l"] if row[st["f"]] <= st["t"] else st["r"])
        print(f"{loss}: {names[int(m['classes'][s.argmax()])]}")

out = Path(__file__).resolve().parent
fig, ax = plt.subplots(2, 2, figsize=(12, 8))
metrics[metrics["split"] == "test"].set_index("loss")[["accuracy", "precision", "recall", "f1-score"]].T.plot.bar(ax=ax[0, 0])
ax[0, 0].set_title("test metrics")
ax[0, 0].set_ylim(0, 1.15)
cms = np.hstack([confusion_matrix(y_test, results[loss]["yp_te"], labels=range(3)) for loss in results])
ax[0, 1].imshow(cms, cmap="Blues")
ax[0, 1].set_title("confusion test: deviance | exponential")
ax[0, 1].set_xticks(range(6), names + names, rotation=20, ha="right")
ax[0, 1].set_yticks(range(3), names)
for i in range(3):
    for j in range(6):
        ax[0, 1].text(j, i, str(int(cms[i, j])), ha="center", va="center")
for loss, m in results.items():
    ax[1, 0].plot(m["tr"], label=f"{loss} train")
    ax[1, 0].plot(m["te"], label=f"{loss} test")
ax[1, 0].set_title("learning curves")
ax[1, 0].legend(fontsize=8)
pd.DataFrame({loss: m["imp"] for loss, m in results.items()}, index=iris.feature_names).plot.barh(ax=ax[1, 1])
ax[1, 1].set_title("feature importance")
plt.tight_layout()
plt.savefig(out / "charts.png", dpi=150)
print(f"Chart saved: {out / 'charts.png'}")
plt.show()
