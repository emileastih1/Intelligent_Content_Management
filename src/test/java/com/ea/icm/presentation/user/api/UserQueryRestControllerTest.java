package com.ea.icm.presentation.user.api;

import com.ea.icm.application.user.dto.UserDto;
import com.ea.icm.application.user.port.query.UserManagementQueryService;
import com.ea.icm.domain.common.model.UniqueUserId;
import com.ea.icm.domain.user.model.UserAggregate;
import com.ea.icm.presentation.config.security.JwtSecurityConfig;
import com.ea.icm.presentation.user.mapper.UserPresentationMapper;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.Charset;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest(
        value = UserQueryRestController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtSecurityConfig.class)
)
@Import(UserQueryRestControllerTest.TestSecurityConfig.class)
public class UserQueryRestControllerTest {

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
    @DisplayName("Authorization: GET /api/test")
    class TestGetAuthorizationTests {

        @Test
        @DisplayName("No token -> 401 Unauthorized")
        void whenNoToken_thenUnauthorized() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/test"))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }

        @Test
        @DisplayName("Wrong role (ROLE_WRITE) -> 403 Forbidden")
        void whenWrongRole_thenForbidden() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/test")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_WRITE"))))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Authorization: POST /api/test/post")
    class TestPostAuthorizationTests {

        @Test
        @DisplayName("No token -> 401 Unauthorized")
        void whenNoToken_thenUnauthorized() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/test/post"))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }

        @Test
        @DisplayName("Wrong role (ROLE_WRITE) -> 403 Forbidden")
        void whenWrongRole_thenForbidden() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/test/post")
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
    @DisplayName("GET /api/test with correct role -> 200 OK")
    void testGetHttpVerb() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/test")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_READ"))))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("POST /api/test/post with correct role -> 200 OK")
    void testPostHttpVerb() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/test/post")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_READ"))))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
