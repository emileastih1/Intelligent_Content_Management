package com.ea.architecture.domain.driven.infrastructure.persistance.external.adapter.query;

import com.ea.architecture.domain.driven.domain.document.vo.ai.Answer;
import com.ea.architecture.domain.driven.domain.document.vo.ai.Question;
import com.ea.architecture.domain.driven.domain.externe.repository.query.AiServiceClientQueryPort;
import com.ea.architecture.domain.driven.infrastructure.repository.external.AiServiceExternalRepository;
import org.springframework.stereotype.Service;

@Service
public class DocumentAiQueryServiceClientCommandAdapter implements AiServiceClientQueryPort {

    AiServiceExternalRepository aiServiceExternalRepository;

    public DocumentAiQueryServiceClientCommandAdapter(AiServiceExternalRepository aiServiceExternalRepository) {
        this.aiServiceExternalRepository = aiServiceExternalRepository;
    }

    @Override
    public Answer askQuestion(Question question) {
        return aiServiceExternalRepository.askQuestion(question);
    }
}
