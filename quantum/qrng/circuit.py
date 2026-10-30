from __future__ import annotations

from qiskit import QuantumCircuit

from qrng.config import QuantumRngConfig


class HadamardCircuitBuilder:
    def __init__(self, config: QuantumRngConfig) -> None:
        self._config = config

    def build(self) -> QuantumCircuit:
        qubit_count = self._config.qubit_count
        circuit = QuantumCircuit(qubit_count, qubit_count, name="hadamard_qrng")
        circuit.h(range(qubit_count))
        circuit.barrier()
        circuit.measure(range(qubit_count), range(qubit_count))
        return circuit
