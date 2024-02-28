package com.ea.architecture.domain.driven.domain.document.entity;

import java.util.Map;

public record DocumentAttachment(
    String documentId,
    String documentName,
    String documentType,
    Map<String, String> documentMetadata,
    byte[] file
) {
}
