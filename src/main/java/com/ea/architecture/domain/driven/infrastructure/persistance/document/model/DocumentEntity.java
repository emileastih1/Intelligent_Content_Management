package com.ea.architecture.domain.driven.infrastructure.persistance.document.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(indexName = "document")
public final class DocumentEntity {
    private String elasticId;
    private String documentId;
    @Field(type = FieldType.Text, name = "documentName")
    private String documentName;
    private String documentType;
    private String documentContent;
    private String documentMetadata;
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis, name = "creationDate")
    private String creationDate;
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis, name = "modificationDate")
    private String modificationDate;
    private  String owner;

}
