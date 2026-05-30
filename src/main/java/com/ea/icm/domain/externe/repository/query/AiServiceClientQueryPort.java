package com.ea.icm.domain.externe.repository.query;

import com.ea.icm.domain.document.vo.ai.Answer;
import com.ea.icm.domain.document.vo.ai.Question;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AiServiceClientQueryPort {
    Answer askQuestion(Question question, int topK, Double temperature);
    Flux<String> streamAnswer(Question question, int topK, Double temperature);
    Flux<String> streamAnswer(Question question, int topK, Double temperature, List<Long> documentIds);
    String classifySentiment(String content);
}
