package com.ea.architecture.domain.driven.presentation.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BasicAuthSecurityTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    SecurityAutoConfiguration.class,
                    ServletWebSecurityAutoConfiguration.class,
                    SecurityFilterAutoConfiguration.class
            ))
            .withUserConfiguration(BasicAuthSecurity.class)
            .withPropertyValues("spring.profiles.active=secured");

    @Test
    void swaggerUiIsAccessibleWithoutAuth() {
        contextRunner.run(ctx -> {
            MockMvc mockMvc = MockMvcBuilders
                    .webAppContextSetup((WebApplicationContext) ctx)
                    .apply(springSecurity())
                    .build();
            mockMvc.perform(get("/swagger-ui.html"))
                   .andExpect(status().isNotFound()); // 404 = not secured, just no handler
        });
    }

    @Test
    void actuatorHealthIsAccessibleWithoutAuth() {
        contextRunner.run(ctx -> {
            MockMvc mockMvc = MockMvcBuilders
                    .webAppContextSetup((WebApplicationContext) ctx)
                    .apply(springSecurity())
                    .build();
            mockMvc.perform(get("/actuator/health"))
                   .andExpect(status().isNotFound()); // 404 = not secured
        });
    }

    @Test
    void apiEndpointRequiresAuth() {
        contextRunner.run(ctx -> {
            MockMvc mockMvc = MockMvcBuilders
                    .webAppContextSetup((WebApplicationContext) ctx)
                    .apply(springSecurity())
                    .build();
            mockMvc.perform(get("/api/documents"))
                   .andExpect(status().isUnauthorized());
        });
    }
}
