package com.ea.icm.application.external.port.query.impl;

import com.ea.icm.application.external.port.query.AiServiceClientQuery;
import com.ea.icm.domain.document.vo.ai.Answer;
import com.ea.icm.domain.document.vo.ai.Question;
import com.ea.icm.domain.externe.repository.query.AiServiceClientQueryPort;
import org.springframework.stereotype.Service;

@Service
public class AiServiceClientQueryImpl implements AiServiceClientQuery {
    AiServiceClientQueryPort aiServiceClientQueryPort;

    public AiServiceClientQueryImpl(AiServiceClientQueryPort aiServiceClientQueryPort) {
        this.aiServiceClientQueryPort = aiServiceClientQueryPort;
    }

    @Override
    public Answer askQuestion(Question question) {
        return aiServiceClientQueryPort.askQuestion(question);
    }
}
