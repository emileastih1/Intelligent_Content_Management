package com.ea.architecture.domain.driven.presentation.exception;

public class ErrorMessageConstants {

    public static final String FORBIDDEN_REQUEST_ACCESS = """
            Request failed, the request is made towards an endpoint
            with invalid API credentials
            """;
    public static final String UNAUTHORIZED_REQUEST_ACCESS = """
            Request failed, the request is made with valid credentials towards an endpoint
            that you do not have the access to.
            """;
    public static final String FUNCTIONAL_ERROR = "Request failed for a functional reason, see details";
    public static final String ERROR_CODE_NOT_AUTHORIZED = "NOT_AUTHORIZED";
}
