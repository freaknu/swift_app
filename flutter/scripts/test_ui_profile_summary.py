import json
import unittest

from summarize_ui_profile import summarize


def log(*, rate=120.0, misses=0.5, mode="profile", frames=240, discarded=0):
    return "flutter: SWIFT_UI_PROFILE " + json.dumps({
        "mode": mode, "window": 1, "frameCount": frames,
        "displayRefreshRateHz": rate, "discardedSamples": discarded,
        "target120HzBudget": {"eitherStageOverBudgetPercent": misses},
    })


class ProfileSummaryTest(unittest.TestCase):
    def test_exports_only_timing_lines_and_never_unrelated_log_data(self):
        report = summarize(["unrelated private log", log()])
        self.assertEqual(report["frameCount"], 240)
        self.assertNotIn("private", json.dumps(report))

    def test_slow_window_cannot_be_hidden_by_fast_windows(self):
        report = summarize([log(), log(misses=5)], max_miss_percent=1)
        self.assertFalse(report["gate"]["passed"])

    def test_rejects_debug_missing_and_malformed_samples(self):
        for lines in ([log(mode="debug")], [], ["SWIFT_UI_PROFILE broken"]):
            with self.assertRaises(ValueError):
                summarize(lines)

    def test_120hz_gate_rejects_lower_or_unknown_refresh_rates(self):
        for rate in (60, None, float("nan")):
            self.assertFalse(summarize([log(rate=rate)], require_120hz=True)["gate"]["passed"])

    def test_gate_rejects_incomplete_or_idle_capture(self):
        for line in (log(frames=10), log(discarded=1)):
            self.assertFalse(summarize([line], max_miss_percent=1)["gate"]["passed"])

    def test_per_window_budget_gate_passes_valid_120hz_capture(self):
        self.assertTrue(summarize([log()], require_120hz=True, max_miss_percent=1)["gate"]["passed"])


if __name__ == "__main__":
    unittest.main()
