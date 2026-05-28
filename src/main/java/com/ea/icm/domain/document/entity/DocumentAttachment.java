package com.ea.icm.domain.document.entity;

import java.util.Map;

public record DocumentAttachment(
        long documentId,
        String documentName,
        String documentType,
        Map<String, String> documentMetadata,
        byte[] file
) {
}
