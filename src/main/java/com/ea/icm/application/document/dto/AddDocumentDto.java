package com.ea.icm.application.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AddDocumentDto(@NotBlank(message = "Document name is mandatory")
                             @Schema(description = "Name of the document", example = "Test Document", requiredMode = Schema.RequiredMode.REQUIRED)
                             String name,
                             @Schema(description = "Editable text content (authoring ingestion path)", example = "hello world", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                             String content,
                             @Schema(description = "Free-form tags", example = "[\"java\",\"spring\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                             List<String> tags,
                             @Schema(description = "Single category", example = "engineering", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                             String category,
                             @Schema(description = "Base64 representation of the file (upload ingestion path)", example = "base64string", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                             String base64File,
                             @Schema(description = "Size of the file", example = "5 MB", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                             String fileSize,
                             @Schema(description = "Type of the file", example = "PDF", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                             String fileType
) {
}
