package com.ea.architecture.domain.driven.presentation.document.mapper;

import com.ea.architecture.domain.driven.application.document.dto.DocumentDto;
import com.ea.architecture.domain.driven.application.document.dto.DocumentResult;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.common.adapter.EntityMapperUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public interface DocumentPresentationMapper extends EntityMapperUtil {
    @Mapping(target = "owner", source = "creationUser")
    @Mapping(target = "documentName", source = "name")
    public DocumentAggregate dtoToDomain(DocumentDto document);

    @Mapping(target = "creationUser", source = "owner")
    @Mapping(target = "name", source = "documentName")
    public DocumentDto domainToDto(DocumentAggregate document);

    @Mapping(target = "status", source = "documentResult")
    DocumentResult toDocumentResult(String documentResult);
}
