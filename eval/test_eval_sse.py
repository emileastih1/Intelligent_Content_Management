"""
Tests for the SSE-aware ask_question() function.

Uses requests-mock to simulate the ICM SSE endpoint.
"""
import pytest
import requests_mock as requests_mock_lib

from eval import ask_question


def _sse_body(*tokens, terminal="[DONE]"):
    """Build a mock SSE response body from tokens."""
    lines = [f"data: {t}\n\n" for t in tokens]
    lines.append(f"data: {terminal}\n\n")
    return "".join(lines)


def test_ask_question_accumulates_tokens(requests_mock):
    body = _sse_body("The ", "candidate ", "has experience")
    requests_mock.post(
        "http://icm/idm/api/v1/document/ask?topK=2&temperature=0.7",
        text=body,
        headers={"Content-Type": "text/event-stream"},
    )
    result = ask_question(
        icm_url="http://icm",
        token="tok",
        question="What experience?",
        top_k=2,
        temperature=0.7,
    )
    assert result == "The candidate has experience"


def test_ask_question_done_not_included(requests_mock):
    body = _sse_body("answer")
    requests_mock.post(
        "http://icm/idm/api/v1/document/ask?topK=2&temperature=0.7",
        text=body,
        headers={"Content-Type": "text/event-stream"},
    )
    result = ask_question("http://icm", "tok", "q?", 2, 0.7)
    assert "[DONE]" not in result


def test_ask_question_stream_error_raises(requests_mock):
    body = "data: partial\n\ndata: [STREAM_ERROR]\n\n"
    requests_mock.post(
        "http://icm/idm/api/v1/document/ask?topK=2&temperature=0.7",
        text=body,
        headers={"Content-Type": "text/event-stream"},
    )
    with pytest.raises(RuntimeError, match="STREAM_ERROR"):
        ask_question("http://icm", "tok", "q?", 2, 0.7)
