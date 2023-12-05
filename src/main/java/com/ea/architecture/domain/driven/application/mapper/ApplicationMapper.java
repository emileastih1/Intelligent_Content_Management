package com.ea.architecture.domain.driven.application.mapper;

import com.ea.architecture.domain.driven.application.user.dto.UserDto;
import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;
import org.mapstruct.Mapper;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.stereotype.Component;

@Component
@Mapper
public interface ApplicationMapper {
    <T extends AbstractAggregateRoot<T>> T dtoToDomain(Record dto);
}
