package com.ea.icm.infrastructure.persistance.external.adapter.query;

import com.ea.icm.domain.document.vo.ai.Answer;
import com.ea.icm.domain.document.vo.ai.Question;
import com.ea.icm.domain.externe.repository.query.AiServiceClientQueryPort;
import com.ea.icm.infrastructure.repository.external.AiServiceExternalRepository;
import org.springframework.stereotype.Service;

@Service
public class DocumentAiQueryServiceClientCommandAdapter implements AiServiceClientQueryPort {

    AiServiceExternalRepository aiServiceExternalRepository;

    public DocumentAiQueryServiceClientCommandAdapter(AiServiceExternalRepository aiServiceExternalRepository) {
        this.aiServiceExternalRepository = aiServiceExternalRepository;
    }

    @Override
    public Answer askQuestion(Question question, int topK, Double temperature) {
        return aiServiceExternalRepository.askQuestion(question, topK, temperature);
    }
}
