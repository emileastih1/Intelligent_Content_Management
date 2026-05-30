import json
import pytest


@pytest.fixture
def ground_truth():
    with open("eval/ground_truth.json") as f:
        return json.load(f)


def test_total_entry_count(ground_truth):
    assert len(ground_truth) == 16


def test_synthesis_and_completeness_entries_have_gold_answer(ground_truth):
    for entry in ground_truth:
        dims = entry.get("eval_dimensions", ["factual"])
        if "synthesis" in dims or "completeness" in dims:
            assert entry.get("gold_answer"), f"Missing gold_answer for {entry['id']}"


def test_eval_dimensions_values_are_valid(ground_truth):
    allowed = {"factual", "synthesis", "completeness"}
    for entry in ground_truth:
        dims = entry.get("eval_dimensions", ["factual"])
        for d in dims:
            assert d in allowed, f"Invalid dimension '{d}' in {entry['id']}"


def test_synthesis_entries_have_synthesis_dimension(ground_truth):
    synthesis_ids = {"S1", "S2", "S3", "S4"}
    for entry in ground_truth:
        if entry["id"] in synthesis_ids:
            assert "synthesis" in entry.get("eval_dimensions", [])


def test_completeness_entries_have_completeness_dimension(ground_truth):
    completeness_ids = {"C1", "C2", "C3", "C4"}
    for entry in ground_truth:
        if entry["id"] in completeness_ids:
            assert "completeness" in entry.get("eval_dimensions", [])
