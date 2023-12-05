package com.ea.architecture.domain.driven.presentation.user.mapper;

import com.ea.architecture.domain.driven.application.user.dto.UserDto;
import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper
public interface UserPresentationMapper {
    UserDto domainToDto(UserAggregate userAggregate);
}
