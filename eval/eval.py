"""
eval.py — Factual-extraction evaluation script for the ICM RAG pipeline.

Uploads a CV document, waits for async indexing, then asks 8 ground-truth
questions and checks each answer for required keywords.

Usage:
    python eval/eval.py [--topk N] [--temperature F] [--sleep S]
                        [--icm-url URL] [--keycloak-url URL]
"""

import argparse
import base64
import json
import os
import sys
import time
from pathlib import Path
from typing import List, Union

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


def run_eval(
    icm_url: str,
    keycloak_url: str,
    top_k: int,
    temperature: float,
    sleep_seconds: int,
) -> int:
    """
    Run the full evaluation pipeline.

    Returns the number of failed questions (0 = all pass).
    """
    ground_truth = json.loads(GROUND_TRUTH_PATH.read_text(encoding="utf-8"))

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

    passed = 0
    failed = 0
    total = len(ground_truth)

    print()
    for entry in ground_truth:
        qid = entry["id"]
        question = entry["question"]
        keywords = entry["keywords"]

        answer = ask_question(
            icm_url=icm_url,
            token=read_token,
            question=question,
            top_k=top_k,
            temperature=temperature,
        )

        missing = check_keywords(answer, keywords, return_missing=True)
        if not missing:
            print(f"  PASS  [{qid}] {question}")
            passed += 1
        else:
            print(f"  FAIL  [{qid}] {question}")
            print(f"        Missing keywords: {missing}")
            print(f"        Answer: {answer[:200]}")
            failed += 1

    print()
    score_pct = int(passed / total * 100)
    print(f"Score: {passed}/{total} ({score_pct}%)")
    return failed


# ---------------------------------------------------------------------------
# CLI entry point
# ---------------------------------------------------------------------------


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Evaluate ICM RAG pipeline factual-extraction accuracy."
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

    args = parser.parse_args()

    failures = run_eval(
        icm_url=args.icm_url,
        keycloak_url=args.keycloak_url,
        top_k=args.topk,
        temperature=args.temperature,
        sleep_seconds=args.sleep,
    )
    sys.exit(0 if failures == 0 else 1)


if __name__ == "__main__":
    main()
