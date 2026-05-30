import json
import re
from pathlib import Path

import requests


def judge(
    question: str,
    gold_answer: str,
    model_answer: str,
    model: str = "gemma3:4b",
    ollama_url: str = "http://localhost:11434",
) -> dict:
    """
    Evaluate a model answer against a gold answer using an LLM judge.

    Returns:
        dict with keys: synthesis (float), completeness (float), reasoning (str)
    """
    prompt_path = Path(__file__).parent / "judge_prompt.txt"
    prompt_template = prompt_path.read_text(encoding="utf-8")
    filled_prompt = (
        prompt_template
        .replace("{question}", question)
        .replace("{gold_answer}", gold_answer)
        .replace("{model_answer}", model_answer)
    )

    response = requests.post(
        f"{ollama_url}/api/generate",
        json={"model": model, "prompt": filled_prompt, "stream": False},
    )
    raw = response.json()["response"].strip()

    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        # Strip markdown fences and retry once
        cleaned = re.sub(r"^```(?:json)?\s*", "", raw)
        cleaned = re.sub(r"\s*```$", "", cleaned).strip()
        return json.loads(cleaned)
