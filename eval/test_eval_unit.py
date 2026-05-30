"""Unit tests for eval.py — run with pytest before implementing eval.py (RED phase)."""

import pytest
from unittest.mock import patch, MagicMock


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

KEYCLOAK_URL = "http://localhost:8180"
ICM_URL = "http://localhost:8085"
REALM = "intelligent-content-management"
CLIENT_ID = "intelligent-content-management-api"
CLIENT_SECRET = "icm-secret"


# ---------------------------------------------------------------------------
# get_token
# ---------------------------------------------------------------------------

class TestGetToken:
    def test_get_token_calls_correct_keycloak_url(self):
        """get_token() calls the correct Keycloak token endpoint."""
        from eval import get_token

        with patch("eval.requests.post") as mock_post:
            mock_post.return_value.json.return_value = {"access_token": "tok123"}
            mock_post.return_value.raise_for_status = MagicMock()

            get_token(
                keycloak_url=KEYCLOAK_URL,
                username="user-write",
                password="password",
            )

        expected_url = (
            f"{KEYCLOAK_URL}/realms/{REALM}/protocol/openid-connect/token"
        )
        mock_post.assert_called_once()
        actual_url = mock_post.call_args[0][0]
        assert actual_url == expected_url

    def test_get_token_sends_correct_form_body(self):
        """get_token() sends the correct form fields (grant_type, client_id, username, password)."""
        from eval import get_token

        with patch("eval.requests.post") as mock_post:
            mock_post.return_value.json.return_value = {"access_token": "tok123"}
            mock_post.return_value.raise_for_status = MagicMock()

            get_token(
                keycloak_url=KEYCLOAK_URL,
                username="user-write",
                password="password",
            )

        call_kwargs = mock_post.call_args[1]
        data = call_kwargs.get("data", {})
        assert data["grant_type"] == "password"
        assert data["client_id"] == CLIENT_ID
        assert data["client_secret"] == CLIENT_SECRET
        assert data["username"] == "user-write"
        assert data["password"] == "password"

    def test_get_token_returns_access_token_string(self):
        """get_token() returns the access_token string from the response."""
        from eval import get_token

        with patch("eval.requests.post") as mock_post:
            mock_post.return_value.json.return_value = {"access_token": "my-token-abc"}
            mock_post.return_value.raise_for_status = MagicMock()

            token = get_token(
                keycloak_url=KEYCLOAK_URL,
                username="user-read",
                password="password",
            )

        assert token == "my-token-abc"


# ---------------------------------------------------------------------------
# upload_document
# ---------------------------------------------------------------------------

class TestUploadDocument:
    def test_upload_document_sends_post_with_bearer_token(self):
        """upload_document() sends POST /idm/api/v1/document with Authorization: Bearer header."""
        from eval import upload_document

        with patch("eval.requests.post") as mock_post:
            mock_post.return_value.raise_for_status = MagicMock()
            mock_post.return_value.json.return_value = {"id": "doc-1"}

            upload_document(
                icm_url=ICM_URL,
                token="bearer-xyz",
                name="alex-morgan-cv.txt",
                base64_file="AAAA",
                file_size="100",
                file_type="PDF",
            )

        mock_post.assert_called_once()
        call_kwargs = mock_post.call_args[1]
        headers = call_kwargs.get("headers", {})
        assert headers.get("Authorization") == "Bearer bearer-xyz"

    def test_upload_document_sends_correct_json_body(self):
        """upload_document() sends the correct JSON body fields."""
        from eval import upload_document

        with patch("eval.requests.post") as mock_post:
            mock_post.return_value.raise_for_status = MagicMock()
            mock_post.return_value.json.return_value = {"id": "doc-1"}

            upload_document(
                icm_url=ICM_URL,
                token="bearer-xyz",
                name="test.txt",
                base64_file="BASE64DATA",
                file_size="512",
                file_type="PDF",
            )

        call_kwargs = mock_post.call_args[1]
        json_body = call_kwargs.get("json", {})
        assert json_body["name"] == "test.txt"
        assert json_body["base64File"] == "BASE64DATA"
        assert json_body["fileSize"] == "512"
        assert json_body["fileType"] == "PDF"

    def test_upload_document_hits_correct_endpoint(self):
        """upload_document() posts to /idm/api/v1/document."""
        from eval import upload_document

        with patch("eval.requests.post") as mock_post:
            mock_post.return_value.raise_for_status = MagicMock()
            mock_post.return_value.json.return_value = {}

            upload_document(
                icm_url=ICM_URL,
                token="tok",
                name="f.txt",
                base64_file="x",
                file_size="1",
                file_type="PDF",
            )

        actual_url = mock_post.call_args[0][0]
        assert actual_url == f"{ICM_URL}/idm/api/v1/document"


# ---------------------------------------------------------------------------
# ask_question
# ---------------------------------------------------------------------------

class TestAskQuestion:
    def test_ask_question_includes_topk_in_query_string(self):
        """ask_question() appends ?topK=<n> to the URL when called with topK=5."""
        from eval import ask_question

        with patch("eval.requests.post") as mock_post:
            mock_post.return_value.raise_for_status = MagicMock()
            mock_post.return_value.json.return_value = {"answer": "42"}

            ask_question(
                icm_url=ICM_URL,
                token="tok",
                question="What is the answer?",
                top_k=5,
                temperature=0.7,
            )

        actual_url = mock_post.call_args[0][0]
        assert "topK=5" in actual_url

    def test_ask_question_includes_temperature_in_query_string(self):
        """ask_question() includes temperature in the query string."""
        from eval import ask_question

        with patch("eval.requests.post") as mock_post:
            mock_post.return_value.raise_for_status = MagicMock()
            mock_post.return_value.json.return_value = {"answer": "42"}

            ask_question(
                icm_url=ICM_URL,
                token="tok",
                question="What?",
                top_k=2,
                temperature=0.3,
            )

        actual_url = mock_post.call_args[0][0]
        assert "temperature=0.3" in actual_url

    def test_ask_question_sends_bearer_auth_header(self):
        """ask_question() sends Authorization: Bearer header."""
        from eval import ask_question

        with patch("eval.requests.post") as mock_post:
            mock_post.return_value.raise_for_status = MagicMock()
            mock_post.return_value.json.return_value = {"answer": "x"}

            ask_question(
                icm_url=ICM_URL,
                token="read-token",
                question="Q",
                top_k=2,
                temperature=0.7,
            )

        headers = mock_post.call_args[1].get("headers", {})
        assert headers.get("Authorization") == "Bearer read-token"

    def test_ask_question_returns_answer_string(self):
        """ask_question() returns the answer string from the response."""
        from eval import ask_question

        with patch("eval.requests.post") as mock_post:
            mock_post.return_value.raise_for_status = MagicMock()
            mock_post.return_value.json.return_value = {"answer": "The answer is Java."}

            result = ask_question(
                icm_url=ICM_URL,
                token="tok",
                question="Q",
                top_k=2,
                temperature=0.7,
            )

        assert result == "The answer is Java."

    def test_ask_question_sends_question_in_json_body(self):
        """ask_question() sends the question text in the JSON body."""
        from eval import ask_question

        with patch("eval.requests.post") as mock_post:
            mock_post.return_value.raise_for_status = MagicMock()
            mock_post.return_value.json.return_value = {"answer": "x"}

            ask_question(
                icm_url=ICM_URL,
                token="tok",
                question="How many years of Java?",
                top_k=2,
                temperature=0.7,
            )

        json_body = mock_post.call_args[1].get("json", {})
        assert json_body.get("question") == "How many years of Java?"


# ---------------------------------------------------------------------------
# check_keywords
# ---------------------------------------------------------------------------

class TestCheckKeywords:
    def test_returns_true_when_all_keywords_present_exact_case(self):
        """check_keywords() returns True when all keywords are present (exact case)."""
        from eval import check_keywords

        answer = "Alex Morgan knows Java, Python, and SQL."
        keywords = ["Java", "Python", "SQL"]
        assert check_keywords(answer, keywords) is True

    def test_returns_true_when_keywords_present_different_case(self):
        """check_keywords() is case-insensitive — returns True regardless of case."""
        from eval import check_keywords

        answer = "alex morgan knows JAVA, python, and sql."
        keywords = ["Java", "Python", "SQL"]
        assert check_keywords(answer, keywords) is True

    def test_returns_false_when_one_keyword_missing(self):
        """check_keywords() returns False when at least one keyword is missing."""
        from eval import check_keywords

        answer = "Alex Morgan knows Java and SQL."
        keywords = ["Java", "Python", "SQL"]
        assert check_keywords(answer, keywords) is False

    def test_returns_false_when_all_keywords_missing(self):
        """check_keywords() returns False when all keywords are missing."""
        from eval import check_keywords

        answer = "I don't know."
        keywords = ["Java", "Python", "SQL"]
        assert check_keywords(answer, keywords) is False

    def test_returns_true_for_empty_keywords_list(self):
        """check_keywords() returns True when no keywords are required."""
        from eval import check_keywords

        assert check_keywords("anything", []) is True

    def test_multi_word_keyword_detection(self):
        """check_keywords() detects multi-word keywords like 'Senior Software Engineer'."""
        from eval import check_keywords

        answer = "The candidate holds the title of Senior Software Engineer at TechCorp."
        keywords = ["Senior Software Engineer"]
        assert check_keywords(answer, keywords) is True

    def test_returns_correct_missing_keywords(self):
        """check_keywords() returns the list of missing keywords when called with return_missing=True."""
        from eval import check_keywords

        answer = "Alex knows Java only."
        missing = check_keywords(answer, ["Java", "Python", "SQL"], return_missing=True)
        assert set(missing) == {"Python", "SQL"}


# ---------------------------------------------------------------------------
# run_eval mode tests (new — issue #62)
# ---------------------------------------------------------------------------


class TestRunEvalModes:
    """Tests for --mode factual/synthesis/completeness/all behavior."""

    def _make_ask_mock(self, answer="Alex knows Java, Python, SQL."):
        """Return a mock requests.post that simulates /ask and /token responses."""
        mock = MagicMock()
        mock.return_value.raise_for_status = MagicMock()
        mock.return_value.json.side_effect = [
            # get_token (write)
            {"access_token": "write-tok"},
            # upload_document
            {"id": "doc-1"},
            # get_token (read)
            {"access_token": "read-tok"},
        ] + [{"answer": answer}] * 20  # up to 20 ask calls
        return mock

    def test_mode_factual_is_default(self):
        """run_eval() in factual mode (default) never calls the judge function."""
        from eval import run_eval

        judge_mock = MagicMock()

        with patch("eval.requests.post", self._make_ask_mock()), \
             patch("eval.time.sleep"), \
             patch("eval.CV_PATH") as mock_cv:
            mock_cv.read_bytes.return_value = b"fake-cv"
            mock_cv.name = "cv.txt"

            run_eval(
                icm_url=ICM_URL,
                keycloak_url=KEYCLOAK_URL,
                top_k=2,
                temperature=0.7,
                sleep_seconds=0,
                mode="factual",
                judge_fn=judge_mock,
            )

        judge_mock.assert_not_called()

    def test_mode_synthesis_calls_judge_for_synthesis_entries(self):
        """run_eval(mode='synthesis') calls judge() exactly 4 times for S1–S4 entries."""
        from eval import run_eval

        judge_mock = MagicMock(return_value={"synthesis": 0.8, "completeness": 0.8, "reasoning": "ok"})

        with patch("eval.requests.post", self._make_ask_mock()), \
             patch("eval.time.sleep"), \
             patch("eval.CV_PATH") as mock_cv:
            mock_cv.read_bytes.return_value = b"fake-cv"
            mock_cv.name = "cv.txt"

            run_eval(
                icm_url=ICM_URL,
                keycloak_url=KEYCLOAK_URL,
                top_k=2,
                temperature=0.7,
                sleep_seconds=0,
                mode="synthesis",
                judge_fn=judge_mock,
            )

        assert judge_mock.call_count == 4

    def test_mode_completeness_calls_judge_for_completeness_entries(self):
        """run_eval(mode='completeness') calls judge() exactly 4 times for C1–C4 entries."""
        from eval import run_eval

        judge_mock = MagicMock(return_value={"synthesis": 0.8, "completeness": 0.8, "reasoning": "ok"})

        with patch("eval.requests.post", self._make_ask_mock()), \
             patch("eval.time.sleep"), \
             patch("eval.CV_PATH") as mock_cv:
            mock_cv.read_bytes.return_value = b"fake-cv"
            mock_cv.name = "cv.txt"

            run_eval(
                icm_url=ICM_URL,
                keycloak_url=KEYCLOAK_URL,
                top_k=2,
                temperature=0.7,
                sleep_seconds=0,
                mode="completeness",
                judge_fn=judge_mock,
            )

        assert judge_mock.call_count == 4

    def test_pass_threshold_causes_nonzero_exit_when_below(self):
        """run_eval returns non-zero failures indicator when synthesis avg < pass_threshold."""
        from eval import run_eval

        # Judge returns synthesis score of 0.5
        judge_mock = MagicMock(return_value={"synthesis": 0.5, "completeness": 0.5, "reasoning": "poor"})

        with patch("eval.requests.post", self._make_ask_mock()), \
             patch("eval.time.sleep"), \
             patch("eval.CV_PATH") as mock_cv:
            mock_cv.read_bytes.return_value = b"fake-cv"
            mock_cv.name = "cv.txt"

            result = run_eval(
                icm_url=ICM_URL,
                keycloak_url=KEYCLOAK_URL,
                top_k=2,
                temperature=0.7,
                sleep_seconds=0,
                mode="synthesis",
                judge_fn=judge_mock,
                pass_threshold=0.7,
            )

        # failures > 0 means exit code should be non-zero
        assert result > 0

    def test_mode_all_runs_all_three_dimensions(self):
        """run_eval(mode='all') runs factual keyword checks AND calls judge 8 times (4 synthesis + 4 completeness)."""
        from eval import run_eval

        judge_mock = MagicMock(return_value={"synthesis": 0.8, "completeness": 0.8, "reasoning": "ok"})

        with patch("eval.requests.post", self._make_ask_mock("Java Python SQL 6 Senior Software Engineer TechCorp DataSoft 2018 2021 MSc Edinburgh BSc Glasgow AWS Solutions Architect 2022 CKA Kubernetes 2023")), \
             patch("eval.time.sleep"), \
             patch("eval.CV_PATH") as mock_cv:
            mock_cv.read_bytes.return_value = b"fake-cv"
            mock_cv.name = "cv.txt"

            run_eval(
                icm_url=ICM_URL,
                keycloak_url=KEYCLOAK_URL,
                top_k=2,
                temperature=0.7,
                sleep_seconds=0,
                mode="all",
                judge_fn=judge_mock,
            )

        assert judge_mock.call_count == 8
