"""LLM-as-judge module for RAG pipeline evaluation.

Calls Ollama directly — independent of ICM/DMS — to score answers on
synthesis quality and completeness against a gold-standard reference.
"""

import json
import re
from pathlib import Path

import requests

_PROMPT_TEMPLATE = (Path(__file__).parent / "judge_prompt.txt").read_text(encoding="utf-8")
_DEFAULT_MODEL = "gemma3:4b"
_DEFAULT_OLLAMA_URL = "http://localhost:11434"


def judge(
    question: str,
    gold_answer: str,
    model_answer: str,
    model: str = _DEFAULT_MODEL,
    ollama_url: str = _DEFAULT_OLLAMA_URL,
) -> dict:
    """Score a model answer against a gold reference on synthesis and completeness.

    Returns:
        dict with keys: synthesis (float 0-1), completeness (float 0-1), reasoning (str)
    """
    prompt = _PROMPT_TEMPLATE.format(
        question=question,
        gold_answer=gold_answer,
        model_answer=model_answer,
    )

    resp = requests.post(
        f"{ollama_url}/api/generate",
        json={"model": model, "prompt": prompt, "stream": False},
        timeout=60,
    )
    resp.raise_for_status()
    raw = resp.json()["response"].strip()

    return _parse(raw)


def _parse(raw: str) -> dict:
    """Parse JSON from the model response, stripping markdown fences if present."""
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        # Strip ```json ... ``` fences and retry once
        cleaned = re.sub(r"```(?:json)?\s*|\s*```", "", raw).strip()
        return json.loads(cleaned)
