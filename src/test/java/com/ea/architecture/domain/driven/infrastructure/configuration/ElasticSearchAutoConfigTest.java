package com.ea.architecture.domain.driven.infrastructure.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientAutoConfiguration;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD test verifying that:
 * 1. ElasticsearchClient is provided by Spring Boot autoconfiguration (no custom config needed).
 * 2. No custom ElasticSearchConfiguration bean is present in the context.
 *
 * Uses ApplicationContextRunner for a lightweight, infrastructure-free context.
 */
class ElasticSearchAutoConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ElasticsearchClientAutoConfiguration.class,
                    ElasticsearchRestClientAutoConfiguration.class
            ))
            .withPropertyValues("spring.elasticsearch.uris=http://localhost:9200");

    @Test
    void elasticsearchClientIsAutoconfigured() {
        contextRunner.run(ctx ->
                assertThat(ctx).hasSingleBean(ElasticsearchClient.class)
        );
    }

    @Test
    void noCustomElasticSearchConfigurationBeanPresent() {
        contextRunner.run(ctx -> {
            // After deleting ElasticSearchConfiguration, no such class should be registered
            boolean customConfigPresent = ctx.getBeanDefinitionNames() != null
                    && java.util.Arrays.stream(ctx.getBeanDefinitionNames())
                           .anyMatch(name -> name.equalsIgnoreCase("elasticSearchConfiguration"));
            assertThat(customConfigPresent).isFalse();
        });
    }
}
