package com.ea.architecture.domain.driven.domain.exception;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class FunctionalException extends RuntimeException{

    private final MessageCode code;
    private final String messageDetails;

    public FunctionalException(MessageCode code, String messageDetails) {
        super(code.name());
        this.code = code;
        this.messageDetails = messageDetails;
    }
}
