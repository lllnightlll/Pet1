import numpy as np
import pandas as pd
from pathlib import Path

# run: & "C:\Users\lllnightlll\anaconda3\envs\llm\python.exe" main.py

series = pd.Series(np.linspace(0, 20, 30))
new_series = series[series % 2 != 0]

print(series.to_string())
print(f"Series size with odd elements: {new_series.size}")
print(f"Sum of odd elements: {float(new_series.sum())}")

DATA_DIR = Path(__file__).resolve().parent / "data"

tr_mcc_codes = pd.read_csv(DATA_DIR / "tr_mcc_codes.csv", sep=";")
transactions = pd.read_csv(DATA_DIR / "transactions.csv", sep=",", nrows=500_000).set_index("customer_id")

customer_id = int(transactions["amount"].abs().idxmax())
max_abs_amount = float(transactions["amount"].abs().max())

abs_amounts = transactions.loc[customer_id, "amount"].abs()
most_frequent_abs_amount = float(abs_amounts.value_counts().index[0])

print("\n\n=== tr_mcc_codes ===")
print(f"shape: {tr_mcc_codes.shape}")
print(tr_mcc_codes.head().to_string(index=False))
print(tr_mcc_codes.info())
print("\n=== transactions ===")
print(f"shape: {transactions.shape}")
print(transactions.reset_index().head().to_string(index=False))
print(transactions.info())
print(f"\nCustomer with max |amount|: {customer_id}")
print(f"Max |amount|: {max_abs_amount}")
print(f"Most frequent |amount| for this customer: {most_frequent_abs_amount}")