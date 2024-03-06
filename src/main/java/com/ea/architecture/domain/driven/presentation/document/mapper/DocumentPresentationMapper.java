package com.ea.architecture.domain.driven.presentation.document.mapper;

import com.ea.architecture.domain.driven.application.document.dto.AddDocumentDto;
import com.ea.architecture.domain.driven.application.document.dto.DocumentDto;
import com.ea.architecture.domain.driven.domain.common.adapter.EntityMapperUtil;
import com.ea.architecture.domain.driven.domain.document.entity.DocumentResult;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
@Mapper(componentModel = "spring")
public interface DocumentPresentationMapper extends EntityMapperUtil {
    @Mapping(target = "owner", source = "creationUser")
    @Mapping(target = "fileSize", source = "fileSize", qualifiedByName = "mapStringToFileSize")
    DocumentAggregate dtoToDomain(DocumentDto document);

    @Mapping(target = "creationUser", source = "owner")
    @Mapping(target = "fileSize", source = "fileSize", qualifiedByName = "mapFileSizeToString")
    DocumentDto domainToDto(DocumentAggregate document);

    @Mapping(target = "status", source = "documentResult")
    DocumentResult toDocumentResult(String documentResult);

    @Mapping(target = "documentName", source = "name")
    @Mapping(target = "file", source = "base64File", qualifiedByName = "mapBase64StringToByteArray")
    @Mapping(target = "fileSize", source = "fileSize", qualifiedByName = "mapStringToFileSize")
    @Mapping(target = "documentType", source = "fileType", qualifiedByName = "mapStringToDocumentType")
    DocumentAggregate dtoAddDocumentToDomain(AddDocumentDto document);

    @Mapping(target = "base64File", source = "file", qualifiedByName = "mapByteArrayToBase64String")
    @Mapping(target = "documentName", source = "documentName")
    @Mapping(target = "id", source = "id", qualifiedByName = "mapLongToString")
    @Mapping(target = "fileSize", source = "fileSize", qualifiedByName = "mapFileSizeToString")
    DocumentResult domainToDocumentResult(DocumentAggregate document);

    /**
     * Set the creation date to the current date
     *
     * @param document
     */
    @AfterMapping
    default void setCreationDate(@MappingTarget DocumentAggregate document) {
        document.setCreationDate(ZonedDateTime.now());
    }
}
