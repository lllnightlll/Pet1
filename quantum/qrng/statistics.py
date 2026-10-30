from __future__ import annotations

import math
from collections import Counter
from dataclasses import dataclass

from qrng.config import QuantumRngConfig


@dataclass(frozen=True)
class DistributionReport:
    sample_size: int
    unique_values: int
    observed_min: int
    observed_max: int
    mean: float
    std: float
    theoretical_mean: float
    theoretical_std: float
    shannon_entropy_bits: float
    max_entropy_bits: float
    chi_square: float
    chi_square_degrees: int
    acceptance_rate: float
    theoretical_acceptance_rate: float
    samples: list[int]


class DistributionAnalyzer:
    def __init__(self, config: QuantumRngConfig) -> None:
        self._config = config

    def analyze(
        self,
        accepted: Counter[int],
        acceptance_rate: float,
    ) -> DistributionReport:
        samples = self._expand_samples(accepted)
        if not samples:
            raise ValueError("После отбраковки выборка пуста — увеличьте shots")

        sample_size = len(samples)
        mean = sum(samples) / sample_size
        variance = sum((value - mean) ** 2 for value in samples) / sample_size
        std = math.sqrt(variance)

        range_size = self._config.range_size
        theoretical_mean = (self._config.min_value + self._config.max_value) / 2.0
        theoretical_variance = (range_size**2 - 1) / 12.0
        theoretical_std = math.sqrt(theoretical_variance)

        entropy = self._shannon_entropy(accepted, sample_size)
        max_entropy = math.log2(range_size)
        chi_square = self._chi_square(accepted, sample_size, range_size)

        return DistributionReport(
            sample_size=sample_size,
            unique_values=len(accepted),
            observed_min=min(samples),
            observed_max=max(samples),
            mean=mean,
            std=std,
            theoretical_mean=theoretical_mean,
            theoretical_std=theoretical_std,
            shannon_entropy_bits=entropy,
            max_entropy_bits=max_entropy,
            chi_square=chi_square,
            chi_square_degrees=range_size - 1,
            acceptance_rate=acceptance_rate,
            theoretical_acceptance_rate=range_size / self._config.raw_space_size,
            samples=samples,
        )

    def _expand_samples(self, accepted: Counter[int]) -> list[int]:
        samples: list[int] = []
        for value in range(self._config.min_value, self._config.max_value + 1):
            samples.extend([value] * accepted.get(value, 0))
        return samples

    def _shannon_entropy(self, accepted: Counter[int], sample_size: int) -> float:
        entropy = 0.0
        for frequency in accepted.values():
            if frequency <= 0:
                continue
            probability = frequency / sample_size
            entropy -= probability * math.log2(probability)
        return entropy

    def _chi_square(
        self,
        accepted: Counter[int],
        sample_size: int,
        range_size: int,
    ) -> float:
        expected = sample_size / range_size
        statistic = 0.0
        for value in range(self._config.min_value, self._config.max_value + 1):
            observed = accepted.get(value, 0)
            statistic += (observed - expected) ** 2 / expected
        return statistic
