package com.ea.architecture.domain.driven.application.document.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Base64;

public record AddDocumentDto( @NotNull String name,
                             Base64 file,
                             String fileSize
                             ){
}
