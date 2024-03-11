package com.ea.architecture.domain.driven.domain.document.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record Question(
        @NotBlank(message = "Question is mandatory")
        @Schema(description = "Question about documents", example = "base64string", requiredMode = Schema.RequiredMode.REQUIRED)
        String question) {
}
