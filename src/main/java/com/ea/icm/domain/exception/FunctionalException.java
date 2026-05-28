package com.ea.icm.domain.exception;

import lombok.Getter;

@Getter
public class FunctionalException extends RuntimeException {

    private final MessageCode code;
    private final String messageDetails;

    public FunctionalException(MessageCode code, String messageDetails) {
        super(code.name());
        this.code = code;
        this.messageDetails = messageDetails;
    }
}
