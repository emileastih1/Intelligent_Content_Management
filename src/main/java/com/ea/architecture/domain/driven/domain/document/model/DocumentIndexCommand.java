package com.ea.architecture.domain.driven.domain.document.model;

import java.util.Map;

public record DocumentIndexCommand(
        long documentId,
        String elasticId,
        String documentName,
        String documentType,
        byte[] file,
        Map<String, String> documentMetadata) {
}
