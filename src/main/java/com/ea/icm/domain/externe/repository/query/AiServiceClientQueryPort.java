package com.ea.icm.domain.externe.repository.query;

import com.ea.icm.domain.document.vo.ai.Answer;
import com.ea.icm.domain.document.vo.ai.Question;

public interface AiServiceClientQueryPort {
    Answer askQuestion(Question question, int topK, Double temperature);
}
