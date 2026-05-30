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
        // Build a minimal payload carrying the ICM documentId for chunk tagging (ADR-0007)
        var payload = new java.util.HashMap<String, Object>();
        payload.put("id", aggregate.getId());
        payload.put("documentId", aggregate.getId());
        payload.put("documentName", aggregate.getDocumentName());
        payload.put("file", aggregate.getFile());
        restClient.post()
                .uri("/v1/document")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteFromVectorStore(long documentId) {
        LOGGER.info("DocumentAiRestClient.deleteFromVectorStore documentId: {}", documentId);
        restClient.delete()
                .uri("/v1/document/" + documentId)
                .retrieve()
                .toBodilessEntity();
    }

    public String classifySentiment(String content) {
        LOGGER.info("DocumentAiRestClient.classifySentiment");
        var response = restClient.post()
                .uri("/v1/document/classify-sentiment")
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("content", content))
                .retrieve()
                .toEntity(new org.springframework.core.ParameterizedTypeReference<java.util.Map<String, String>>() {})
                .getBody();
        return response != null ? response.getOrDefault("sentiment", "Neutral") : "Neutral";
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
        return streamAnswer(question, topK, temperature, null);
    }

    public Flux<String> streamAnswer(Question question, int topK, Double temperature,
                                     java.util.List<Long> documentIds) {
        LOGGER.info("DocumentAiRestClient.streamAnswer topK: {}, temperature: {}, docIds: {}",
                topK, temperature, documentIds);
        return webClient.post()
                .uri(uriBuilder -> {
                    uriBuilder.path("/AiServiceClient/v1/document/ask").queryParam("topK", topK);
                    if (temperature != null) { uriBuilder.queryParam("temperature", temperature); }
                    if (documentIds != null && !documentIds.isEmpty()) {
                        uriBuilder.queryParam("documentIds", documentIds.stream()
                                .map(String::valueOf).toArray());
                    }
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
