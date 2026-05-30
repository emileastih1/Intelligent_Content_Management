package com.ea.icm.presentation.document.api.query;

import com.ea.icm.application.document.dto.DocumentDto;
import com.ea.icm.application.document.port.query.DocumentManagementQueryService;
import com.ea.icm.domain.document.model.DocumentAggregate;

import java.util.List;
import com.ea.icm.presentation.config.security.JwtSecurityConfig;
import com.ea.icm.presentation.document.mapper.DocumentPresentationMapper;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest(
        value = DocumentQueryRestController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtSecurityConfig.class)
)
@Import(DocumentQueryRestControllerTest.TestSecurityConfig.class)
public class DocumentQueryRestControllerTest {

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
                "Legal Document", "the document body", List.of("law"), "legal", "98785", "25 MB", "/home/documents");

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
