package com.ea.architecture.domain.driven.presentation.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.Validate;

import java.time.LocalDateTime;

public record ApiErrorDto (
        @Schema(description = "Error title", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,
        @Schema(description = "Error code for this specific error", requiredMode = Schema.RequiredMode.REQUIRED)
        String errorCode,

        @Schema(description = "Http status code generated for the error", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer status,

        @Schema(description = "Date when the error occurred", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
        LocalDateTime timestamp,

        @Schema(description = "Detail explanation about the error", requiredMode = Schema.RequiredMode.REQUIRED)
        String detail
        ) {

        public ApiErrorDto{
           validate(title, errorCode, status, detail);
        }
        public ApiErrorDto(String title, String errorCode, Integer status, String detail) {
            this(title, errorCode, status, LocalDateTime.now(), detail);
        }

    private void validate(String title, String errorCode, Integer status, String detail) {
        Validate.notBlank(title);
        Validate.notBlank(errorCode);
        Validate.notBlank(detail);
    }
}
