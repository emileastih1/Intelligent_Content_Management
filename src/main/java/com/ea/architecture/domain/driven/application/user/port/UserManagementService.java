package com.ea.architecture.domain.driven.application.user.port;

import com.ea.architecture.domain.driven.application.user.dto.UserDto;
import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;

public interface UserManagementService {

    UserAggregate findUser(UserDto userDTO);
}
