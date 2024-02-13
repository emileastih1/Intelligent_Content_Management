package com.ea.architecture.domain.driven.presentation.exception;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@ApiResponse(responseCode = "403", description = ErrorMessageConstants.FORBIDDEN_REQUEST_ACCESS, content = @Content(schema = @Schema(implementation = String.class)))
@ApiResponse(responseCode = "401", description = ErrorMessageConstants.UNAUTHORIZED_REQUEST_ACCESS, content = @Content(schema = @Schema(implementation = String.class)))
public @interface ApiResponseUnauthorized {
}
