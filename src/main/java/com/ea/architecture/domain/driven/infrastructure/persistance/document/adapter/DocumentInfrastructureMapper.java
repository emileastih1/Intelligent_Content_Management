package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter;

import com.ea.architecture.domain.driven.domain.common.adapter.EntityMapperUtil;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.model.DocumentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring")
public interface DocumentInfrastructureMapper extends EntityMapperUtil {
    @Mapping(target = "documentType", source = "documentType", qualifiedByName = "mapDocumentTypeToString")
    @Mapping(target = "creationDate", source = "creationDate", qualifiedByName = "mapZonedDateTimeToString")
    @Mapping(target = "documentId", source = "id" , qualifiedByName = "mapUniqueIdToString")
    DocumentEntity domainToEntity(DocumentAggregate documentAggregate);
    @Mapping(target = "documentType", source = "documentType", qualifiedByName = "mapStringToDocumentType")
    @Mapping(target = "creationDate", source = "creationDate", qualifiedByName = "mapStringToZonedDateTime")
    @Mapping(target = "id", source = "documentId" , qualifiedByName = "mapStringToUniqueId")
    DocumentAggregate entityToDomain(DocumentEntity documentEntity);

}
