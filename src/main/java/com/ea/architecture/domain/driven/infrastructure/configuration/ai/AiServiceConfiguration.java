package com.ea.architecture.domain.driven.infrastructure.configuration.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AiServiceConfiguration {
    @Value("${aiServiceClient.url}")
    private String aiServiceUrl;
    @Value("${aiServiceClient.path}")
    private String aiServicePath;
    @Value("${aiServiceClient.type}")
    private String aiServiceType;

    @Bean("aiServiceRestClient")
    public RestClient restClient(RestClient.Builder builder) {
        return builder
                .baseUrl(aiServiceUrl + aiServicePath)
                .build();
    }

}
