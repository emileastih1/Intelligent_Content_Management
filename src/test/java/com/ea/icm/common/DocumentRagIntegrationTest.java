package com.ea.icm.common;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
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
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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

        // Stub AI service: question answering
        wireMock.stubFor(post(urlPathEqualTo("/AiServiceClient/v1/document/ask"))
                .withQueryParam("topK", equalTo("2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"answer\":\"The candidate has 3 years of manual and automation testing experience.\"}")));

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
        assertThat(askResponse.getBody())
                .as("Response should contain an 'answer' field")
                .contains("answer");

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
