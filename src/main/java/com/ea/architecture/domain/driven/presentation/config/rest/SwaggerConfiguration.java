package com.ea.architecture.domain.driven.presentation.config.rest;


import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Here you can modify swagger to show dynamic API based on config using springdoc-openapi
 */
@Configuration
public class SwaggerConfiguration {

    public final String contextPath;
    public final String localPort;

    public SwaggerConfiguration(
            @Value("${server.servlet.context-path}")  String contextPath,
            @Value("${server.port}") String localPort) {
        this.contextPath = contextPath;
        this.localPort = localPort;
    }

}
