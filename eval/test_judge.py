import json
from unittest.mock import patch, MagicMock
import pytest
from judge import judge


SAMPLE_QUESTION = "What programming languages does Alex Morgan know?"
SAMPLE_GOLD = "Alex Morgan knows Java (6 years), Python (2 years), and SQL (6 years)."
SAMPLE_MODEL_ANSWER = "Alex Morgan knows Java and Python."


def _mock_ollama_response(text: str):
    """Return a mock requests.Response that simulates Ollama /api/generate."""
    mock_resp = MagicMock()
    mock_resp.raise_for_status = MagicMock()
    mock_resp.json.return_value = {"response": text}
    return mock_resp


class TestJudgeReturnsValidStructure:
    def test_returns_dict_with_required_keys(self):
        payload = json.dumps({"synthesis": 0.8, "completeness": 0.6, "reasoning": "ok"})
        with patch("requests.post", return_value=_mock_ollama_response(payload)):
            result = judge(SAMPLE_QUESTION, SAMPLE_GOLD, SAMPLE_MODEL_ANSWER)
        assert set(result.keys()) >= {"synthesis", "completeness", "reasoning"}

    def test_synthesis_score_is_float_in_range(self):
        payload = json.dumps({"synthesis": 0.8, "completeness": 0.6, "reasoning": "ok"})
        with patch("requests.post", return_value=_mock_ollama_response(payload)):
            result = judge(SAMPLE_QUESTION, SAMPLE_GOLD, SAMPLE_MODEL_ANSWER)
        assert 0.0 <= result["synthesis"] <= 1.0

    def test_completeness_score_is_float_in_range(self):
        payload = json.dumps({"synthesis": 0.8, "completeness": 0.6, "reasoning": "ok"})
        with patch("requests.post", return_value=_mock_ollama_response(payload)):
            result = judge(SAMPLE_QUESTION, SAMPLE_GOLD, SAMPLE_MODEL_ANSWER)
        assert 0.0 <= result["completeness"] <= 1.0

    def test_reasoning_is_nonempty_string(self):
        payload = json.dumps({"synthesis": 0.8, "completeness": 0.6, "reasoning": "partial answer"})
        with patch("requests.post", return_value=_mock_ollama_response(payload)):
            result = judge(SAMPLE_QUESTION, SAMPLE_GOLD, SAMPLE_MODEL_ANSWER)
        assert isinstance(result["reasoning"], str) and result["reasoning"]


class TestMarkdownFenceRetry:
    def test_parses_json_wrapped_in_markdown_fences(self):
        payload = json.dumps({"synthesis": 0.5, "completeness": 0.7, "reasoning": "fenced"})
        fenced = f"```json\n{payload}\n```"
        with patch("requests.post", return_value=_mock_ollama_response(fenced)):
            result = judge(SAMPLE_QUESTION, SAMPLE_GOLD, SAMPLE_MODEL_ANSWER)
        assert result["synthesis"] == 0.5
        assert result["completeness"] == 0.7
