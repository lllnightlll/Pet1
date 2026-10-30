from __future__ import annotations

import json
import random
from pathlib import Path

from qrng.config import QuantumRngConfig
from qrng.generator import QuantumRandomNumberGenerator
from qrng.visualization import ExperimentVisualizer


RESULTS_DIR = Path(__file__).resolve().parent / "results"


def _preview_samples(samples: list[int], seed: int | None, limit: int = 32) -> list[int]:
    rng = random.Random(seed)
    if len(samples) <= limit:
        preview = list(samples)
        rng.shuffle(preview)
        return preview
    return rng.sample(samples, limit)


def run_experiment() -> None:
    config = QuantumRngConfig()
    generator = QuantumRandomNumberGenerator(config)
    accepted, report = generator.generate()

    visualizer = ExperimentVisualizer(RESULTS_DIR)
    circuit_path = visualizer.save_circuit(generator.circuit())
    histogram_path = visualizer.save_histogram(config, accepted, report)

    summary = {
        "min_value": config.min_value,
        "max_value": config.max_value,
        "qubit_count": config.qubit_count,
        "shots": config.shots,
        "seed": config.seed,
        "sample_size": report.sample_size,
        "unique_values": report.unique_values,
        "observed_min": report.observed_min,
        "observed_max": report.observed_max,
        "mean": round(report.mean, 4),
        "std": round(report.std, 4),
        "theoretical_mean": round(report.theoretical_mean, 4),
        "theoretical_std": round(report.theoretical_std, 4),
        "shannon_entropy_bits": round(report.shannon_entropy_bits, 6),
        "max_entropy_bits": round(report.max_entropy_bits, 6),
        "chi_square": round(report.chi_square, 4),
        "chi_square_degrees": report.chi_square_degrees,
        "acceptance_rate": round(report.acceptance_rate, 6),
        "theoretical_acceptance_rate": round(report.theoretical_acceptance_rate, 6),
        "preview": _preview_samples(report.samples, config.seed),
        "circuit_path": str(circuit_path.name),
        "histogram_path": str(histogram_path.name),
    }

    summary_path = RESULTS_DIR / "summary.json"
    summary_path.write_text(
        json.dumps(summary, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    print("QRNG experiment finished")
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    run_experiment()
