package com.ea.architecture.domain.driven.application.document.dto;

public record DocumentResult(long id,
                             String documentName,
                             String documentType,
                             String fileSize,
                             String base64File,
                             String status) {
}
