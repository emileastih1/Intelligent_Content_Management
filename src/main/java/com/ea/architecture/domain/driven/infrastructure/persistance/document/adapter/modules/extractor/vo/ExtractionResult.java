package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.vo;

import java.util.Map;

public record ExtractionResult(String extractedText, Map<String , String> metadata) {
    public ExtractionResult(String extractedText){
        this(extractedText, null);
    }
}
