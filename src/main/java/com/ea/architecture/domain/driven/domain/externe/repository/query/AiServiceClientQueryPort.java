package com.ea.architecture.domain.driven.domain.externe.repository.query;

import com.ea.architecture.domain.driven.domain.document.vo.ai.Answer;
import com.ea.architecture.domain.driven.domain.document.vo.ai.Question;

public interface AiServiceClientQueryPort {
    Answer askQuestion(Question question);
}
