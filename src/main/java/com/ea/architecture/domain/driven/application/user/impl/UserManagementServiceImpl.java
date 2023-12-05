package com.ea.architecture.domain.driven.application.user.impl;

import com.ea.architecture.domain.driven.application.mapper.ApplicationMapper;
import com.ea.architecture.domain.driven.application.user.port.UserManagementService;
import com.ea.architecture.domain.driven.application.user.dto.UserDto;
import com.ea.architecture.domain.driven.domain.user.port.UserDomainService;
import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * This class represents the application Service
 * It should remain as thin as possible and should not contain any domain logic
 * It should handle transactions, authorization and route calls down to the domain layer
 */
@Transactional
@Service
public class UserManagementServiceImpl implements UserManagementService {

    UserDomainService userDomainService;
    ApplicationMapper applicationMapper;

    public UserManagementServiceImpl(UserDomainService userDomainService, ApplicationMapper applicationMapper) {
        this.userDomainService = userDomainService;
        this.applicationMapper = applicationMapper;
    }

    @Override
    public UserAggregate findUser(UserDto userDTO) {
        return userDomainService.findUser(applicationMapper.dtoToDomain(userDTO));
    }
}
