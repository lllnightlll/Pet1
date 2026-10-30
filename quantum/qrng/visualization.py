from __future__ import annotations

from collections import Counter
from pathlib import Path

import matplotlib.pyplot as plt

from qiskit import QuantumCircuit

from qrng.config import QuantumRngConfig
from qrng.statistics import DistributionReport


class ExperimentVisualizer:
    def __init__(self, output_dir: Path) -> None:
        self._output_dir = output_dir
        self._output_dir.mkdir(parents=True, exist_ok=True)

    def save_circuit(self, circuit: QuantumCircuit) -> Path:
        path = self._output_dir / "circuit.txt"
        path.write_text(str(circuit.draw(output="text")), encoding="utf-8")
        return path

    def save_histogram(
        self,
        config: QuantumRngConfig,
        accepted: Counter[int],
        report: DistributionReport,
    ) -> Path:
        values = list(range(config.min_value, config.max_value + 1))
        frequencies = [accepted.get(value, 0) for value in values]
        expected = report.sample_size / config.range_size

        figure, axes = plt.subplots(figsize=(12, 5))
        axes.bar(values, frequencies, width=1.0, color="#4C78A8", edgecolor="none")
        axes.axhline(
            expected,
            color="#E45756",
            linestyle="--",
            linewidth=1.5,
            label=f"ожидание N/K = {expected:.2f}",
        )
        axes.set_title("Квантовый ГСЧ: распределение после гейта Адамара")
        axes.set_xlabel("значение")
        axes.set_ylabel("частота")
        axes.set_xlim(config.min_value - 1, config.max_value + 1)
        axes.legend()
        figure.tight_layout()

        path = self._output_dir / "histogram.png"
        figure.savefig(path, dpi=140)
        plt.close(figure)
        return path
