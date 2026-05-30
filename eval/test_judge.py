import json
import unittest
from unittest.mock import patch, MagicMock


# Test 1: happy path — Ollama returns clean JSON
def test_judge_returns_synthesis_completeness_reasoning():
    mock_response = {"response": '{"synthesis": 0.8, "completeness": 0.9, "reasoning": "Good answer"}'}
    with patch("requests.post") as mock_post:
        mock_post.return_value.json.return_value = mock_response
        from judge import judge
        result = judge("Q?", "gold", "model answer")
    assert "synthesis" in result
    assert "completeness" in result
    assert "reasoning" in result
    assert 0.0 <= result["synthesis"] <= 1.0
    assert 0.0 <= result["completeness"] <= 1.0


# Test 2: scores are floats in [0.0, 1.0]
def test_judge_scores_are_floats_in_range():
    mock_response = {"response": '{"synthesis": 0.5, "completeness": 0.5, "reasoning": "Partial"}'}
    with patch("requests.post") as mock_post:
        mock_post.return_value.json.return_value = mock_response
        from judge import judge
        result = judge("Q?", "gold", "model answer")
    assert isinstance(result["synthesis"], float)
    assert isinstance(result["completeness"], float)


# Test 3: markdown fence retry — Ollama wraps JSON in ```json ... ```
def test_judge_handles_markdown_fenced_json():
    fenced = '```json\n{"synthesis": 0.7, "completeness": 0.6, "reasoning": "Fenced"}\n```'
    mock_response = {"response": fenced}
    with patch("requests.post") as mock_post:
        mock_post.return_value.json.return_value = mock_response
        from judge import judge
        result = judge("Q?", "gold", "model answer")
    assert result["synthesis"] == 0.7
    assert result["completeness"] == 0.6
