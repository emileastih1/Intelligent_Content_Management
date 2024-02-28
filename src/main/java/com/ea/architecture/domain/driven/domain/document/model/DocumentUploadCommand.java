package com.ea.architecture.domain.driven.domain.document.model;

import com.ea.architecture.domain.driven.domain.common.model.UniqueId;

import java.util.Map;

public record DocumentUploadCommand(
        UniqueId documentId,
        String elasticId,
        String documentName,
        String documentType,
        byte[] file,
        Map<String, String> documentMetadata) {
}
