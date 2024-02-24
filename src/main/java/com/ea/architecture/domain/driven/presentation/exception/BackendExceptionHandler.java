package com.ea.architecture.domain.driven.presentation.exception;

import com.ea.architecture.domain.driven.domain.exception.FunctionalException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handler for the backend.
 */
@RestControllerAdvice
public class BackendExceptionHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger(BackendExceptionHandler.class);

    /**
     * Handles FunctionalException thrown when a request fails for a functional reason.
     *
     * @param ex The FunctionalException
     * @return The ApiErrorDto
     */
    @ExceptionHandler(value = FunctionalException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponse(
            responseCode = "400",
            description = ErrorMessageConstants.FUNCTIONAL_ERROR,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorDto.class)
            )
    )
    public ApiErrorDto handleFunctionalException(FunctionalException ex) {
        ApiErrorDto apiErrorDto = new ApiErrorDto(ErrorMessageConstants.FUNCTIONAL_ERROR, ex.getCode().name(), HttpStatus.BAD_REQUEST.value(), ex.getMessageDetails());
        LOGGER.error("Functional error: {}", apiErrorDto);
        return apiErrorDto;
    }


    /**
     * Handles AccessDeniedException thrown when a user tries to access a resource that he/she is not allowed to access @PreAuthorize
     *
     * @param ex The AccessDeniedException
     * @return The ApiErrorDto
     */
    @ExceptionHandler(value = AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ApiResponse(
            responseCode = "403",
            description = ErrorMessageConstants.FORBIDDEN_REQUEST_ACCESS,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorDto.class)
            )
    )
    public ApiErrorDto handleForbiddenException(AccessDeniedException ex) {
        ApiErrorDto apiErrorDto = new ApiErrorDto(ErrorMessageConstants.FORBIDDEN_REQUEST_ACCESS, ErrorMessageConstants.ERROR_CODE_FORBIDDEN, HttpStatus.FORBIDDEN.value(), ex.getMessage());
        LOGGER.error("Access denied error: {}", apiErrorDto);
        return apiErrorDto;
    }

    /**
     * Handles MethodArgumentNotValidException thrown when a request fails because of invalid arguments
     *
     * @param ex The MethodArgumentNotValidException
     * @return The ApiErrorDto
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponse(
            responseCode = "400",
            description = ErrorMessageConstants.ERROR_ARGUMENT_NOT_VALID_EXCEPTION,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorDto.class)
            )
    )
    public ApiErrorDto handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        StringBuilder allErrorMessages = new StringBuilder();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            allErrorMessages.append(fieldName).append(": ").append(errorMessage).append(" ");
        });
        ApiErrorDto apiErrorDto = new ApiErrorDto(ErrorMessageConstants.ERROR_ARGUMENT_NOT_VALID_EXCEPTION, ErrorMessageConstants.ERROR_CODE_ARGUMENT_NOT_VALID_EXCEPTION, HttpStatus.BAD_REQUEST.value(), allErrorMessages.toString());
        LOGGER.error("Validation error: {}", apiErrorDto);
        return apiErrorDto;
    }

}
