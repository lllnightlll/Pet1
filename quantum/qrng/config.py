from __future__ import annotations

import math
from dataclasses import dataclass


@dataclass(frozen=True)
class QuantumRngConfig:
    min_value: int = 0
    max_value: int = 255
    shots: int = 16384
    seed: int | None = 42
    histogram_bins: int | None = None

    def __post_init__(self) -> None:
        if self.min_value < 0:
            raise ValueError("min_value не может быть отрицательным")
        if self.max_value < self.min_value:
            raise ValueError("max_value должен быть не меньше min_value")
        if self.shots <= 0:
            raise ValueError("shots должен быть положительным")

    @property
    def range_size(self) -> int:
        return self.max_value - self.min_value + 1

    @property
    def qubit_count(self) -> int:
        return math.ceil(math.log2(self.max_value + 1))

    @property
    def raw_space_size(self) -> int:
        return 1 << self.qubit_count

    @property
    def bin_count(self) -> int:
        if self.histogram_bins is not None:
            return self.histogram_bins
        return self.range_size
