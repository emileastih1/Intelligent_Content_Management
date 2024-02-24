package com.ea.architecture.domain.driven.infrastructure.persistance.document.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(indexName = "document")
public record DocumentEntity(
        long documentId,
        String documentName,
        String documentType,
        String documentContent,
        String documentMetadata,
        @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis, name = "creationDate")
        String creationDate,
        @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis, name = "modificationDate")
        String modificationDate,
        String owner
)
{}
