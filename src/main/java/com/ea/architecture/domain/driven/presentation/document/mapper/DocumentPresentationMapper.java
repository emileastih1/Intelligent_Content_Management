package com.ea.architecture.domain.driven.presentation.document.mapper;

import com.ea.architecture.domain.driven.application.document.dto.DocumentDto;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.presentation.common.adapter.EntityMapperUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

import javax.swing.text.Document;

@Component
@Mapper(componentModel = "spring")
public interface DocumentPresentationMapper extends EntityMapperUtil {
    @Mapping(target = "owner", source = "creationUser")
    public DocumentAggregate dtoToDomain(DocumentDto document);

    public DocumentDto domainToDto(DocumentAggregate document);
}
