package com.ea.architecture.domain.driven.domain.document.mapper;

import com.ea.architecture.domain.driven.domain.common.adapter.EntityMapperUtil;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.model.DocumentIndexCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentIndexMapper extends EntityMapperUtil {
    DocumentIndexCommand domainToCommand(DocumentAggregate documentAggregate);
}
