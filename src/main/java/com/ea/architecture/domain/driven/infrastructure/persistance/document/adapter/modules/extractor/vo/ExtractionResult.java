package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.vo;

public record ExtractionResult(String extractedText, String metadata) {
    public ExtractionResult(String extractedText) {
        this(extractedText, null);
    }
}
