package com.ea.architecture.domain.driven.presentation.user.mapper;

import com.ea.architecture.domain.driven.application.user.dto.UserDto;
import com.ea.architecture.domain.driven.domain.common.adapter.EntityMapperUtil;
import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public interface UserPresentationMapper extends EntityMapperUtil{
    @Mapping(target = "id", source = "id.id")
    UserDto domainToDto(UserAggregate userAggregate);
    @Mapping(target = "id.id", source = "id")
    UserAggregate dtoToDomain(UserDto userDto);
}
