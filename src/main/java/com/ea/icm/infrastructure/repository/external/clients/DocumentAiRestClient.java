package com.ea.icm.infrastructure.repository.external.clients;

import com.ea.icm.domain.document.model.DocumentAggregate;
import com.ea.icm.domain.document.vo.ai.Answer;
import com.ea.icm.domain.document.vo.ai.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Component
public class DocumentAiRestClient {
    public static final Logger LOGGER = LoggerFactory.getLogger(DocumentAiRestClient.class);

    @Qualifier("aiServiceRestClient")
    RestClient restClient;

    @Qualifier("streamingAiServiceWebClient")
    private final WebClient webClient;

    public DocumentAiRestClient(@Qualifier("aiServiceRestClient") RestClient restClient,
                                @Qualifier("streamingAiServiceWebClient") WebClient webClient) {
        this.restClient = restClient;
        this.webClient = webClient;
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

    public Answer askQuestion(Question question, int topK, Double temperature) {
        LOGGER.info("DocumentAiRestClient.askQuestion question: " + question);
        return restClient.post()
                .uri(uriBuilder -> {
                    uriBuilder.path("/v1/document/ask").queryParam("topK", topK);
                    if (temperature != null) { uriBuilder.queryParam("temperature", temperature); }
                    return uriBuilder.build();
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(question)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Answer>() {
                })
                .getBody();
    }

    public Flux<String> streamAnswer(Question question, int topK, Double temperature) {
        LOGGER.info("DocumentAiRestClient.streamAnswer topK: {}, temperature: {}", topK, temperature);
        return webClient.post()
                .uri(uriBuilder -> {
                    uriBuilder.path("/AiServiceClient/v1/document/ask").queryParam("topK", topK);
                    if (temperature != null) { uriBuilder.queryParam("temperature", temperature); }
                    return uriBuilder.build();
                })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(question)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .mapNotNull(ServerSentEvent::data)
                .filter(data -> !data.isBlank());
    }
}
