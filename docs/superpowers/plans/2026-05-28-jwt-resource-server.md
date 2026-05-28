# JWT Resource Server Implementation Plan (Issue #45)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `@Profile("secured")` Basic Auth security placeholder with a stateless JWT Resource Server that reads Keycloak client roles from the `resource_access` JWT claim.

**Architecture:** A new `JwtSecurityConfig` (no `@Profile`) wires Spring Security's OAuth2 resource server with a custom `KeycloakJwtGrantedAuthoritiesConverter`. `RestSecurityConfiguration` constants are updated so that the three controllers' `@SecurityRequirement` annotations reference `bearerAuth` with simplified `READ`/`WRITE` scopes. The old `BasicAuthSecurity` class and its tests are deleted.

**Tech Stack:** Java 21, Spring Boot 4.0.5, `spring-boot-starter-oauth2-resource-server` (already in pom.xml), JUnit 5, Spring Security Test

---

## File Map

| Action | Path | Responsibility |
|--------|------|----------------|
| DELETE | `src/main/java/com/ea/icm/presentation/config/security/BasicAuthSecurity.java` | Remove Basic Auth placeholder |
| DELETE | `src/test/java/com/ea/icm/presentation/config/security/BasicAuthSecurityTest.java` | Remove tests for deleted class |
| MODIFY | `src/main/java/com/ea/icm/application/config/security/RestSecurityConfiguration.java` | Replace `BASIC_AUTH`/`PERMISSION_PREFIX` with `BEARER_AUTH`, simplified `PERM_READ`/`PERM_WRITE` |
| CREATE | `src/main/java/com/ea/icm/presentation/config/security/KeycloakJwtGrantedAuthoritiesConverter.java` | Pure converter: reads `resource_access.<client-id>.roles` from JWT |
| CREATE | `src/main/java/com/ea/icm/presentation/config/security/JwtSecurityConfig.java` | `@Configuration @EnableWebSecurity` — stateless JWT filter chain |
| CREATE | `src/test/java/com/ea/icm/presentation/config/security/KeycloakJwtGrantedAuthoritiesConverterTest.java` | 4 unit tests for the converter |
| MODIFY | `src/main/java/com/ea/icm/presentation/user/api/UserQueryRestController.java` | Update `@SecurityRequirement` to use `BEARER_AUTH` + `PERM_READ` |
| MODIFY | `src/main/java/com/ea/icm/presentation/document/api/command/DocumentCommandRestController.java` | Update `@SecurityRequirement` to use `BEARER_AUTH` + `PERM_WRITE` |
| MODIFY | `src/main/java/com/ea/icm/presentation/document/api/query/DocumentQueryRestController.java` | Update `@SecurityRequirement` to use `BEARER_AUTH` + `PERM_READ` |

---

## Task 1: Update `RestSecurityConfiguration` constants

**Files:**
- Modify: `src/main/java/com/ea/icm/application/config/security/RestSecurityConfiguration.java`

- [ ] **Step 1.1: Replace the constant body**

Replace the entire file content with:

```java
package com.ea.icm.application.config.security;

public class RestSecurityConfiguration {

    public static final String URL_BASE_PATH = "/api";

    public static final String BEARER_AUTH = "bearerAuth";

    public static final String PERM_READ = "READ";
    public static final String PERM_WRITE = "WRITE";
}
```

- [ ] **Step 1.2: Verify the file compiles (quick sanity check)**

Run:
```powershell
.\mvnw.cmd compile -q 2>&1 | Select-String "ERROR"
```

Expected: compilation errors about `BASIC_AUTH` and `PERMISSION_PREFIX` in three controllers — that's expected; we fix them in Task 5.

---

## Task 2: Create `KeycloakJwtGrantedAuthoritiesConverter`

**Files:**
- Create: `src/main/java/com/ea/icm/presentation/config/security/KeycloakJwtGrantedAuthoritiesConverter.java`

- [ ] **Step 2.1: Create the converter file**

```java
package com.ea.icm.presentation.config.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeycloakJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String CLIENT_ID = "intelligent-content-management-api";
    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS);
        if (resourceAccess == null || !resourceAccess.containsKey(CLIENT_ID)) {
            return Collections.emptyList();
        }
        Object clientAccess = resourceAccess.get(CLIENT_ID);
        if (!(clientAccess instanceof Map)) {
            return Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> clientMap = (Map<String, Object>) clientAccess;
        Object rolesObj = clientMap.get(ROLES);
        if (!(rolesObj instanceof List)) {
            return Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) rolesObj;
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .collect(Collectors.toList());
    }
}
```

---

## Task 3: Create `KeycloakJwtGrantedAuthoritiesConverterTest`

**Files:**
- Create: `src/test/java/com/ea/icm/presentation/config/security/KeycloakJwtGrantedAuthoritiesConverterTest.java`
- Test: same file

- [ ] **Step 3.1: Write the failing tests first**

```java
package com.ea.icm.presentation.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtGrantedAuthoritiesConverterTest {

    private final KeycloakJwtGrantedAuthoritiesConverter converter =
            new KeycloakJwtGrantedAuthoritiesConverter();

    private Jwt buildJwt(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "RS256");
        claims.forEach(builder::claim);
        return builder.build();
    }

    @Test
    void givenJwtWithReadAndWriteRoles_whenConvert_thenReturnsBothAuthorities() {
        Map<String, Object> clientRoles = Map.of("roles", List.of("READ", "WRITE"));
        Map<String, Object> resourceAccess = Map.of("intelligent-content-management-api", clientRoles);
        Jwt jwt = buildJwt(Map.of("resource_access", resourceAccess));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_READ", "ROLE_WRITE");
    }

    @Test
    void givenJwtWithMissingResourceAccessClaim_whenConvert_thenReturnsEmpty() {
        Jwt jwt = buildJwt(Map.of("sub", "user123"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void givenJwtWithWrongClientId_whenConvert_thenReturnsEmpty() {
        Map<String, Object> clientRoles = Map.of("roles", List.of("READ"));
        Map<String, Object> resourceAccess = Map.of("other-client", clientRoles);
        Jwt jwt = buildJwt(Map.of("resource_access", resourceAccess));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void givenJwtWithEmptyRolesArray_whenConvert_thenReturnsEmpty() {
        Map<String, Object> clientRoles = Map.of("roles", List.of());
        Map<String, Object> resourceAccess = Map.of("intelligent-content-management-api", clientRoles);
        Jwt jwt = buildJwt(Map.of("resource_access", resourceAccess));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }
}
```

- [ ] **Step 3.2: Run just these tests to verify they fail (converter class not yet wired — but the class exists from Task 2, so they should actually pass)**

Run:
```powershell
.\mvnw.cmd test -pl . -Dtest=KeycloakJwtGrantedAuthoritiesConverterTest -Pwindows-docker-desktop 2>&1 | Select-String "Tests run|BUILD|ERROR"
```

Expected: `Tests run: 4, Failures: 0, Errors: 0` — the converter logic is pure Java, no Spring context needed.

---

## Task 4: Create `JwtSecurityConfig`

**Files:**
- Create: `src/main/java/com/ea/icm/presentation/config/security/JwtSecurityConfig.java`

- [ ] **Step 4.1: Create the JWT security config**

```java
package com.ea.icm.presentation.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class JwtSecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/rest-api-docs/**",
            "/actuator/health"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtGrantedAuthoritiesConverter());
        return converter;
    }
}
```

---

## Task 5: Update controllers to use `BEARER_AUTH`

Three controllers reference `RestSecurityConfiguration.BASIC_AUTH` in their `@SecurityRequirement` annotations. Update each one.

**Files:**
- Modify: `src/main/java/com/ea/icm/presentation/user/api/UserQueryRestController.java`
- Modify: `src/main/java/com/ea/icm/presentation/document/api/command/DocumentCommandRestController.java`
- Modify: `src/main/java/com/ea/icm/presentation/document/api/query/DocumentQueryRestController.java`

- [ ] **Step 5.1: Update `UserQueryRestController` — change the `@SecurityRequirement` at line 43**

Replace:
```java
            security = {@SecurityRequirement(name = RestSecurityConfiguration.BASIC_AUTH, scopes = {RestSecurityConfiguration.PERM_READ})},
```
With:
```java
            security = {@SecurityRequirement(name = RestSecurityConfiguration.BEARER_AUTH, scopes = {RestSecurityConfiguration.PERM_READ})},
```

- [ ] **Step 5.2: Update `DocumentCommandRestController` — change the `@SecurityRequirement` at line 41**

Replace:
```java
            security = {@SecurityRequirement(name = RestSecurityConfiguration.BASIC_AUTH, scopes = {RestSecurityConfiguration.PERM_WRITE})},
```
With:
```java
            security = {@SecurityRequirement(name = RestSecurityConfiguration.BEARER_AUTH, scopes = {RestSecurityConfiguration.PERM_WRITE})},
```

- [ ] **Step 5.3: Update `DocumentQueryRestController` — change the `@SecurityRequirement` at line 38**

Replace:
```java
            security = {@SecurityRequirement(name = RestSecurityConfiguration.BASIC_AUTH, scopes = {RestSecurityConfiguration.PERM_READ})},
```
With:
```java
            security = {@SecurityRequirement(name = RestSecurityConfiguration.BEARER_AUTH, scopes = {RestSecurityConfiguration.PERM_READ})},
```

- [ ] **Step 5.4: Verify compilation is clean**

Run:
```powershell
.\mvnw.cmd compile -q 2>&1 | Select-String "ERROR"
```

Expected: no output (zero errors).

---

## Task 6: Delete old Basic Auth files

**Files:**
- Delete: `src/main/java/com/ea/icm/presentation/config/security/BasicAuthSecurity.java`
- Delete: `src/test/java/com/ea/icm/presentation/config/security/BasicAuthSecurityTest.java`

- [ ] **Step 6.1: Delete both files**

```powershell
Remove-Item "src\main\java\com\ea\icm\presentation\config\security\BasicAuthSecurity.java"
Remove-Item "src\test\java\com\ea\icm\presentation\config\security\BasicAuthSecurityTest.java"
```

- [ ] **Step 6.2: Verify nothing references `BasicAuthSecurity` any more**

Run:
```powershell
.\mvnw.cmd compile -q 2>&1 | Select-String "ERROR"
```

Expected: no output.

---

## Task 7: Run all tests and commit

- [ ] **Step 7.1: Run full test suite**

```powershell
.\mvnw.cmd test -Pwindows-docker-desktop 2>&1 | Select-String "Tests run|BUILD|FAILURE|ERROR" | Select-Object -Last 20
```

Expected: `BUILD SUCCESS` with zero failures. The converter unit tests (`KeycloakJwtGrantedAuthoritiesConverterTest`) should show `Tests run: 4`.

- [ ] **Step 7.2: Stage and commit**

```powershell
git add src/main/java/com/ea/icm/application/config/security/RestSecurityConfiguration.java
git add src/main/java/com/ea/icm/presentation/config/security/KeycloakJwtGrantedAuthoritiesConverter.java
git add src/main/java/com/ea/icm/presentation/config/security/JwtSecurityConfig.java
git add src/test/java/com/ea/icm/presentation/config/security/KeycloakJwtGrantedAuthoritiesConverterTest.java
git add src/main/java/com/ea/icm/presentation/user/api/UserQueryRestController.java
git add src/main/java/com/ea/icm/presentation/document/api/command/DocumentCommandRestController.java
git add src/main/java/com/ea/icm/presentation/document/api/query/DocumentQueryRestController.java
git rm src/main/java/com/ea/icm/presentation/config/security/BasicAuthSecurity.java
git rm src/test/java/com/ea/icm/presentation/config/security/BasicAuthSecurityTest.java
```

```powershell
git commit -m "feat: replace BasicAuthSecurity with JWT Resource Server config and converter (#45)"
```

- [ ] **Step 7.3: Confirm commit hash**

```powershell
git log --oneline -3
```

Expected: top commit is the `feat: replace BasicAuthSecurity...` commit on branch `feat/keycloak-oauth2-security`.

---

## Self-Review

**Spec coverage:**
- DELETE `BasicAuthSecurity.java` — Task 6
- DELETE `BasicAuthSecurityTest.java` — Task 6
- UPDATE `RestSecurityConfiguration.java` (`BEARER_AUTH`, drop `PERMISSION_PREFIX`, simplified perms) — Task 1
- CREATE `KeycloakJwtGrantedAuthoritiesConverter.java` — Task 2
- CREATE `JwtSecurityConfig.java` — Task 4
- CREATE `KeycloakJwtGrantedAuthoritiesConverterTest.java` (4 cases) — Task 3
- Controllers updated to reference `BEARER_AUTH` — Task 5
- Tests pass — Task 7

**Placeholder scan:** No TBD/TODO/similar-to-task references present.

**Type consistency:** `KeycloakJwtGrantedAuthoritiesConverter` is created in Task 2 and referenced in both `JwtSecurityConfig` (Task 4) and the test (Task 3) — consistent. `RestSecurityConfiguration.BEARER_AUTH` defined in Task 1, used in Task 5 — consistent.
