package com.ea.architecture.domain.driven.infrastructure.repository.external.clients;

import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.vo.ai.Answer;
import com.ea.architecture.domain.driven.domain.document.vo.ai.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DocumentAiRestClient {
    public static final Logger LOGGER = LoggerFactory.getLogger(DocumentAiRestClient.class);

    @Qualifier("aiServiceRestClient")
    RestClient restClient;

    public DocumentAiRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public void sendToVectorStore(DocumentAggregate aggregate) {
        LOGGER.info("DocumentAiRestClient.sendToVectorStore aggregate: " + aggregate);
        restClient.post()
                .uri("/v1/document")
                .contentType(MediaType.APPLICATION_JSON)
                .body(aggregate)
                .retrieve()
                .toBodilessEntity();
    }

    public Answer askQuestion(Question question) {
        LOGGER.info("DocumentAiRestClient.askQuestion question: " + question);
        return restClient.post()
                .uri("/v1/document/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .body(question)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Answer>() {
                })
                .getBody();
    }
}
