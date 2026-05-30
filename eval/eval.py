"""
eval.py — Factual-extraction evaluation script for the ICM RAG pipeline.

Uploads a CV document, waits for async indexing, then asks ground-truth
questions and checks answers for required keywords (factual) and/or scores
answers using an LLM judge (synthesis, completeness).

Usage:
    python eval/eval.py [--topk N] [--temperature F] [--sleep S]
                        [--icm-url URL] [--keycloak-url URL]
                        [--mode factual|synthesis|completeness|all]
                        [--judge-model MODEL] [--pass-threshold F]
"""

import argparse
import base64
import json
import os
import sys
import time
from pathlib import Path
from typing import Callable, List, Optional, Union

import requests

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

REALM = "intelligent-content-management"
CLIENT_ID = "intelligent-content-management-api"
CLIENT_SECRET = "icm-secret"

WRITE_USER = "user-write"
READ_USER = "user-read"
USER_PASSWORD = "password"

GROUND_TRUTH_PATH = Path(__file__).parent / "ground_truth.json"
CV_PATH = Path(__file__).parent.parent / "docs" / "test-fixtures" / "alex-morgan-cv.txt"


# ---------------------------------------------------------------------------
# Core functions (testable)
# ---------------------------------------------------------------------------


def get_token(
    keycloak_url: str,
    username: str,
    password: str,
) -> str:
    """Obtain a Keycloak access token via resource owner password grant."""
    url = f"{keycloak_url}/realms/{REALM}/protocol/openid-connect/token"
    response = requests.post(
        url,
        data={
            "grant_type": "password",
            "client_id": CLIENT_ID,
            "client_secret": CLIENT_SECRET,
            "username": username,
            "password": password,
        },
    )
    response.raise_for_status()
    return response.json()["access_token"]


def upload_document(
    icm_url: str,
    token: str,
    name: str,
    base64_file: str,
    file_size: str,
    file_type: str = "PDF",
) -> dict:
    """Upload a document to the ICM API."""
    url = f"{icm_url}/idm/api/v1/document"
    response = requests.post(
        url,
        json={
            "name": name,
            "base64File": base64_file,
            "fileSize": file_size,
            "fileType": file_type,
        },
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
        },
    )
    response.raise_for_status()
    return response.json()


def ask_question(
    icm_url: str,
    token: str,
    question: str,
    top_k: int,
    temperature: float,
) -> str:
    """Send a question to the ICM ask endpoint and return the answer string."""
    url = f"{icm_url}/idm/api/v1/document/ask?topK={top_k}&temperature={temperature}"
    response = requests.post(
        url,
        json={"question": question},
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
        },
    )
    response.raise_for_status()
    return response.json()["answer"]


def check_keywords(
    answer: str,
    keywords: List[str],
    return_missing: bool = False,
) -> Union[bool, List[str]]:
    """
    Check whether all keywords are present in the answer (case-insensitive).

    If return_missing=True, returns the list of missing keywords instead of a bool.
    """
    answer_lower = answer.lower()
    missing = [kw for kw in keywords if kw.lower() not in answer_lower]
    if return_missing:
        return missing
    return len(missing) == 0


# ---------------------------------------------------------------------------
# Evaluation runner
# ---------------------------------------------------------------------------


def _load_judge_fn() -> Callable:
    """Lazily import and return the judge function from judge.py."""
    import sys as _sys
    eval_dir = str(Path(__file__).parent)
    if eval_dir not in _sys.path:
        _sys.path.insert(0, eval_dir)
    from judge import judge  # noqa: PLC0415
    return judge


def run_eval(
    icm_url: str,
    keycloak_url: str,
    top_k: int,
    temperature: float,
    sleep_seconds: int,
    mode: str = "factual",
    judge_model: str = "gemma3:4b",
    pass_threshold: float = 0.7,
    judge_fn: Optional[Callable] = None,
) -> int:
    """
    Run the full evaluation pipeline.

    Parameters
    ----------
    mode:           factual | synthesis | completeness | all
    judge_model:    Ollama model to use for LLM judging (synthesis/completeness)
    pass_threshold: Minimum average score for synthesis/completeness dimensions
    judge_fn:       Injectable judge callable (default: loads from judge.py)

    Returns the number of failures (0 = all pass).
    Non-zero if any factual keyword fails OR any LLM dim avg < pass_threshold.
    """
    ground_truth = json.loads(GROUND_TRUTH_PATH.read_text(encoding="utf-8"))

    run_factual = mode in ("factual", "all")
    run_synthesis = mode in ("synthesis", "all")
    run_completeness = mode in ("completeness", "all")

    # Lazily resolve judge only when needed to avoid import errors in factual-only runs
    if (run_synthesis or run_completeness) and judge_fn is None:
        judge_fn = _load_judge_fn()

    # --- Upload step ---
    print("Obtaining WRITE token …")
    write_token = get_token(keycloak_url, username=WRITE_USER, password=USER_PASSWORD)

    cv_bytes = CV_PATH.read_bytes()
    encoded = base64.b64encode(cv_bytes).decode("utf-8")
    file_size = str(len(cv_bytes))

    print(f"Uploading {CV_PATH.name} …")
    upload_document(
        icm_url=icm_url,
        token=write_token,
        name=CV_PATH.name,
        base64_file=encoded,
        file_size=file_size,
        file_type="PDF",
    )
    print(f"Upload complete. Sleeping {sleep_seconds}s for async indexing …")
    time.sleep(sleep_seconds)

    # --- Ask step ---
    print("Obtaining READ token …")
    read_token = get_token(keycloak_url, username=READ_USER, password=USER_PASSWORD)

    # Factual tracking
    factual_passed = 0
    factual_failed = 0
    factual_total = 0

    # LLM-judge tracking
    synthesis_scores: List[float] = []
    completeness_scores: List[float] = []

    print()

    # Collect entries for each active dimension
    factual_entries = []
    synthesis_entries = []
    completeness_entries = []

    for entry in ground_truth:
        dims = entry.get("eval_dimensions", ["factual"])
        if run_factual and "factual" in dims:
            factual_entries.append(entry)
        if run_synthesis and "synthesis" in dims:
            synthesis_entries.append(entry)
        if run_completeness and "completeness" in dims:
            completeness_entries.append(entry)

    # Deduplicate entries for ask (same entry may be asked multiple times for different dims)
    # But each entry needs exactly one /ask call; we may reuse answers across dims
    entries_to_ask: dict = {}
    for entry in factual_entries + synthesis_entries + completeness_entries:
        entries_to_ask[entry["id"]] = entry

    answers: dict = {}
    for entry in entries_to_ask.values():
        answer = ask_question(
            icm_url=icm_url,
            token=read_token,
            question=entry["question"],
            top_k=top_k,
            temperature=temperature,
        )
        answers[entry["id"]] = answer

    # --- Factual evaluation ---
    if run_factual:
        factual_total = len(factual_entries)
        for entry in factual_entries:
            qid = entry["id"]
            question = entry["question"]
            keywords = entry["keywords"]
            answer = answers[qid]
            missing = check_keywords(answer, keywords, return_missing=True)
            if not missing:
                print(f"  PASS  [{qid}] {question}")
                factual_passed += 1
            else:
                print(f"  FAIL  [{qid}] {question}")
                print(f"        Missing keywords: {missing}")
                print(f"        Answer: {answer[:200]}")
                factual_failed += 1

    # --- Synthesis evaluation ---
    if run_synthesis:
        for entry in synthesis_entries:
            qid = entry["id"]
            question = entry["question"]
            gold_answer = entry.get("gold_answer", "")
            answer = answers[qid]
            result = judge_fn(
                question=question,
                gold_answer=gold_answer,
                model_answer=answer,
                model=judge_model,
            )
            score = result.get("synthesis", 0.0)
            synthesis_scores.append(score)
            print(f"  JUDGE [{qid}] synthesis={score:.2f} — {question[:60]}")

    # --- Completeness evaluation ---
    if run_completeness:
        for entry in completeness_entries:
            qid = entry["id"]
            question = entry["question"]
            gold_answer = entry.get("gold_answer", "")
            answer = answers[qid]
            result = judge_fn(
                question=question,
                gold_answer=gold_answer,
                model_answer=answer,
                model=judge_model,
            )
            score = result.get("completeness", 0.0)
            completeness_scores.append(score)
            print(f"  JUDGE [{qid}] completeness={score:.2f} — {question[:60]}")

    # --- Final report ---
    print()
    score_parts: List[str] = []
    total_failures = factual_failed

    if run_factual and factual_total > 0:
        pct = int(factual_passed / factual_total * 100)
        score_parts.append(f"factual {factual_passed}/{factual_total} ({pct}%)")

    if run_synthesis and synthesis_scores:
        avg = sum(synthesis_scores) / len(synthesis_scores)
        score_parts.append(f"synthesis {avg:.2f} avg")
        if avg < pass_threshold:
            total_failures += 1

    if run_completeness and completeness_scores:
        avg = sum(completeness_scores) / len(completeness_scores)
        score_parts.append(f"completeness {avg:.2f} avg")
        if avg < pass_threshold:
            total_failures += 1

    print("Score: " + " | ".join(score_parts))
    return total_failures


# ---------------------------------------------------------------------------
# CLI entry point
# ---------------------------------------------------------------------------


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Evaluate ICM RAG pipeline accuracy (factual, synthesis, completeness)."
    )
    parser.add_argument("--topk", type=int, default=2, help="topK for /ask (default 2)")
    parser.add_argument(
        "--temperature", type=float, default=0.7, help="temperature for /ask (default 0.7)"
    )
    parser.add_argument(
        "--sleep",
        type=int,
        default=10,
        help="seconds to wait after upload for async indexing (default 10)",
    )
    parser.add_argument(
        "--icm-url", default="http://localhost:8085", help="ICM base URL"
    )
    parser.add_argument(
        "--keycloak-url", default="http://localhost:8180", help="Keycloak base URL"
    )
    parser.add_argument(
        "--mode",
        choices=["factual", "synthesis", "completeness", "all"],
        default="factual",
        help="Evaluation mode (default: factual)",
    )
    parser.add_argument(
        "--judge-model",
        default="gemma3:4b",
        help="Ollama model for LLM judge (default: gemma3:4b)",
    )
    parser.add_argument(
        "--pass-threshold",
        type=float,
        default=0.7,
        help="Minimum average score for synthesis/completeness to pass (default: 0.7)",
    )

    args = parser.parse_args()

    failures = run_eval(
        icm_url=args.icm_url,
        keycloak_url=args.keycloak_url,
        top_k=args.topk,
        temperature=args.temperature,
        sleep_seconds=args.sleep,
        mode=args.mode,
        judge_model=args.judge_model,
        pass_threshold=args.pass_threshold,
    )
    sys.exit(0 if failures == 0 else 1)


if __name__ == "__main__":
    main()
