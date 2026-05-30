"""Tests for eval.py multi-mode scoring (--mode synthesis/completeness/all).

Uses monkeypatching to avoid live Ollama and Keycloak calls.
"""
import json
from unittest.mock import patch, MagicMock
import pytest

# Import the functions we'll be testing directly
# eval.py must expose _check(), _run_judge_dimension(), and _build_report()
# as module-level functions so they can be unit-tested.
import importlib.util, sys
from pathlib import Path

_EVAL_PATH = Path(__file__).parent / "eval.py"


def _load_eval():
    spec = importlib.util.spec_from_file_location("eval_module", _EVAL_PATH)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


class TestCheckFunction:
    def test_all_keywords_present_passes(self):
        mod = _load_eval()
        ok, missing = mod._check("Java Python SQL expert", ["Java", "Python", "SQL"])
        assert ok is True
        assert missing == []

    def test_missing_keyword_fails(self):
        mod = _load_eval()
        ok, missing = mod._check("Java expert", ["Java", "Python", "SQL"])
        assert ok is False
        assert "Python" in missing
        assert "SQL" in missing

    def test_case_insensitive(self):
        mod = _load_eval()
        ok, _ = mod._check("java python sql", ["Java", "Python", "SQL"])
        assert ok is True


class TestJudgeDimensionScoring:
    def test_synthesis_score_averaged_over_entries(self):
        mod = _load_eval()
        entries = [
            {"id": "S1", "question": "Q?", "eval_dimensions": ["synthesis"],
             "gold_answer": "gold", "must_contain": []},
            {"id": "S2", "question": "Q2?", "eval_dimensions": ["synthesis"],
             "gold_answer": "gold2", "must_contain": []},
        ]
        answers = {"S1": "answer1", "S2": "answer2"}
        fake_judge = MagicMock(return_value={"synthesis": 0.8, "completeness": 0.6, "reasoning": "ok"})

        scores = mod._run_judge_dimension(entries, answers, "synthesis", fake_judge)
        assert abs(scores - 0.8) < 0.01  # average of 0.8 + 0.8

    def test_only_matching_dimension_entries_scored(self):
        mod = _load_eval()
        entries = [
            {"id": "S1", "question": "Q?", "eval_dimensions": ["synthesis"],
             "gold_answer": "gold", "must_contain": []},
            {"id": "Q1", "question": "Q1?", "eval_dimensions": ["factual"],
             "must_contain": ["Java"]},
        ]
        answers = {"S1": "answer1", "Q1": "Java Python"}
        fake_judge = MagicMock(return_value={"synthesis": 1.0, "completeness": 1.0, "reasoning": "ok"})

        scores = mod._run_judge_dimension(entries, answers, "synthesis", fake_judge)
        assert fake_judge.call_count == 1  # Only S1, not Q1
