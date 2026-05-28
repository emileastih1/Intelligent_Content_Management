# Migrate Controller Tests to @WebMvcTest with jwt() Post-Processor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate all three controller tests from `standaloneSetup` + `httpBasic` to `@WebMvcTest` slices that load the Spring Security filter chain, and add 401/403 authorization scenarios per endpoint using the `jwt()` post-processor.

**Architecture:** Each controller test class is rewritten as a standalone `@WebMvcTest` (no longer extending `AbstractRestTest`). The Spring Security filter chain is loaded automatically by `@WebMvcTest`, and `@MockBean JwtDecoder` prevents auto-configuration from requiring a real Keycloak server. The `AbstractRestTest` base class is left untouched.

**Tech Stack:** Java 21, Spring Boot 4.0.5, Spring Security Test (`jwt()` post-processor), JUnit 5, Mockito, `@WebMvcTest`

---

## File Map

| File | Action |
|---|---|
| `src/test/java/com/ea/icm/presentation/document/api/command/DocumentCommandRestControllerTest.java` | Rewrite — drop `AbstractRestTest`, add `@WebMvcTest`, add 401/403 tests |
| `src/test/java/com/ea/icm/presentation/document/api/query/DocumentQueryRestControllerTest.java` | Rewrite — drop `AbstractRestTest`, add `@WebMvcTest`, add 401/403 tests |
| `src/test/java/com/ea/icm/presentation/user/api/UserQueryRestControllerTest.java` | Rewrite — drop `AbstractRestTest`, add `@WebMvcTest`, add 401/403 tests |
| `src/test/java/com/ea/icm/common/AbstractRestTest.java` | No change |
| `pom.xml` | No change — `spring-security-test` already present |

---

### Task 1: Rewrite DocumentCommandRestControllerTest

**Files:**
- Modify: `src/test/java/com/ea/icm/presentation/document/api/command/DocumentCommandRestControllerTest.java`

- [ ] **Step 1: Write the new test class**

Replace the entire file with:

```java
package com.ea.icm.presentation.document.api.command;

import com.ea.icm.application.document.dto.AddDocumentDto;
import com.ea.icm.application.document.port.command.DocumentManagementCommandService;
import com.ea.icm.domain.document.entity.DocumentResult;
import com.ea.icm.domain.document.model.DocumentAggregate;
import com.ea.icm.domain.document.vo.DocumentStatus;
import com.ea.icm.domain.document.vo.DocumentTypes;
import com.ea.icm.domain.document.vo.FileSize;
import com.ea.icm.domain.document.vo.UnitOfMeasurement;
import com.ea.icm.presentation.document.mapper.DocumentPresentationMapper;
import com.ea.icm.presentation.exception.ErrorMessageConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.Charset;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest(DocumentCommandRestController.class)
class DocumentCommandRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DocumentManagementCommandService documentManagementCommandService;

    @MockitoBean
    private DocumentPresentationMapper documentPresentationMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("Authorization: POST /api/v1/document")
    class AuthorizationTests {

        @Test
        @DisplayName("No token -> 401 Unauthorized")
        void whenNoToken_thenUnauthorized() throws Exception {
            AddDocumentDto addDocumentDto = new AddDocumentDto("TestDocument", "base64File", "15 MB", "JPG");
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/v1/document")
                            .content(objectMapper.writeValueAsString(addDocumentDto))
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(Charset.defaultCharset()))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }

        @Test
        @DisplayName("Wrong role (ROLE_READ) -> 403 Forbidden")
        void whenWrongRole_thenForbidden() throws Exception {
            AddDocumentDto addDocumentDto = new AddDocumentDto("TestDocument", "base64File", "15 MB", "JPG");
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/v1/document")
                            .content(objectMapper.writeValueAsString(addDocumentDto))
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(Charset.defaultCharset())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_READ"))))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Arguments validation: when a post request is made to add a document")
    class ArgumentsValidation {

        @Test
        @DisplayName("With valid data then add document")
        void whenPostRequestToAddDocumentWithValidData_thenAddDocument() throws Exception {
            AddDocumentDto addDocumentDto = new AddDocumentDto("TestDocument", "base64File", "15 MB", "JPG");

            DocumentResult documentResult = new DocumentResult("123", "", "", "", DocumentStatus.CREATED.name());
            DocumentAggregate documentAggregate = DocumentAggregate.builder()
                    .id(123L)
                    .documentName("TestDocument")
                    .documentType(DocumentTypes.JPG)
                    .fileSize(new FileSize("15", UnitOfMeasurement.MB))
                    .file(null)
                    .build();

            Mockito.doReturn(documentAggregate).when(documentPresentationMapper).dtoAddDocumentToDomain(addDocumentDto);
            Mockito.doReturn(documentResult).when(documentManagementCommandService).addDocument(documentAggregate);

            String jsonRequest = objectMapper.writeValueAsString(addDocumentDto);
            String jsonResult = objectMapper.writeValueAsString(documentResult);

            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/v1/document")
                            .content(jsonRequest)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(Charset.defaultCharset())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WRITE"))))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.content().json(jsonResult));
        }

        @Test
        @DisplayName("With empty document name then bad request")
        void whenPostRequestToAddDocumentWithEmptyName_thenThrowMethodArgumentNotValidException() throws Exception {
            AddDocumentDto addDocumentDto = new AddDocumentDto("", "base64File", "15 MB", "JPG");

            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/v1/document")
                            .content(objectMapper.writeValueAsString(addDocumentDto))
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(Charset.defaultCharset())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WRITE"))))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.title").value(ErrorMessageConstants.ERROR_ARGUMENT_NOT_VALID_EXCEPTION))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value(ErrorMessageConstants.ERROR_CODE_ARGUMENT_NOT_VALID_EXCEPTION))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value("name: Document name is mandatory "));
        }

        @Test
        @DisplayName("With empty file then bad request")
        void whenPostRequestToAddDocumentWithEmptyFile_thenThrowMethodArgumentNotValidException() throws Exception {
            AddDocumentDto addDocumentDto = new AddDocumentDto("TestDocument", "", "15 MB", "JPG");

            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/v1/document")
                            .content(objectMapper.writeValueAsString(addDocumentDto))
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(Charset.defaultCharset())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WRITE"))))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.title").value(ErrorMessageConstants.ERROR_ARGUMENT_NOT_VALID_EXCEPTION))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value(ErrorMessageConstants.ERROR_CODE_ARGUMENT_NOT_VALID_EXCEPTION))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value("base64File: File is mandatory "));
        }

        @Test
        @DisplayName("With empty document type then bad request")
        void whenPostRequestToAddDocumentWithEmptyDocumentType_thenThrowMethodArgumentNotValidException() throws Exception {
            AddDocumentDto addDocumentDto = new AddDocumentDto("TestDocument", "base64File", "15 MB", "");

            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/v1/document")
                            .content(objectMapper.writeValueAsString(addDocumentDto))
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(Charset.defaultCharset())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WRITE"))))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.title").value(ErrorMessageConstants.ERROR_ARGUMENT_NOT_VALID_EXCEPTION))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value(ErrorMessageConstants.ERROR_CODE_ARGUMENT_NOT_VALID_EXCEPTION))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value("fileType: Document type is mandatory "));
        }
    }
}
```

- [ ] **Step 2: Run only this test class to verify it compiles and passes**

Run: `.\mvnw.cmd test -Pwindows-docker-desktop -pl . -Dtest=DocumentCommandRestControllerTest -q`
Expected: All tests in this class pass

---

### Task 2: Rewrite DocumentQueryRestControllerTest

**Files:**
- Modify: `src/test/java/com/ea/icm/presentation/document/api/query/DocumentQueryRestControllerTest.java`

- [ ] **Step 1: Write the new test class**

Replace the entire file with:

```java
package com.ea.icm.presentation.document.api.query;

import com.ea.icm.application.document.dto.DocumentDto;
import com.ea.icm.application.document.port.query.DocumentManagementQueryService;
import com.ea.icm.domain.document.model.DocumentAggregate;
import com.ea.icm.presentation.document.mapper.DocumentPresentationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest(DocumentQueryRestController.class)
public class DocumentQueryRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DocumentManagementQueryService documentManagementQueryService;

    @MockitoBean
    private DocumentPresentationMapper documentPresentationMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("Authorization: GET /api/v1/document/{id}")
    class AuthorizationTests {

        @Test
        @DisplayName("No token -> 401 Unauthorized")
        void whenNoToken_thenUnauthorized() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/v1/document/1")
                            .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }

        @Test
        @DisplayName("Wrong role (ROLE_WRITE) -> 403 Forbidden")
        void whenWrongRole_thenForbidden() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/api/v1/document/1")
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WRITE"))))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isForbidden());
        }
    }

    @Test
    @DisplayName("Should return document given valid id")
    void should_return_document_given_valid_id() throws Exception {
        DocumentDto document = new DocumentDto(1L, "1212121212",
                "Legal Document", "98785", "25 MB", "/home/documents");

        DocumentAggregate documentAggregate = DocumentAggregate.builder()
                .id(1L)
                .documentName("Legal Document")
                .owner("98785")
                .location("/home/documents")
                .build();

        Mockito.when(documentManagementQueryService.findDocumentById("1")).thenReturn(documentAggregate);
        Mockito.when(documentPresentationMapper.domainToDto(documentAggregate)).thenReturn(document);

        String jsonReturned = objectMapper.writeValueAsString(document);

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/document/1")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_READ"))))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(jsonReturned))
                .andExpect(MockMvcResultMatchers.jsonPath("$.documentName").value("Legal Document"));
    }

    @Test
    @DisplayName("Should throw given invalid id")
    void should_throw_given_invalid_id() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/document/")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_READ"))))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }
}
```

- [ ] **Step 2: Run only this test class**

Run: `.\mvnw.cmd test -Pwindows-docker-desktop -Dtest=DocumentQueryRestControllerTest -q`
Expected: All tests pass

---

### Task 3: Rewrite UserQueryRestControllerTest

**Files:**
- Modify: `src/test/java/com/ea/icm/presentation/user/api/UserQueryRestControllerTest.java`

- [ ] **Step 1: Write the new test class**

Replace the entire file with:

```java
package com.ea.icm.presentation.user.api;

import com.ea.icm.application.user.dto.UserDto;
import com.ea.icm.application.user.port.query.UserManagementQueryService;
import com.ea.icm.domain.common.model.UniqueUserId;
import com.ea.icm.domain.user.model.UserAggregate;
import com.ea.icm.presentation.user.mapper.UserPresentationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.Charset;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest(UserQueryRestController.class)
public class UserQueryRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserManagementQueryService userManagementQueryService;

    @MockitoBean
    private UserPresentationMapper userPresentationMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("Authorization: POST /api/v1/person/search")
    class PersonSearchAuthorizationTests {

        @Test
        @DisplayName("No token -> 401 Unauthorized")
        void whenNoToken_thenUnauthorized() throws Exception {
            UserDto inputDto = new UserDto(1L, "", "", "", null);
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/v1/person/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(Charset.defaultCharset())
                            .content(objectMapper.writeValueAsString(inputDto)))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }

        @Test
        @DisplayName("Wrong role (ROLE_WRITE) -> 403 Forbidden")
        void whenWrongRole_thenForbidden() throws Exception {
            UserDto inputDto = new UserDto(1L, "", "", "", null);
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/v1/person/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(Charset.defaultCharset())
                            .content(objectMapper.writeValueAsString(inputDto))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WRITE"))))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Authorization: GET /api/v1/test")
    class TestGetAuthorizationTests {

        @Test
        @DisplayName("No token -> 401 Unauthorized")
        void whenNoToken_thenUnauthorized() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/test"))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }

        @Test
        @DisplayName("Wrong role (ROLE_WRITE) -> 403 Forbidden")
        void whenWrongRole_thenForbidden() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/test")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WRITE"))))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Authorization: POST /api/v1/test/post")
    class TestPostAuthorizationTests {

        @Test
        @DisplayName("No token -> 401 Unauthorized")
        void whenNoToken_thenUnauthorized() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/test/post"))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }

        @Test
        @DisplayName("Wrong role (ROLE_WRITE) -> 403 Forbidden")
        void whenWrongRole_thenForbidden() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/test/post")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WRITE"))))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isForbidden());
        }
    }

    @Test
    @DisplayName("Find user with correct role -> 200 OK")
    void testFindUserEndpoint() throws Exception {
        UserDto inputDto = new UserDto(1L, "", "", "", null);
        UserAggregate userAggregateInput = new UserAggregate();
        userAggregateInput.setId(new UniqueUserId(inputDto.id()));

        UserAggregate userAggregateReturned = new UserAggregate(new UniqueUserId(inputDto.id()), "Emile", "Astih", "33", null);
        UserDto dtoReturned = new UserDto(1L, "Emile", "Astih", "33", null);

        Mockito.doReturn(userAggregateReturned).when(userManagementQueryService).getUserByFilter(userAggregateInput);
        Mockito.doReturn(userAggregateInput).when(userPresentationMapper).dtoToDomain(inputDto);
        Mockito.doReturn(dtoReturned).when(userPresentationMapper).domainToDto(userAggregateReturned);

        String jsonRequest = objectMapper.writeValueAsString(inputDto);
        String jsonResult = objectMapper.writeValueAsString(dtoReturned);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/person/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(Charset.defaultCharset())
                        .content(jsonRequest)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_READ"))))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(jsonResult))
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        System.out.println("Response JSON: " + jsonResponse);
    }

    @Test
    @DisplayName("GET /api/v1/test with correct role -> 200 OK")
    void testGetHttpVerb() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/test")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_READ"))))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/test/post with correct role -> 200 OK")
    void testPostHttpVerb() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/test/post")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_READ"))))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
```

- [ ] **Step 2: Run only this test class**

Run: `.\mvnw.cmd test -Pwindows-docker-desktop -Dtest=UserQueryRestControllerTest -q`
Expected: All tests pass

---

### Task 4: Run all tests and commit

- [ ] **Step 1: Run all tests**

Run: `.\mvnw.cmd test -Pwindows-docker-desktop`
Expected: More than 17 tests pass, BUILD SUCCESS

- [ ] **Step 2: Commit**

```bash
git add src/test/java/com/ea/icm/presentation/document/api/command/DocumentCommandRestControllerTest.java
git add src/test/java/com/ea/icm/presentation/document/api/query/DocumentQueryRestControllerTest.java
git add src/test/java/com/ea/icm/presentation/user/api/UserQueryRestControllerTest.java
git commit -m "test: migrate controller tests to @WebMvcTest with jwt() post-processor (#48)"
```

---

## Self-Review

**Spec coverage:**
- [x] `spring-security-test` verified present — no pom.xml change needed
- [x] `@WebMvcTest` with `@Autowired MockMvc` — all three classes
- [x] `@MockitoBean JwtDecoder` — all three classes
- [x] No token → 401: DocumentCommand, DocumentQuery, UserQuery (all 3 endpoints)
- [x] Correct role → 200: all existing happy-path tests migrated to use `jwt()` with correct role
- [x] Wrong role → 403: DocumentCommand(ROLE_READ), DocumentQuery(ROLE_WRITE), UserQuery (all 3 endpoints with ROLE_WRITE)
- [x] `AbstractRestTest` untouched
- [x] No production code changes

**Note on `@MockitoBean`:** Spring Boot 4.x uses `@MockitoBean` (from `org.springframework.test.context.bean.override.mockito`) instead of the deprecated `@MockBean`. This is consistent with the Spring Boot 4.0.5 stack.
