package com.ea.icm.presentation.document.api.command;

import com.ea.icm.application.document.dto.AddDocumentDto;
import com.ea.icm.application.document.port.command.DocumentManagementCommandService;
import com.ea.icm.domain.document.entity.DocumentResult;
import com.ea.icm.domain.document.model.DocumentAggregate;
import com.ea.icm.domain.document.vo.DocumentStatus;
import com.ea.icm.domain.document.vo.DocumentTypes;
import com.ea.icm.domain.document.vo.FileSize;
import com.ea.icm.domain.document.vo.UnitOfMeasurement;
import com.ea.icm.presentation.config.security.JwtSecurityConfig;
import com.ea.icm.presentation.document.mapper.DocumentPresentationMapper;
import com.ea.icm.presentation.exception.ErrorMessageConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.Charset;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest(
        value = DocumentCommandRestController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtSecurityConfig.class)
)
@Import(DocumentCommandRestControllerTest.TestSecurityConfig.class)
class DocumentCommandRestControllerTest {

    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
            AddDocumentDto addDocumentDto = new AddDocumentDto("TestDocument", null, "base64File", "15 MB", "JPG");
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
            AddDocumentDto addDocumentDto = new AddDocumentDto("TestDocument", null, "base64File", "15 MB", "JPG");
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
            AddDocumentDto addDocumentDto = new AddDocumentDto("TestDocument", null, "base64File", "15 MB", "JPG");

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
            AddDocumentDto addDocumentDto = new AddDocumentDto("", null, "base64File", "15 MB", "JPG");

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
        @DisplayName("Authoring without a file (content only) is accepted (content-first, ADR-0004)")
        void whenPostRequestToAuthorDocumentWithoutFile_thenOk() throws Exception {
            // Content-first: base64File and fileType are no longer mandatory; authored
            // content alone is a valid Document (ADR-0004).
            AddDocumentDto addDocumentDto = new AddDocumentDto("TestDocument", "hello world", null, null, null);

            mockMvc.perform(MockMvcRequestBuilders
                            .post("/api/v1/document")
                            .content(objectMapper.writeValueAsString(addDocumentDto))
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(Charset.defaultCharset())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WRITE"))))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isOk());
        }
    }
}
