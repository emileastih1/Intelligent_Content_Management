package com.ea.icm.common;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
class KeycloakSecurityIntegrationTest {

    private static final String REALM = "intelligent-content-management";
    private static final String CLIENT_ID = "intelligent-content-management-api";
    private static final String CLIENT_SECRET = "icm-secret";

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
    }

    @LocalServerPort
    private int port;

    /**
     * After the Keycloak container starts, use the admin REST API to clear any
     * lingering required actions (e.g. VERIFY_PROFILE added by Keycloak 26 by
     * default) so that password-grant logins succeed.
     */
    /**
     * Keycloak 26 enforces user profile validation (VERIFY_PROFILE) at login time via realm-level policy,
     * even when requiredActions is empty on the user. To ensure clean password-grant logins, we:
     * 1. Obtain an admin token.
     * 2. Clear any pending required actions on each imported user via the Admin REST API.
     * 3. Explicitly reset each user's password (Keycloak 26 may not honour plain-text credential imports).
     */
    @BeforeAll
    static void prepareKeycloakUsers() throws Exception {
        String adminTokenUrl = keycloak.getAuthServerUrl()
                + "/realms/master/protocol/openid-connect/token";
        String adminForm = "grant_type=password"
                + "&client_id=admin-cli"
                + "&username=" + keycloak.getAdminUsername()
                + "&password=" + keycloak.getAdminPassword();

        HttpClient http = HttpClient.newHttpClient();

        // 1. Obtain admin access token from the master realm
        HttpResponse<String> tokenResp = http.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(adminTokenUrl))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(adminForm))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        String adminToken = extractJsonField(tokenResp.body(), "access_token");

        // 2. List users in the ICM realm
        String usersUrl = keycloak.getAuthServerUrl()
                + "/admin/realms/" + REALM + "/users";

        HttpResponse<String> usersResp = http.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(usersUrl))
                        .header("Authorization", "Bearer " + adminToken)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        // Parse user IDs from the JSON array (minimal string parsing — no extra JSON library)
        String usersJson = usersResp.body();
        int idx = 0;
        while ((idx = usersJson.indexOf("\"id\":\"", idx)) != -1) {
            int start = idx + 6;
            int end = usersJson.indexOf("\"", start);
            String userId = usersJson.substring(start, end);
            idx = end;

            // Clear required actions and confirm email verification
            http.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(usersUrl + "/" + userId))
                            .header("Authorization", "Bearer " + adminToken)
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofString(
                                    "{\"requiredActions\":[],\"emailVerified\":true}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            // Explicitly set password (Keycloak 26 does not honour plain-text credential values in imports)
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

    private static String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key) + key.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    // -------------------------------------------------------------------------
    // Test 1: context loads and the JWKS endpoint is reachable
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Context loads and Keycloak JWKS endpoint returns 200")
    void contextLoadsAndJwksIsReachable() throws Exception {
        String jwksUrl = keycloak.getAuthServerUrl()
                + "/realms/" + REALM + "/protocol/openid-connect/certs";

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jwksUrl))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode())
                .as("JWKS endpoint should return 200")
                .isEqualTo(200);
        assertThat(response.body())
                .as("JWKS response should contain 'keys'")
                .contains("keys");
    }

    // -------------------------------------------------------------------------
    // Test 2: user-read token is accepted on the read endpoint (not 401/403)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Valid READ token accepted on GET /api/v1/document/{id} (not 401 or 403)")
    void readTokenAcceptedOnReadEndpoint() {
        String token = obtainToken("user-read", "password");

        RestClient restClient = RestClient.create();
        ResponseEntity<String> response = restClient.get()
                .uri("http://localhost:" + port + "/idm/api/v1/document/00000000-0000-0000-0000-000000000001")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(status -> true, (req, res) -> { /* accept all status codes */ })
                .toEntity(String.class);

        int statusCode = response.getStatusCode().value();
        assertThat(statusCode)
                .as("READ token should not be rejected by security (expect 4xx other than 401/403, e.g. 404)")
                .isNotEqualTo(HttpStatus.UNAUTHORIZED.value())
                .isNotEqualTo(HttpStatus.FORBIDDEN.value());
    }

    // -------------------------------------------------------------------------
    // Test 3: user-read token is rejected on the write endpoint (403)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("READ-only token rejected on POST /api/v1/document with 403 Forbidden")
    void readTokenRejectedOnWriteEndpoint() {
        String token = obtainToken("user-read", "password");

        RestClient restClient = RestClient.create();
        ResponseEntity<String> response = restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"TestDoc\",\"base64File\":\"dGVzdA==\",\"fileSize\":\"1 MB\",\"fileType\":\"PDF\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> { /* accept all status codes */ })
                .toEntity(String.class);

        assertThat(response.getStatusCode().value())
                .as("READ-only token must be denied on write endpoint with 403")
                .isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    // -------------------------------------------------------------------------
    // Test 4: user-write token can successfully add a document (hits real DB)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Valid WRITE token on POST /api/v1/document persists document and returns 200")
    void writeTokenCanAddDocument() {
        String token = obtainToken("user-write", "password");

        RestClient restClient = RestClient.create();
        ResponseEntity<String> response = restClient.post()
                .uri("http://localhost:" + port + "/idm/api/v1/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\":\"test-doc.pdf\",\"base64File\":\"dGVzdA==\",\"fileSize\":\"1 KB\",\"fileType\":\"PDF\"}")
                .retrieve()
                .onStatus(status -> true, (req, res) -> { /* accept all status codes */ })
                .toEntity(String.class);

        assertThat(response.getStatusCode().value())
                .as("WRITE token should successfully persist a document and return 200. Body: " + response.getBody())
                .isEqualTo(HttpStatus.OK.value());
    }

    // -------------------------------------------------------------------------
    // Helper: obtain a JWT access token from Keycloak using password grant
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

            // Extract access_token via simple string parsing (no extra JSON lib dependency)
            String body = response.body();
            int tokenStart = body.indexOf("\"access_token\":\"") + "\"access_token\":\"".length();
            int tokenEnd = body.indexOf("\"", tokenStart);
            return body.substring(tokenStart, tokenEnd);

        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain token for user: " + username, e);
        }
    }
}
