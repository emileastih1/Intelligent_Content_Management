package com.ea.architecture.domain.driven.application.user.impl;

import com.ea.architecture.domain.driven.application.user.UserManagementService;
import com.ea.architecture.domain.driven.application.user.dto.UserDto;
import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;

/**
 * This class represents the application Service
 */
public class UserManagementServiceImpl implements UserManagementService {
    @Override
    public UserAggregate findUser(UserDto userDTO) {
        return null;
    }
}
