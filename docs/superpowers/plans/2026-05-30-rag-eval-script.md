# RAG Pipeline Eval Script Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a standalone Python eval script (`eval/eval.py`) that measures factual-extraction accuracy of the ICM + DMS RAG pipeline by uploading a synthetic CV, waiting for async indexing, asking 8 ground-truth questions, and reporting keyword-match pass/fail with a numeric score. Also wire `topK` and `temperature` as configurable query params through the full ICM → DMS call chain so the script's `--topk` and `--temperature` args have real effect.

**Architecture:** The eval script talks to ICM (port 8085) using Keycloak tokens — a WRITE token to upload the CV, a READ token to ask questions. ICM proxies the ask request to DMS (port 8086), forwarding `topK` and `temperature` as URL query params. DMS uses `topK` in the `SearchRequest` and `temperature` in a per-request `OllamaOptions` override. A fixed sleep (default 10 s, `--sleep` arg) covers the async indexing window.

**Tech Stack:** Python 3.11+, `requests` library, Spring Boot (ICM + DMS), Spring AI (`OllamaOptions`), WireMock 3.9.1 (integration tests), `argparse` (CLI).

---

## Repos and Key Paths

- **ICM:** `C:\Users\eastih\Documents\Home\Dev\workspaces\archetype_ddd\intelligent_content_management`
- **DMS:** `C:\Users\eastih\Documents\Home\Dev\workspaces\ai_chat_dms_workspace\dms`

---

## File Map

### DMS — files to modify

| File | Change |
|------|--------|
| `src/main/java/com/ea/ai/rag/dms/presentation/api/AiServiceClientRestController.java` | Add `topK` (default `2`) and `temperature` (optional) `@RequestParam`s |
| `src/main/java/com/ea/ai/rag/dms/application/service/AiServiceClient.java` | Add `topK`, `temperature` to `askQuestion` signature |
| `src/main/java/com/ea/ai/rag/dms/application/service/impl/AiServiceClientImpl.java` | Pass new params through |
| `src/main/java/com/ea/ai/rag/dms/infrastructure/repository/DocumentAiRepository.java` | Use `topK` in `SearchRequest`; use `temperature` in `OllamaOptions` |
| `src/test/java/com/ea/ai/rag/dms/infrastructure/repository/DocumentAiRepositoryTest.java` | **NEW** — unit test verifying `topK` propagation |

### ICM — files to modify

| File | Change |
|------|--------|
| `src/main/java/com/ea/icm/presentation/external/api/query/AiServiceClientQueryRestController.java` | Add `topK` and `temperature` `@RequestParam`s |
| `src/main/java/com/ea/icm/application/external/port/query/AiServiceClientQuery.java` | Update interface signature |
| `src/main/java/com/ea/icm/application/external/port/query/impl/AiServiceClientQueryImpl.java` | Pass params |
| `src/main/java/com/ea/icm/domain/externe/repository/query/AiServiceClientQueryPort.java` | Update port interface |
| `src/main/java/com/ea/icm/infrastructure/repository/external/clients/DocumentAiRestClient.java` | Add query params to DMS call |
| `src/test/java/com/ea/icm/common/DocumentRagIntegrationTest.java` | Update Cycle 4 WireMock stub + verify to match query params |

### Eval — new files (in ICM repo root)

| File | Purpose |
|------|---------|
| `eval/ground_truth.json` | Machine-readable Q&A ground truth (8 pairs) |
| `eval/requirements.txt` | Python deps (`requests`) |
| `eval/eval.py` | Main eval script |

---

## Task 1: DMS — failing unit test for topK propagation

**Repo:** DMS

**Files:**
- Create: `src/test/java/com/ea/ai/rag/dms/infrastructure/repository/DocumentAiRepositoryTest.java`

- [ ] **Step 1.1: Write the failing test**

```java
package com.ea.ai.rag.dms.infrastructure.repository;

import com.ea.ai.rag.dms.domain.vo.ai.Answer;
import com.ea.ai.rag.dms.domain.vo.ai.Question;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentAiRepositoryTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final ChatModel chatModel = mock(ChatModel.class);
    private final PromptTemplate promptTemplate = new PromptTemplate(
            new ClassPathResource("ai/promptTemplate/rag-document-prompt-template.st"));

    private final DocumentAiRepository repository =
            new DocumentAiRepository(vectorStore, promptTemplate, chatModel);

    @Test
    void askQuestion_uses_topK_from_parameter() {
        // arrange
        var doc = new org.springframework.ai.document.Document("Alex Morgan has 6 years of Java experience.");
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        var generation = mock(Generation.class);
        var output = mock(org.springframework.ai.chat.messages.AssistantMessage.class);
        when(output.getText()).thenReturn("6 years");
        when(generation.getOutput()).thenReturn(output);
        var chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        // act
        repository.askQuestion(new Question("How many years of Java?"), 5, null);

        // assert — SearchRequest must have topK == 5
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().getTopK()).isEqualTo(5);
    }
}
```

- [ ] **Step 1.2: Run the test to confirm it fails**

```
cd C:\Users\eastih\Documents\Home\Dev\workspaces\ai_chat_dms_workspace\dms
mvn test -pl . -Dtest=DocumentAiRepositoryTest -q
```

Expected: **FAIL** — method `askQuestion(Question, int, Double)` does not exist yet.

---

## Task 2: DMS — implement topK and temperature params

**Repo:** DMS

**Files:**
- Modify: `src/main/java/com/ea/ai/rag/dms/infrastructure/repository/DocumentAiRepository.java`
- Modify: `src/main/java/com/ea/ai/rag/dms/application/service/AiServiceClient.java`
- Modify: `src/main/java/com/ea/ai/rag/dms/application/service/impl/AiServiceClientImpl.java`
- Modify: `src/main/java/com/ea/ai/rag/dms/presentation/api/AiServiceClientRestController.java`

- [ ] **Step 2.1: Update DocumentAiRepository**

Replace the `askQuestion` method:

```java
// Add imports at the top of the file:
import org.springframework.ai.ollama.api.OllamaOptions;

// Replace the existing askQuestion method:
public Answer askQuestion(Question question, int topK, Double temperature) {
    List<org.springframework.ai.document.Document> similarDocuments = vectorStore.similaritySearch(
            SearchRequest.builder().query(question.question()).topK(topK).build());

    if (!similarDocuments.isEmpty()) {
        List<String> contentList = similarDocuments.stream()
                .map(org.springframework.ai.document.Document::getText)
                .toList();
        Map<String, Object> promptParameters = new HashMap<>();
        promptParameters.put("input", question.question());
        promptParameters.put("documents", String.join("\n", contentList));
        Prompt prompt = promptTemplate.create(promptParameters);

        ChatResponse response;
        if (temperature != null) {
            OllamaOptions options = OllamaOptions.builder().temperature(temperature).build();
            response = chatModel.call(new Prompt(prompt.getInstructions(), options));
        } else {
            response = chatModel.call(prompt);
        }
        return new Answer(response.getResult().getOutput().getText());
    } else {
        return new Answer("Sorry, I don't have an answer for that question");
    }
}
```

- [ ] **Step 2.2: Update AiServiceClient interface**

```java
// Replace:
Answer askQuestion(Question question);
// With:
Answer askQuestion(Question question, int topK, Double temperature);
```

- [ ] **Step 2.3: Update AiServiceClientImpl**

```java
@Override
public Answer askQuestion(Question question, int topK, Double temperature) {
    return documentAiClientRepository.askQuestion(question, topK, temperature);
}
```

Note: `documentAiClientRepository` is of type `DocumentAiClientRepository` (the domain port). Update that interface's `askQuestion` signature the same way — find the interface with `grep -r "DocumentAiClientRepository" src/main/java` and apply the same change.

- [ ] **Step 2.4: Update AiServiceClientRestController**

```java
@PostMapping(value = "/v1/document/ask", produces = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<Answer> askQuestion(
        @RequestBody @Valid Question question,
        @RequestParam(defaultValue = "2") int topK,
        @RequestParam(required = false) Double temperature) {
    return new ResponseEntity<>(aiServiceClient.askQuestion(question, topK, temperature), HttpStatus.OK);
}
```

- [ ] **Step 2.5: Compile DMS**

```
mvn compile -q
```

Expected: **BUILD SUCCESS**. Fix any remaining compilation errors by following the same signature pattern.

- [ ] **Step 2.6: Run the unit test**

```
mvn test -Dtest=DocumentAiRepositoryTest -q
```

Expected: **PASS**.

- [ ] **Step 2.7: Commit DMS changes**

```bash
git add src/
git commit -m "feat: make topK and temperature configurable on ask endpoint"
```

---

## Task 3: ICM — propagate topK and temperature to DMS

**Repo:** ICM

**Files:**
- Modify: `src/main/java/com/ea/icm/domain/externe/repository/query/AiServiceClientQueryPort.java`
- Modify: `src/main/java/com/ea/icm/application/external/port/query/AiServiceClientQuery.java`
- Modify: `src/main/java/com/ea/icm/application/external/port/query/impl/AiServiceClientQueryImpl.java`
- Modify: `src/main/java/com/ea/icm/presentation/external/api/query/AiServiceClientQueryRestController.java`
- Modify: `src/main/java/com/ea/icm/infrastructure/repository/external/clients/DocumentAiRestClient.java`

- [ ] **Step 3.1: Update AiServiceClientQueryPort (domain port)**

```java
// Replace:
Answer askQuestion(Question question);
// With:
Answer askQuestion(Question question, int topK, Double temperature);
```

- [ ] **Step 3.2: Update AiServiceClientQuery (application port)**

```java
// Replace:
Answer askQuestion(Question question);
// With:
Answer askQuestion(Question question, int topK, Double temperature);
```

- [ ] **Step 3.3: Update AiServiceClientQueryImpl**

```java
@Override
public Answer askQuestion(Question question, int topK, Double temperature) {
    return aiServiceClientQueryPort.askQuestion(question, topK, temperature);
}
```

- [ ] **Step 3.4: Update AiServiceClientQueryRestController**

```java
@PreAuthorize("hasRole('READ')")
@PostMapping(value = "/v1/document/ask", produces = "application/json")
public ResponseEntity<Answer> askQuestion(
        @RequestBody @Valid Question question,
        @RequestParam(defaultValue = "2") int topK,
        @RequestParam(required = false) Double temperature) {
    return new ResponseEntity<Answer>(
            aiServiceClientQuery.askQuestion(question, topK, temperature), HttpStatus.OK);
}
```

- [ ] **Step 3.5: Update DocumentAiRestClient**

```java
public Answer askQuestion(Question question, int topK, Double temperature) {
    LOGGER.info("DocumentAiRestClient.askQuestion question: {}, topK: {}, temperature: {}", question, topK, temperature);
    return restClient.post()
            .uri(uriBuilder -> uriBuilder
                    .path("/v1/document/ask")
                    .queryParam("topK", topK)
                    .queryParam("temperature", temperature)
                    .build())
            .contentType(MediaType.APPLICATION_JSON)
            .body(question)
            .retrieve()
            .toEntity(new ParameterizedTypeReference<Answer>() {})
            .getBody();
}
```

- [ ] **Step 3.6: Compile ICM**

```
cd C:\Users\eastih\Documents\Home\Dev\workspaces\archetype_ddd\intelligent_content_management
mvn compile -q
```

Expected: **BUILD SUCCESS**. Fix any remaining compilation errors by following the same pattern.

---

## Task 4: ICM — update DocumentRagIntegrationTest Cycle 4

**Repo:** ICM

**File:**
- Modify: `src/test/java/com/ea/icm/common/DocumentRagIntegrationTest.java`

The existing Cycle 4 stub and verify use `urlEqualTo("/AiServiceClient/v1/document/ask")` which does exact URL matching and will not match once query params are appended. Switch to `urlPathEqualTo` + `withQueryParam`.

- [ ] **Step 4.1: Update Cycle 4 WireMock stub**

Find the ask stub (currently `post(urlEqualTo("/AiServiceClient/v1/document/ask"))`). Replace it:

```java
wireMock.stubFor(post(urlPathEqualTo("/AiServiceClient/v1/document/ask"))
        .withQueryParam("topK", equalTo("2"))
        .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"answer\":\"The candidate has 3 years...\"}")));
```

Add the `equalTo` import if not present:
```java
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
```

- [ ] **Step 4.2: Update Cycle 4 assertion to pass topK param**

Find the ask call in the test (the `mockMvc.perform(post("/v1/document/ask")...)` block). Add the query param:

```java
mockMvc.perform(post("/v1/document/ask")
        .param("topK", "2")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"question\":\"Does Alex have experience?\"}")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + readToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.answer").exists());
```

- [ ] **Step 4.3: Update WireMock verify to match query param**

```java
wireMock.verify(postRequestedFor(urlPathEqualTo("/AiServiceClient/v1/document/ask"))
        .withQueryParam("topK", equalTo("2")));
```

- [ ] **Step 4.4: Run the integration tests**

```
mvn test -Dtest=DocumentRagIntegrationTest,KeycloakSecurityIntegrationTest -q
```

Expected: **All tests PASS**.

- [ ] **Step 4.5: Commit ICM changes**

```bash
git add src/
git commit -m "feat: propagate topK and temperature to DMS ask endpoint"
```

---

## Task 5: Create eval/ground_truth.json

**Repo:** ICM

**File:**
- Create: `eval/ground_truth.json`

- [ ] **Step 5.1: Write the file**

```json
[
  {
    "id": "Q1",
    "label": "Programming languages",
    "question": "What programming languages does Alex Morgan know?",
    "must_contain": ["Java", "Python", "SQL"]
  },
  {
    "id": "Q2",
    "label": "Years of Java experience",
    "question": "How many years of Java experience does Alex Morgan have?",
    "must_contain": ["6"]
  },
  {
    "id": "Q3",
    "label": "Most recent job title",
    "question": "What is Alex Morgan's most recent job title?",
    "must_contain": ["Senior Software Engineer"]
  },
  {
    "id": "Q4",
    "label": "DataSoft employment dates",
    "question": "When did Alex Morgan work at DataSoft Solutions?",
    "must_contain": ["2018", "2021", "DataSoft"]
  },
  {
    "id": "Q5",
    "label": "Education",
    "question": "What degree does Alex Morgan hold and from which university?",
    "must_contain": ["MSc", "Edinburgh", "BSc", "Glasgow"]
  },
  {
    "id": "Q6",
    "label": "Total professional experience",
    "question": "How many years of professional experience does Alex Morgan have?",
    "must_contain": ["6"]
  },
  {
    "id": "Q7",
    "label": "AWS certifications",
    "question": "What AWS certifications does Alex Morgan hold?",
    "must_contain": ["AWS", "Solutions Architect", "2022"]
  },
  {
    "id": "Q8",
    "label": "All employers",
    "question": "List all companies Alex Morgan has worked for.",
    "must_contain": ["TechCorp", "DataSoft"]
  }
]
```

- [ ] **Step 5.2: Commit**

```bash
git add eval/ground_truth.json
git commit -m "feat: add machine-readable RAG eval ground truth"
```

---

## Task 6: Create eval/requirements.txt

**Repo:** ICM

**File:**
- Create: `eval/requirements.txt`

- [ ] **Step 6.1: Write the file**

```
requests==2.32.3
```

- [ ] **Step 6.2: Create and activate a virtual environment, install deps**

```bash
cd eval
python -m venv .venv
# Windows:
.venv\Scripts\activate
pip install -r requirements.txt
```

Expected: `requests` installs without errors.

- [ ] **Step 6.3: Commit**

```bash
git add eval/requirements.txt
git commit -m "feat: add eval script Python requirements"
```

---

## Task 7: Create eval/eval.py

**Repo:** ICM

**File:**
- Create: `eval/eval.py`

- [ ] **Step 7.1: Write the script**

```python
#!/usr/bin/env python3
"""RAG pipeline factual-extraction evaluator for ICM + DMS.

Uploads docs/test-fixtures/alex-morgan-cv.txt to ICM, waits for async indexing,
then asks each question in ground_truth.json and checks the answer for required keywords.

Usage:
    python eval.py
    python eval.py --topk 4 --temperature 0.3 --sleep 15
"""

import argparse
import base64
import json
import sys
import time
from pathlib import Path

import requests

# ---------------------------------------------------------------------------
# Config — matches ICM docker-compose and Keycloak realm-export.json
# ---------------------------------------------------------------------------
KEYCLOAK_TOKEN_URL = (
    "http://localhost:8180/realms/intelligent-content-management"
    "/protocol/openid-connect/token"
)
ICM_BASE_URL = "http://localhost:8085/idm/api"
CLIENT_ID = "intelligent-content-management-api"
CLIENT_SECRET = "icm-secret"

WRITE_USER = ("user-write", "password")
READ_USER = ("user-read", "password")

_HERE = Path(__file__).parent
GROUND_TRUTH_PATH = _HERE / "ground_truth.json"
CV_PATH = _HERE.parent / "docs" / "test-fixtures" / "alex-morgan-cv.txt"


# ---------------------------------------------------------------------------
# Keycloak
# ---------------------------------------------------------------------------
def _get_token(username: str, password: str) -> str:
    resp = requests.post(
        KEYCLOAK_TOKEN_URL,
        data={
            "grant_type": "password",
            "client_id": CLIENT_ID,
            "client_secret": CLIENT_SECRET,
            "username": username,
            "password": password,
            "scope": "openid",
        },
        timeout=10,
    )
    resp.raise_for_status()
    return resp.json()["access_token"]


# ---------------------------------------------------------------------------
# ICM calls
# ---------------------------------------------------------------------------
def _upload_cv(token: str, cv_path: Path) -> None:
    content = cv_path.read_bytes()
    payload = {
        "name": cv_path.name,
        "base64File": base64.b64encode(content).decode(),
        "fileSize": f"{len(content)} B",
        "fileType": "TXT",
    }
    resp = requests.post(
        f"{ICM_BASE_URL}/v1/document",
        json=payload,
        headers={"Authorization": f"Bearer {token}"},
        timeout=30,
    )
    resp.raise_for_status()


def _ask(token: str, question: str, topk: int, temperature: float) -> str:
    resp = requests.post(
        f"{ICM_BASE_URL}/v1/document/ask",
        params={"topK": topk, "temperature": temperature},
        json={"question": question},
        headers={"Authorization": f"Bearer {token}"},
        timeout=60,
    )
    resp.raise_for_status()
    return resp.json().get("answer", "")


# ---------------------------------------------------------------------------
# Evaluation
# ---------------------------------------------------------------------------
def _check(answer: str, must_contain: list[str]) -> tuple[bool, list[str]]:
    """Case-insensitive keyword presence check."""
    answer_lower = answer.lower()
    missing = [kw for kw in must_contain if kw.lower() not in answer_lower]
    return len(missing) == 0, missing


def _print_result(entry: dict, ok: bool, missing: list[str], answer: str) -> None:
    status = "PASS" if ok else "FAIL"
    label = entry.get("label", entry["id"])
    print(f"  [{status}] {entry['id']} — {label}")
    if not ok:
        print(f"         Missing : {missing}")
        print(f"         Answer  : {answer[:300]}")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main() -> None:
    parser = argparse.ArgumentParser(description="ICM RAG pipeline factual-extraction evaluator")
    parser.add_argument("--topk", type=int, default=2,
                        help="topK for vector similarity search (default: 2)")
    parser.add_argument("--temperature", type=float, default=0.7,
                        help="LLM temperature 0.0–1.0 (default: 0.7)")
    parser.add_argument("--sleep", type=int, default=10,
                        help="Seconds to wait after upload for async indexing (default: 10)")
    args = parser.parse_args()

    ground_truth: list[dict] = json.loads(GROUND_TRUTH_PATH.read_text(encoding="utf-8"))

    print("=" * 60)
    print("ICM RAG Eval")
    print(f"  topK={args.topk}  temperature={args.temperature}  sleep={args.sleep}s")
    print("=" * 60)

    # 1. Tokens
    print("\n[1/4] Fetching Keycloak tokens...")
    write_token = _get_token(*WRITE_USER)
    read_token = _get_token(*READ_USER)
    print("      OK")

    # 2. Upload
    print(f"\n[2/4] Uploading {CV_PATH.name} ...")
    _upload_cv(write_token, CV_PATH)
    print("      OK")

    # 3. Wait for indexing
    print(f"\n[3/4] Waiting {args.sleep}s for async indexing...")
    time.sleep(args.sleep)
    print("      OK")

    # 4. Evaluate
    print("\n[4/4] Evaluating questions:")
    passed = 0
    failed = 0

    for entry in ground_truth:
        answer = _ask(read_token, entry["question"], args.topk, args.temperature)
        ok, missing = _check(answer, entry["must_contain"])
        _print_result(entry, ok, missing, answer)
        if ok:
            passed += 1
        else:
            failed += 1

    total = passed + failed
    score = passed / total * 100 if total else 0

    print()
    print("=" * 60)
    print(f"Score: {passed}/{total}  ({score:.0f}%)")
    print("=" * 60)

    sys.exit(0 if failed == 0 else 1)


if __name__ == "__main__":
    main()
```

- [ ] **Step 7.2: Commit**

```bash
git add eval/eval.py
git commit -m "feat: add RAG pipeline factual-extraction eval script"
```

---

## Task 8: End-to-end smoke run

Prerequisites: ICM running on port 8085, DMS running on port 8086, Keycloak on port 8180, Postgres on port 5434, Ollama on port 11434 with `gemma3:4b` and `mxbai-embed-large` pulled.

Start ICM services:
```bash
docker compose -f src/main/resources/docker/docker-compose.yml up -d
```

- [ ] **Step 8.1: Run with defaults**

```bash
cd eval
.venv\Scripts\activate  # Windows
python eval.py
```

Expected output structure:
```
============================================================
ICM RAG Eval
  topK=2  temperature=0.7  sleep=10s
============================================================

[1/4] Fetching Keycloak tokens...
      OK
[2/4] Uploading alex-morgan-cv.txt ...
      OK
[3/4] Waiting 10s for async indexing...
      OK
[4/4] Evaluating questions:
  [PASS] Q1 — Programming languages
  [PASS] Q2 — Years of Java experience
  ...
============================================================
Score: 7/8  (88%)
============================================================
```

- [ ] **Step 8.2: Try topK=4 to see if score improves**

```bash
python eval.py --topk 4
```

Compare score. If Q7 (cross-section certifications) was failing at topK=2, it often passes at topK=4.

- [ ] **Step 8.3: Record baseline score in a comment on GitHub issue #55**

Post a comment at `https://github.com/emileastih1/Intelligent_Content_Management/issues/55` with:
- topK value used
- Score (X/8)
- Which questions failed (if any)
- A note on whether increasing topK helped

---

## Self-Review

**Spec coverage check:**

| Requirement | Task |
|-------------|------|
| `--topk` CLI arg has real effect on search | Tasks 2, 3 |
| `--temperature` CLI arg has real effect on LLM | Task 2 |
| `--sleep` controls indexing wait | Task 7 |
| Upload CV via ICM WRITE token | Task 7 |
| Ask 8 ground-truth questions via READ token | Tasks 5, 7 |
| Keyword-match pass/fail per question | Task 7 |
| Numeric score printed | Task 7 |
| Integration test covers topK forwarding | Task 4 |
| ADR for streaming captured | ✅ Already written (`docs/adr/0003-sse-streaming-for-ask-endpoint.md`) |

All requirements covered. No placeholders.
