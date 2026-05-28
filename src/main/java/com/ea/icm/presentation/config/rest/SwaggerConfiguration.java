package com.ea.icm.presentation.config.rest;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Here you can modify swagger to show dynamic API based on config using springdoc-openapi
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(title = "Intelligent Content Management API", version = "v1")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class SwaggerConfiguration {

    public final String contextPath;
    public final String localPort;

    public SwaggerConfiguration(
            @Value("${server.servlet.context-path}") String contextPath,
            @Value("${server.port}") String localPort) {
        this.contextPath = contextPath;
        this.localPort = localPort;
    }

}
