from __future__ import annotations

from qiskit import QuantumCircuit, transpile
from qiskit_aer import AerSimulator

from qrng.config import QuantumRngConfig


class AerCircuitExecutor:
    def __init__(self, config: QuantumRngConfig) -> None:
        self._config = config
        self._backend = AerSimulator(seed_simulator=config.seed)

    def run(self, circuit: QuantumCircuit) -> dict[str, int]:
        compiled = transpile(circuit, self._backend)
        job = self._backend.run(compiled, shots=self._config.shots)
        result = job.result()
        return dict(result.get_counts())
