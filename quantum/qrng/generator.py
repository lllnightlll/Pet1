from __future__ import annotations

from collections import Counter

from qiskit import QuantumCircuit

from qrng.circuit import HadamardCircuitBuilder
from qrng.config import QuantumRngConfig
from qrng.decoder import MeasurementDecoder, RangeRejectionFilter
from qrng.simulator import AerCircuitExecutor
from qrng.statistics import DistributionAnalyzer, DistributionReport


class QuantumRandomNumberGenerator:
    def __init__(self, config: QuantumRngConfig | None = None) -> None:
        self._config = config or QuantumRngConfig()
        self._builder = HadamardCircuitBuilder(self._config)
        self._executor = AerCircuitExecutor(self._config)
        self._decoder = MeasurementDecoder()
        self._rejection = RangeRejectionFilter(self._config)
        self._analyzer = DistributionAnalyzer(self._config)
        self._circuit: QuantumCircuit | None = None

    @property
    def config(self) -> QuantumRngConfig:
        return self._config

    def circuit(self) -> QuantumCircuit:
        if self._circuit is None:
            self._circuit = self._builder.build()
        return self._circuit

    def generate(self) -> tuple[Counter[int], DistributionReport]:
        counts = self._executor.run(self.circuit())
        raw_values = self._decoder.decode_counts(counts)
        accepted = self._rejection.apply(raw_values)

        raw_total = sum(raw_values.values())
        accepted_total = sum(accepted.values())
        acceptance_rate = self._rejection.acceptance_rate(raw_total, accepted_total)

        report = self._analyzer.analyze(accepted, acceptance_rate)
        return accepted, report
