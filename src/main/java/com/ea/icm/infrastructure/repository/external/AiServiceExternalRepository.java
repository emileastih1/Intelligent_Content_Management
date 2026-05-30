package com.ea.icm.infrastructure.repository.external;

import com.ea.icm.domain.document.model.DocumentAggregate;
import com.ea.icm.domain.document.vo.ai.Answer;
import com.ea.icm.domain.document.vo.ai.Question;
import com.ea.icm.infrastructure.repository.external.clients.DocumentAiRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public class AiServiceExternalRepository {

    public static final Logger LOGGER = LoggerFactory.getLogger(AiServiceExternalRepository.class);

    DocumentAiRestClient documentAiRestClient;

    public AiServiceExternalRepository(DocumentAiRestClient documentAiRestClient) {
        this.documentAiRestClient = documentAiRestClient;
    }

    public void sendToVectorStore(DocumentAggregate aggregate) {
        LOGGER.info("AiServiceExternalRepository.sendToVectorStore aggregate: " + aggregate);
        documentAiRestClient.sendToVectorStore(aggregate);
    }

    public Answer askQuestion(Question question, int topK, Double temperature) {
        LOGGER.info("AiServiceExternalRepository.askQuestion question: " + question);
        return documentAiRestClient.askQuestion(question, topK, temperature);
    }

    public Flux<String> streamAnswer(Question question, int topK, Double temperature) {
        LOGGER.info("AiServiceExternalRepository.streamAnswer topK: {}, temperature: {}", topK, temperature);
        return documentAiRestClient.streamAnswer(question, topK, temperature);
    }

    public Flux<String> streamAnswer(Question question, int topK, Double temperature,
                                     java.util.List<Long> documentIds) {
        return documentAiRestClient.streamAnswer(question, topK, temperature, documentIds);
    }

    public void deleteFromVectorStore(long documentId) {
        documentAiRestClient.deleteFromVectorStore(documentId);
    }

    public String classifySentiment(String content) {
        return documentAiRestClient.classifySentiment(content);
    }

    public void embedContent(long documentId, String documentName, String content) {
        documentAiRestClient.embedContent(documentId, documentName, content);
    }
}
