import json
from pathlib import Path

GROUND_TRUTH_PATH = Path(__file__).parent / "ground_truth.json"
ALLOWED_DIMENSIONS = {"factual", "synthesis", "completeness"}


def test_schema_contract():
    entries = json.loads(GROUND_TRUTH_PATH.read_text(encoding="utf-8"))

    for entry in entries:
        assert "id" in entry, f"Missing 'id' in entry: {entry}"
        assert "question" in entry, f"Missing 'question' in {entry['id']}"

        dimensions = entry.get("eval_dimensions", ["factual"])
        assert set(dimensions) <= ALLOWED_DIMENSIONS, \
            f"{entry['id']}: invalid dimensions {dimensions}"

        if any(d in dimensions for d in ("synthesis", "completeness")):
            assert entry.get("gold_answer"), \
                f"{entry['id']} has synthesis/completeness dimension but no gold_answer"


def test_has_synthesis_and_completeness_pairs():
    entries = json.loads(GROUND_TRUTH_PATH.read_text(encoding="utf-8"))
    ids = {e["id"] for e in entries}

    synthesis_ids = {"S1", "S2", "S3", "S4"}
    completeness_ids = {"C1", "C2", "C3", "C4"}

    assert synthesis_ids <= ids, f"Missing synthesis pairs: {synthesis_ids - ids}"
    assert completeness_ids <= ids, f"Missing completeness pairs: {completeness_ids - ids}"


def test_total_entry_count():
    entries = json.loads(GROUND_TRUTH_PATH.read_text(encoding="utf-8"))
    assert len(entries) == 16, f"Expected 16 entries (8 original + 8 new), got {len(entries)}"
