package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter;

import com.ea.architecture.domain.driven.domain.common.model.UniqueId;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.vo.DocumentTypes;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.model.DocumentEntity;
import com.ea.architecture.domain.driven.domain.common.adapter.EntityMapperUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentInfrastructureMapper extends EntityMapperUtil {
    @Mapping(target = "documentId", source = "id", qualifiedByName = "mapUniqueIdToLong")
    DocumentEntity domainToEntity(DocumentAggregate documentAggregate);
    @Mapping(target = "id", source = "documentId", qualifiedByName = "mapLongToUniqueId")
    DocumentAggregate entityToDomain(DocumentEntity documentEntity);
    default String mapDocumentType(DocumentTypes documentTypes) {
        return documentTypes != null ? documentTypes.name() : DocumentTypes.UNDEFINED.name();
    }
    default DocumentTypes mapDocumentType(String documentType) {
        return documentType != null ? DocumentTypes.valueOf(documentType) : DocumentTypes.UNDEFINED;
    }

}
