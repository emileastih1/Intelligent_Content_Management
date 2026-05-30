package com.ea.icm.application.external.port.query.impl;

import com.ea.icm.application.external.port.query.AiServiceClientQuery;
import com.ea.icm.domain.document.vo.ai.Answer;
import com.ea.icm.domain.document.vo.ai.Question;
import com.ea.icm.domain.externe.repository.query.AiServiceClientQueryPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AiServiceClientQueryImpl implements AiServiceClientQuery {
    AiServiceClientQueryPort aiServiceClientQueryPort;

    public AiServiceClientQueryImpl(AiServiceClientQueryPort aiServiceClientQueryPort) {
        this.aiServiceClientQueryPort = aiServiceClientQueryPort;
    }

    @Override
    public Answer askQuestion(Question question, int topK, Double temperature) {
        return aiServiceClientQueryPort.askQuestion(question, topK, temperature);
    }

    @Override
    public Flux<String> streamAnswer(Question question, int topK, Double temperature) {
        return aiServiceClientQueryPort.streamAnswer(question, topK, temperature);
    }
}
