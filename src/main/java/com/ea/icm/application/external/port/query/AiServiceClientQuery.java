package com.ea.icm.application.external.port.query;

import com.ea.icm.domain.document.vo.ai.Answer;
import com.ea.icm.domain.document.vo.ai.Question;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

@Service
@Transactional(readOnly = true)
public interface AiServiceClientQuery {
    Answer askQuestion(Question question, int topK, Double temperature);
    Flux<String> streamAnswer(Question question, int topK, Double temperature);
}
