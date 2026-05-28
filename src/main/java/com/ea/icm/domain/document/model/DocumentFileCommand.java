package com.ea.icm.domain.document.model;

import java.util.Map;

public record DocumentFileCommand(
        long documentId,
        String elasticId,
        String documentName,
        String documentType,
        byte[] file,
        Map<String, String> documentMetadata) {
}
