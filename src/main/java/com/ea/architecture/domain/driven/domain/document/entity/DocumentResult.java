package com.ea.architecture.domain.driven.domain.document.entity;

public record DocumentResult(String id,
                             String documentName,
                             String documentType,
                             String fileSize,
                             String base64File,
                             String status) {
    public DocumentResult(String id, String status) {
        this(id, null, null, null, null, status);
    }

    public DocumentResult() {
        this(null, null, null, null, null, null);
    }

    public DocumentResult(String documentId) {
        this(documentId, null, null, null, null, null);
    }
}
