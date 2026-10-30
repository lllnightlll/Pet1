from __future__ import annotations

from collections import Counter

from qrng.config import QuantumRngConfig


class MeasurementDecoder:
    def decode_counts(self, counts: dict[str, int]) -> Counter[int]:
        decoded: Counter[int] = Counter()
        for bitstring, frequency in counts.items():
            clean = bitstring.replace(" ", "")
            decoded[int(clean, 2)] += frequency
        return decoded


class RangeRejectionFilter:
    def __init__(self, config: QuantumRngConfig) -> None:
        self._config = config

    def apply(self, raw_values: Counter[int]) -> Counter[int]:
        accepted: Counter[int] = Counter()
        for value, frequency in raw_values.items():
            if self._config.min_value <= value <= self._config.max_value:
                accepted[value] += frequency
        return accepted

    def acceptance_rate(self, raw_total: int, accepted_total: int) -> float:
        if raw_total == 0:
            raise ValueError("Нет измерений для оценки доли принятых")
        return accepted_total / raw_total
