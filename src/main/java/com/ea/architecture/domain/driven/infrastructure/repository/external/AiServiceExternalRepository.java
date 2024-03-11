package com.ea.architecture.domain.driven.infrastructure.repository.external;

import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.vo.ai.Answer;
import com.ea.architecture.domain.driven.domain.document.vo.ai.Question;
import com.ea.architecture.domain.driven.infrastructure.repository.external.clients.DocumentAiRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

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

    public Answer askQuestion(Question question) {
        LOGGER.info("AiServiceExternalRepository.askQuestion question: " + question);
        return documentAiRestClient.askQuestion(question);
    }
}
