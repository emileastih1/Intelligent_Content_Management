package com.ea.icm.common;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.ea.icm.infrastructure.persistance.document.model.DocumentElasticEntity;
import com.github.tomakehurst.wiremock.WireMockServer;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.awaitility.Awaitility;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.docker.compose.enabled=false",
                "aiServiceClient.type=rest"
        }
)
class DocumentRagIntegrationTest {

    private static final String REALM = "intelligent-content-management";
    private static final String CLIENT_ID = "intelligent-content-management-api";
    private static final String CLIENT_SECRET = "icm-secret";

    // WireMock must start before @DynamicPropertySource runs (which happens before @BeforeAll)
    static final WireMockServer wireMock;

    static {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("doc_management_db")
            .withUsername("postgres")
            .withPassword("toor")
            .withInitScript("db/init/schema.sql");

    @Container
    static ElasticsearchContainer elasticsearch =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:9.0.0")
                    .withEnv("xpack.security.enabled", "false");

    @Container
    static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.0.0")
            .withRealmImportFile("docker/keycloak/realm-export.json")
            .withAdminUsername("admin")
            .withAdminPassword("admin");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.elasticsearch.uris",
                () -> "http://" + elasticsearch.getHost() + ":" + elasticsearch.getMappedPort(9200));
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloak.getAuthServerUrl() + "/realms/" + REALM);
        registry.add("aiServiceClient.url", wireMock::baseUrl);
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @BeforeEach
    void ensureDocumentIndex() {
        var indexOps = elasticsearchOperations.indexOps(DocumentElasticEntity.class);
        if (!indexOps.exists()) {
            indexOps.createWithMapping();
        }
    }

    @BeforeAll
    static void prepareKeycloakUsers() throws Exception {
        String adminTokenUrl = keycloak.getAuthServerUrl()
                + "/realms/master/protocol/openid-connect/token";
        String adminForm = "grant_type=password"
                + "&client_id=admin-cli"
                + "&username=" + keycloak.getAdminUsername()
                + "&password=" + keycloak.getAdminPassword();

        HttpClient http = HttpClient.newHttpClient();

        HttpResponse<String> tokenResp = http.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(adminTokenUrl))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(adminForm))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        String adminToken = extractJsonField(tokenResp.body(), "access_token");

        String usersUrl = keycloak.getAuthServerUrl() + "/admin/realms/" + REALM + "/users";

        HttpResponse<String> usersResp = http.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(usersUrl))
                        .header("Authorization", "Bearer " + adminToken)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        String usersJson = usersResp.body();
        int idx = 0;
        while ((idx = usersJson.indexOf("\"id\":\"", idx)) != -1) {
            int start = idx + 6;
            int end = usersJson.indexOf("\"", start);
            String userId = usersJson.substring(start, end);
            idx = end;

            http.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(usersUrl + "/" + userId))
                            .header("Authorization", "Bearer " + adminToken)
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofString(
                                    "{\"requiredActions\":[],\"emailVerified\":true}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            http.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(usersUrl + "/" + userId + "/reset-password"))
                            .header("Authorization", "Bearer " + adminToken)
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofString(
                                    "{\"type\":\"password\",\"value\":\"password\",\"temporary\":false}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
        }
    }

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @LocalServerPort
    private int port;

    // -------------------------------------------------------------------------
    // Cycle 1: blank question body is rejected (validation)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Blank question body returns 400 Bad Request")
    void blankQuestionReturnsBadRequest() {
        String token = obtainToken("user-read", "password");

        RestClient restClient = RestClient.create();
        ResponseEntity<String> response = restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document/ask")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"question\":\"\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(response.getStatusCode().value())
                .as("Blank question should return 400 Bad Request. Body: " + response.getBody())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    // -------------------------------------------------------------------------
    // Cycle 2: no auth token returns 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("No auth token on POST /v1/document/ask returns 401 Unauthorized")
    void noTokenReturnsUnauthorized() {
        RestClient restClient = RestClient.create();
        ResponseEntity<String> response = restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"question\":\"What is in the document?\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(response.getStatusCode().value())
                .as("Missing Bearer token should return 401 Unauthorized")
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    // -------------------------------------------------------------------------
    // Cycle 3: WRITE-only token is rejected on ask endpoint (403)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("WRITE-only token on POST /v1/document/ask returns 403 Forbidden")
    void writeTokenRejectedOnAskEndpoint() {
        String token = obtainToken("user-write", "password");

        RestClient restClient = RestClient.create();
        ResponseEntity<String> response = restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document/ask")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"question\":\"What is in the document?\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(response.getStatusCode().value())
                .as("WRITE-only token must be denied on ask endpoint with 403")
                .isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    // -------------------------------------------------------------------------
    // Cycle 4: upload → async indexing event fires → ask question → answer returned
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Upload doc then ask question returns 200 with answer (full RAG chain)")
    void uploadDocThenAskQuestionReturnsAnswer() {
        // Stub AI service: document indexing (called async after upload)
        wireMock.stubFor(post(urlEqualTo("/AiServiceClient/v1/document"))
                .willReturn(aResponse().withStatus(200)));

        // Stub AI service: question answering (SSE streaming)
        wireMock.stubFor(post(urlPathEqualTo("/AiServiceClient/v1/document/ask"))
                .withQueryParam("topK", equalTo("2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("data: The candidate has experience\n\ndata: [DONE]\n\n")));

        String writeToken = obtainToken("user-write", "password");
        String readToken = obtainToken("user-read", "password");

        // Step 1: upload a document
        RestClient restClient = RestClient.create();
        ResponseEntity<String> uploadResponse = restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"cv.pdf\",\"base64File\":\"dGVzdA==\",\"fileSize\":\"1 KB\",\"fileType\":\"PDF\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(uploadResponse.getStatusCode().value())
                .as("Document upload should return 200. Body: " + uploadResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());

        // Step 2: wait for the async DocumentSendToVectorStoreEvent to reach WireMock
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        wireMock.verify(postRequestedFor(urlEqualTo("/AiServiceClient/v1/document"))));

        // Step 3: ask a question
        ResponseEntity<String> askResponse = restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document/ask")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + readToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"question\":\"How many years of testing experience does the candidate have?\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(askResponse.getStatusCode().value())
                .as("Ask question should return 200. Body: " + askResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());
        assertThat(askResponse.getHeaders().getContentType().toString())
                .as("Response content type should be text/event-stream")
                .contains("text/event-stream");
        assertThat(askResponse.getBody())
                .as("Response should contain [DONE] sentinel")
                .contains("[DONE]");

        wireMock.verify(postRequestedFor(urlPathEqualTo("/AiServiceClient/v1/document/ask"))
                .withQueryParam("topK", equalTo("2")));
    }

    // -------------------------------------------------------------------------
    // Cycle 5: upload → document is indexed in Elasticsearch
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Upload doc indexes document in Elasticsearch")
    void uploadDocIndexesInElasticsearch() {
        wireMock.stubFor(post(urlEqualTo("/AiServiceClient/v1/document"))
                .willReturn(aResponse().withStatus(200)));

        String writeToken = obtainToken("user-write", "password");

        RestClient restClient = RestClient.create();
        restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"cv.pdf\",\"base64File\":\"dGVzdA==\",\"fileSize\":\"1 KB\",\"fileType\":\"PDF\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    long count = elasticsearchClient.count(c -> c.index("document")).count();
                    assertThat(count)
                            .as("Elasticsearch index 'document' should contain at least one document after upload")
                            .isGreaterThan(0L);
                });
    }

    // -------------------------------------------------------------------------
    // Cycle 6: SSE streaming proxy — multi-token response preserved end-to-end
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Ask question proxies multi-token SSE stream with [DONE] as final event")
    void askQuestionProxiesMultiTokenSseStream() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/AiServiceClient/v1/document/ask"))
                .withQueryParam("topK", equalTo("2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("data: The \n\ndata: candidate \n\ndata: has experience\n\ndata: [DONE]\n\n")));

        String readToken = obtainToken("user-read", "password");

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/idm/api/v1/document/ask?topK=2"))
                .header("Authorization", "Bearer " + readToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"question\":\"What experience does the candidate have?\"}"))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode())
                .as("SSE ask should return 200")
                .isEqualTo(200);

        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .as("Response must be SSE")
                .contains("text/event-stream");

        String body = response.body();
        List<String> dataValues = Arrays.stream(body.split("\n"))
                .filter(line -> line.startsWith("data:"))
                .map(line -> line.substring("data:".length()).strip())
                .collect(Collectors.toList());

        assertThat(dataValues)
                .as("Should receive 4 data events: 3 tokens + [DONE]")
                .hasSizeGreaterThanOrEqualTo(4);

        assertThat(String.join("", dataValues.subList(0, dataValues.size() - 1)))
                .as("Concatenated tokens should contain 'candidate'")
                .contains("candidate");

        assertThat(dataValues.get(dataValues.size() - 1))
                .as("[DONE] must be the final event")
                .isEqualTo("[DONE]");
    }

    // -------------------------------------------------------------------------
    // Cycle 7: content-first authoring — author a Document (content, no file)
    //          then GET list returns it with its content (slice #69)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Author a Document (content, no file) then GET list returns it with its content")
    void authorDocumentThenListReturnsItWithContent() {
        String writeToken = obtainToken("user-write", "password");
        String readToken = obtainToken("user-read", "password");

        RestClient restClient = RestClient.create();

        // Author a content-first Document: content provided directly, no base64File
        ResponseEntity<String> createResponse = restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"My Note\",\"content\":\"hello world\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(createResponse.getStatusCode().value())
                .as("Authoring a content-first document should return 200. Body: " + createResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());

        // The list endpoint returns the authored document, including its content
        ResponseEntity<String> listResponse = restClient.get()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + readToken)
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(listResponse.getStatusCode().value())
                .as("Listing documents should return 200. Body: " + listResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());

        assertThat(listResponse.getBody())
                .as("List should contain the authored document's name and content")
                .contains("My Note")
                .contains("hello world");
    }

    // -------------------------------------------------------------------------
    // Cycle 8: edit Document content — PUT updates content; GET returns updated
    //          content; Elasticsearch is re-indexed (slice #70)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Edit Document content — PUT updates content and GET reflects the change")
    void editDocumentContentThenGetReturnsUpdatedContent() {
        String writeToken = obtainToken("user-write", "password");
        String readToken = obtainToken("user-read", "password");

        wireMock.stubFor(post(urlEqualTo("/AiServiceClient/v1/document"))
                .willReturn(aResponse().withStatus(200)));

        RestClient restClient = RestClient.create();

        // Step 1: create a document
        ResponseEntity<String> createResponse = restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"Editable Note\",\"content\":\"original text\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(createResponse.getStatusCode().value())
                .as("Create should succeed. Body: " + createResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());

        // Extract the document id from the DocumentResult response
        String createBody = createResponse.getBody();
        String docId = extractJsonField(createBody, "id");
        assertThat(docId).as("Created document must have an id").isNotBlank();

        // Step 2: edit the content via PUT
        ResponseEntity<String> updateResponse = restClient.put()
                .uri("http://localhost:" + port + "/idm/api/v1/document/" + docId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"Editable Note\",\"content\":\"updated text\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(updateResponse.getStatusCode().value())
                .as("PUT update should return 200. Body: " + updateResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());

        // Step 3: GET the document by id — content must reflect the update
        ResponseEntity<String> getResponse = restClient.get()
                .uri("http://localhost:" + port + "/idm/api/v1/document/" + docId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + readToken)
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(getResponse.getStatusCode().value())
                .as("GET by id should return 200. Body: " + getResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());

        assertThat(getResponse.getBody())
                .as("GET response should contain the updated content")
                .contains("updated text");
    }

    // -------------------------------------------------------------------------
    // Cycle 9: delete document — DELETE removes it; GET list no longer contains it
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Delete document then GET list no longer contains it")
    void deleteDocumentThenListNoLongerContainsIt() {
        String writeToken = obtainToken("user-write", "password");
        String readToken = obtainToken("user-read", "password");

        RestClient restClient = RestClient.create();

        // Step 1: create a document
        ResponseEntity<String> createResponse = restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"ToDelete Note\",\"content\":\"content to delete\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(createResponse.getStatusCode().value())
                .as("Create should succeed. Body: " + createResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());

        String docId = extractJsonField(createResponse.getBody(), "id");
        assertThat(docId).as("Created document must have an id").isNotBlank();

        // Step 2: DELETE the document
        ResponseEntity<Void> deleteResponse = restClient.delete()
                .uri("http://localhost:" + port + "/idm/api/v1/document/" + docId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toBodilessEntity();

        assertThat(deleteResponse.getStatusCode().value())
                .as("DELETE should return 200")
                .isEqualTo(HttpStatus.OK.value());

        // Step 3: GET list must no longer contain the document name
        ResponseEntity<String> listResponse = restClient.get()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + readToken)
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(listResponse.getStatusCode().value())
                .as("List should return 200. Body: " + listResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());

        assertThat(listResponse.getBody())
                .as("List should NOT contain the deleted document's name")
                .doesNotContain("ToDelete Note");
    }

    // -------------------------------------------------------------------------
    // Cycle 10: tags + category on Document — create with classification,
    //           GET list returns document with matching tags and category
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Create document with tags and category then GET list returns classification")
    void setTagsAndCategoryOnDocumentThenListReturnsClassification() {
        String writeToken = obtainToken("user-write", "password");
        String readToken = obtainToken("user-read", "password");

        RestClient restClient = RestClient.create();

        // Create a document that includes tags and category
        ResponseEntity<String> createResponse = restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"Classified Note\",\"content\":\"some content\","
                        + "\"tags\":[\"java\",\"spring\"],\"category\":\"engineering\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(createResponse.getStatusCode().value())
                .as("Creating document with tags/category should return 200. Body: " + createResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());

        // GET list and verify the document carries its classification
        ResponseEntity<String> listResponse = restClient.get()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + readToken)
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(listResponse.getStatusCode().value())
                .as("Listing documents should return 200. Body: " + listResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());

        assertThat(listResponse.getBody())
                .as("List should contain the document's tag 'java'")
                .contains("java");

        assertThat(listResponse.getBody())
                .as("List should contain the document's category 'engineering'")
                .contains("engineering");
    }

    // -------------------------------------------------------------------------
    // Cycle 11: full-text search — upload doc, wait for ES index, search by name
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Search documents returns matching results by document name")
    void searchDocumentsReturnsMatchingResults() {
        // Stub AI service indexing call (fired async after upload)
        wireMock.stubFor(post(urlEqualTo("/AiServiceClient/v1/document"))
                .willReturn(aResponse().withStatus(200)));

        String writeToken = obtainToken("user-write", "password");
        String readToken = obtainToken("user-read", "password");

        RestClient restClient = RestClient.create();

        // Step 1: upload a document so it gets indexed in ES
        ResponseEntity<String> uploadResponse = restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"SearchableReport\",\"content\":\"quarterly results analysis\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(uploadResponse.getStatusCode().value())
                .as("Document upload should return 200. Body: " + uploadResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());

        // Step 2: wait for Elasticsearch to index the document
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    long count = elasticsearchClient.count(c -> c.index("document")).count();
                    assertThat(count)
                            .as("Elasticsearch must have at least one document before searching")
                            .isGreaterThan(0L);
                });

        // Step 3: search by a word from the document name
        ResponseEntity<String> searchResponse = restClient.get()
                .uri("http://localhost:" + port + "/idm/api/v1/document/search?q=SearchableReport")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + readToken)
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(searchResponse.getStatusCode().value())
                .as("Search should return 200. Body: " + searchResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());

        assertThat(searchResponse.getBody())
                .as("Search response should contain the uploaded document's name")
                .contains("SearchableReport");
    }

    // -------------------------------------------------------------------------
    // Cycle 12: upload ingestion path — file → extracted content stored in TEXT_CONTENT
    //           (ADR-0004 content-first: Tika-extracted text must be in the content field)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Upload file document then GET list returns document with extracted content")
    void uploadFileThenGetDocumentHasExtractedContent() {
        // Stub AI service: vector store indexing call (fired async after upload)
        wireMock.stubFor(post(urlEqualTo("/AiServiceClient/v1/document"))
                .willReturn(aResponse().withStatus(200)));

        String writeToken = obtainToken("user-write", "password");
        String readToken = obtainToken("user-read", "password");

        RestClient restClient = RestClient.create();

        // Upload a document with a base64-encoded file payload ("test content" in plain text)
        // base64("test content") = "dGVzdCBjb250ZW50"
        ResponseEntity<String> uploadResponse = restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"extracted.txt\",\"base64File\":\"dGVzdCBjb250ZW50\",\"fileSize\":\"1 KB\",\"fileType\":\"PDF\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(uploadResponse.getStatusCode().value())
                .as("Upload should return 200. Body: " + uploadResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());

        // Wait for async events to complete (vector store call is the last async step)
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        wireMock.verify(postRequestedFor(urlEqualTo("/AiServiceClient/v1/document"))));

        // GET list and find the uploaded document — its content field must be non-empty
        ResponseEntity<String> listResponse = restClient.get()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + readToken)
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(listResponse.getStatusCode().value())
                .as("List should return 200. Body: " + listResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());

        // The uploaded document must appear in the list with a non-empty content field
        String listBody = listResponse.getBody();
        assertThat(listBody)
                .as("List response should contain the uploaded document name")
                .contains("extracted.txt");

        // Extract the content value for the uploaded document from JSON
        // The list is an array of DocumentDto objects; find the one named "extracted.txt"
        // and assert its "content" field is not null/empty
        assertThat(listBody)
                .as("The uploaded document's content field must be non-empty after Tika extraction "
                        + "(ADR-0004: upload ingestion path must persist extracted text to TEXT_CONTENT). "
                        + "Body: " + listBody)
                .containsPattern("\"content\"\\s*:\\s*\"[^\"]+\"");
    }

    // -------------------------------------------------------------------------
    // Cycle 12: upload ingestion path — file bytes are Tika-extracted into
    //           TEXT_CONTENT so the uploaded document has editable content
    //           (ADR-0004, slice #72)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Upload file then GET list returns document with extracted text content")
    void uploadFileThenListReturnsDocumentWithExtractedContent() {
        wireMock.stubFor(post(urlEqualTo("/AiServiceClient/v1/document"))
                .willReturn(aResponse().withStatus(200)));

        String writeToken = obtainToken("user-write", "password");
        String readToken  = obtainToken("user-read", "password");

        RestClient restClient = RestClient.create();

        // "Hello from upload" as a plain-text file, base64-encoded
        // Base64 of "Hello from upload"
        String base64PlainText = java.util.Base64.getEncoder()
                .encodeToString("Hello from upload".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        ResponseEntity<String> createResponse = restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"upload-test.txt\","
                        + "\"base64File\":\"" + base64PlainText + "\","
                        + "\"fileType\":\"TXT\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(createResponse.getStatusCode().value())
                .as("Upload should return 200. Body: " + createResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());

        String docId = extractJsonField(createResponse.getBody(), "id");

        // Wait briefly for async events then fetch the document by id
        Awaitility.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS)
                .pollDelay(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ResponseEntity<String> getResponse = restClient.get()
                            .uri("http://localhost:" + port + "/idm/api/v1/document/" + docId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + readToken)
                            .retrieve()
                            .onStatus(status -> true, (req2, res2) -> {})
                            .toEntity(String.class);

                    assertThat(getResponse.getStatusCode().value()).isEqualTo(200);
                    assertThat(getResponse.getBody())
                            .as("GET should contain the extracted text from the uploaded file")
                            .contains("Hello from upload");
                });
    }

    // -------------------------------------------------------------------------
    // Cycle 13: batch update — POST /v1/document/batch-update applies tags-to-add
    //           and a category to a set of documentIds (slice #77)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Batch update applies tags and category to all targeted documents")
    void batchUpdateAppliesTagsAndCategoryToTargetedDocuments() {
        String writeToken = obtainToken("user-write", "password");
        String readToken  = obtainToken("user-read", "password");

        RestClient restClient = RestClient.create();

        // Create two documents
        String id1 = extractJsonField(restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"Batch Doc A\",\"content\":\"content a\"}")
                .retrieve().onStatus(s -> true, (q, r) -> {}).toEntity(String.class).getBody(), "id");

        String id2 = extractJsonField(restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"Batch Doc B\",\"content\":\"content b\"}")
                .retrieve().onStatus(s -> true, (q, r) -> {}).toEntity(String.class).getBody(), "id");

        // Batch update: add tag "batch-tag" and set category "ops" on both docs
        ResponseEntity<String> batchResponse = restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document/batch-update")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"documentIds\":[" + id1 + "," + id2 + "],"
                        + "\"updatePayload\":{\"tagsToAdd\":[\"batch-tag\"],\"category\":\"ops\"}}")
                .retrieve()
                .onStatus(s -> true, (q, r) -> {})
                .toEntity(String.class);

        assertThat(batchResponse.getStatusCode().value())
                .as("Batch update should return 200. Body: " + batchResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());

        // Verify both docs now carry the tag and category via the list endpoint
        ResponseEntity<String> listResponse = restClient.get()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + readToken)
                .retrieve().onStatus(s -> true, (q, r) -> {}).toEntity(String.class);

        assertThat(listResponse.getBody())
                .as("List should contain 'batch-tag' from batch update")
                .contains("batch-tag");

        assertThat(listResponse.getBody())
                .as("List should contain category 'ops' from batch update")
                .contains("ops");
    }

    // -------------------------------------------------------------------------
    // Cycle 14: content-first embedding — authored document create fires
    //           embed-content to DMS using TEXT_CONTENT (not file bytes, ADR-0004)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Author a document then DMS embed-content is called with TEXT_CONTENT")
    void authorDocumentFiresEmbedContentToDms() {
        // Stub: DMS embed-content endpoint (new content-first path)
        wireMock.stubFor(post(urlEqualTo("/AiServiceClient/v1/document/embed-content"))
                .willReturn(aResponse().withStatus(200)));

        String writeToken = obtainToken("user-write", "password");
        RestClient restClient = RestClient.create();

        ResponseEntity<String> createResponse = restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"Authored Note\",\"content\":\"content for embedding\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(createResponse.getStatusCode().value())
                .as("Author create should return 200. Body: " + createResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());

        // DMS embed-content must be called asynchronously with the document content
        Awaitility.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS)
                .untilAsserted(() ->
                        wireMock.verify(postRequestedFor(urlEqualTo("/AiServiceClient/v1/document/embed-content"))
                                .withRequestBody(containing("content for embedding"))));
    }

    @Test
    @DisplayName("Upload a file then DMS embed-content is called with extracted TEXT_CONTENT")
    void uploadDocumentFiresEmbedContentWithExtractedText() {
        // Stub: DMS embed-content (new path) — old /v1/document should NOT be called
        wireMock.stubFor(post(urlEqualTo("/AiServiceClient/v1/document/embed-content"))
                .willReturn(aResponse().withStatus(200)));

        String writeToken = obtainToken("user-write", "password");
        RestClient restClient = RestClient.create();

        String base64 = java.util.Base64.getEncoder()
                .encodeToString("extracted upload text".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        ResponseEntity<String> createResponse = restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"upload.txt\",\"base64File\":\"" + base64 + "\",\"fileType\":\"TXT\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(createResponse.getStatusCode().value())
                .as("Upload create should return 200. Body: " + createResponse.getBody())
                .isEqualTo(HttpStatus.OK.value());

        // embed-content must be called with the Tika-extracted text
        Awaitility.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS)
                .untilAsserted(() ->
                        wireMock.verify(postRequestedFor(urlEqualTo("/AiServiceClient/v1/document/embed-content"))
                                .withRequestBody(containing("extracted upload text"))));
    }

    // -------------------------------------------------------------------------
    // Cycle 15: re-embed on edit — editing content fires delete-then-embed-content
    //           so chat reflects the updated text (ADR-0004, ADR-0007, slice #90)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Editing document content fires delete-then-embed-content to DMS")
    void editDocumentContentFiresDeleteThenEmbedContent() {
        // Stub all three DMS calls that may fire
        wireMock.stubFor(post(urlEqualTo("/AiServiceClient/v1/document/embed-content"))
                .willReturn(aResponse().withStatus(200)));
        wireMock.stubFor(delete(urlMatching("/AiServiceClient/v1/document/\\d+"))
                .willReturn(aResponse().withStatus(200)));
        wireMock.stubFor(post(urlEqualTo("/AiServiceClient/v1/document"))
                .willReturn(aResponse().withStatus(200)));

        String writeToken = obtainToken("user-write", "password");
        RestClient restClient = RestClient.create();

        // Create a document first
        ResponseEntity<String> createResponse = restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"Edit Me\",\"content\":\"original content\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(createResponse.getStatusCode().value()).isEqualTo(200);
        String docId = extractJsonField(createResponse.getBody(), "id");

        // Reset WireMock so we only count calls from the edit below
        wireMock.resetRequests();

        // Edit the content
        ResponseEntity<String> updateResponse = restClient.put()
                .uri("http://localhost:" + port + "/idm/api/v1/document/" + docId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + writeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"Edit Me\",\"content\":\"updated content after edit\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(updateResponse.getStatusCode().value())
                .as("PUT update should return 200. Body: " + updateResponse.getBody())
                .isEqualTo(200);

        // DMS delete must fire (purge old chunks)
        Awaitility.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS)
                .untilAsserted(() ->
                        wireMock.verify(deleteRequestedFor(
                                urlMatching("/AiServiceClient/v1/document/\\d+"))));

        // DMS embed-content must fire with the new content
        Awaitility.await().atMost(5, java.util.concurrent.TimeUnit.SECONDS)
                .untilAsserted(() ->
                        wireMock.verify(postRequestedFor(
                                urlEqualTo("/AiServiceClient/v1/document/embed-content"))
                                .withRequestBody(containing("updated content after edit"))));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String obtainToken(String username, String password) {
        String tokenUrl = keycloak.getAuthServerUrl()
                + "/realms/" + REALM + "/protocol/openid-connect/token";

        String formBody = "grant_type=password"
                + "&client_id=" + CLIENT_ID
                + "&client_secret=" + CLIENT_SECRET
                + "&username=" + username
                + "&password=" + password;

        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode())
                    .as("Token endpoint should return 200 for user: " + username
                            + ". Response body: " + response.body())
                    .isEqualTo(200);

            String body = response.body();
            int tokenStart = body.indexOf("\"access_token\":\"") + "\"access_token\":\"".length();
            int tokenEnd = body.indexOf("\"", tokenStart);
            return body.substring(tokenStart, tokenEnd);

        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain token for user: " + username, e);
        }
    }

    private static String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key) + key.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
