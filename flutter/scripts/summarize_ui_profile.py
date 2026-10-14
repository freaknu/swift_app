#!/usr/bin/env python3
"""Export timing-only opt-in Flutter logs; optional per-window budget gate."""
import argparse
import json
import math
from pathlib import Path

PREFIX = "SWIFT_UI_PROFILE "


def summarize(lines, *, require_120hz=False, max_miss_percent=None):
    windows = []
    for line in lines:
        if PREFIX not in line:
            continue
        window = json.loads(line.split(PREFIX, 1)[1])
        if window.get("mode") != "profile":
            raise ValueError("Only profile-mode timings can be evaluated")
        windows.append(window)
    if not windows:
        raise ValueError("No SWIFT_UI_PROFILE timing windows found")
    failures = []
    for window in windows:
        label = f"window {window.get('window')}"
        rate = window.get("displayRefreshRateHz")
        if require_120hz and (
            not isinstance(rate, (int, float)) or not math.isfinite(rate) or rate < 119
        ):
            failures.append(f"{label}: display did not report 120 Hz")
        if window.get("discardedSamples", 0):
            failures.append(f"{label}: timing buffer overflowed")
        if max_miss_percent is not None:
            if window.get("frameCount", 0) < 120:
                failures.append(f"{label}: fewer than 120 frames; repeat an animated workload")
            percentage = window.get("target120HzBudget", {}).get("eitherStageOverBudgetPercent")
            if (
                not isinstance(percentage, (int, float))
                or not math.isfinite(percentage)
                or percentage > max_miss_percent
            ):
                failures.append(f"{label}: UI/raster 8.33 ms budget misses exceed {max_miss_percent}%")
    return {
        "schemaVersion": 1,
        "source": "Opt-in PROFILE_UI engine frame recording",
        "frameCount": sum(window.get("frameCount", 0) for window in windows),
        "windows": windows,
        "gate": {
            "requested": require_120hz or max_miss_percent is not None,
            "require120HzDisplay": require_120hz,
            "maxStageMissPercentPerWindow": max_miss_percent,
            "passed": not failures,
            "failures": failures,
        },
        "limitations": [
            "Engine UI/raster durations do not measure presented FPS or native map frames.",
            "Display refresh rate alone does not establish that this is a physical mobile device.",
            "Results cover only actions actually performed during the captured windows.",
        ],
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("log", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--require-120hz", action="store_true")
    parser.add_argument("--max-stage-miss-percent", type=float)
    args = parser.parse_args()
    if args.max_stage_miss_percent is not None and not (
        math.isfinite(args.max_stage_miss_percent) and 0 <= args.max_stage_miss_percent <= 100
    ):
        parser.error("--max-stage-miss-percent must be between 0 and 100")
    try:
        with args.log.open(encoding="utf-8") as source:
            report = summarize(
                source,
                require_120hz=args.require_120hz,
                max_miss_percent=args.max_stage_miss_percent,
            )
    except (OSError, ValueError, TypeError, AttributeError) as error:
        parser.exit(2, f"Unable to evaluate timings: {error}\n")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(f"Saved {report['frameCount']} frame samples to {args.output}")
    for failure in report["gate"]["failures"]:
        print(failure)
    return 0 if report["gate"]["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
