package com.ea.architecture.domain.driven.application.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AddDocumentDto(@NotBlank(message = "Document name is mandatory")
                             @Schema(description = "Name of the document", example = "Test Document", requiredMode = Schema.RequiredMode.REQUIRED)
                             String name,
                             @NotBlank(message = "File is mandatory")
                             @Schema(description = "Base64 representation of the file", example = "base64string" ,requiredMode = Schema.RequiredMode.REQUIRED)
                             String base64File,
                             @Schema(description = "Size of the file", example = "5 MB", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                             String fileSize,
                             @NotBlank(message = "Document type is mandatory")
                             @Schema(description = "Type of the file", example = "PDF", requiredMode = Schema.RequiredMode.REQUIRED)
                             String fileType
) {
}
