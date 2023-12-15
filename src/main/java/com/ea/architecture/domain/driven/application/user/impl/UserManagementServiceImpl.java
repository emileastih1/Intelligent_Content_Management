package com.ea.architecture.domain.driven.application.user.impl;

import com.ea.architecture.domain.driven.application.mapper.ApplicationMapper;
import com.ea.architecture.domain.driven.application.user.port.UserManagementService;
import com.ea.architecture.domain.driven.domain.user.repository.UserDomainServicePort;
import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * This class represents the application Service
 * It should remain as thin as possible and should not contain any domain logic
 * It should handle transactions, authorizations and route calls down to the domain layer
 */
@Transactional
@Service
public class UserManagementServiceImpl implements UserManagementService {

    UserDomainServicePort userDomainServicePort;
    ApplicationMapper applicationMapper;

    public UserManagementServiceImpl(UserDomainServicePort userDomainServicePort, ApplicationMapper applicationMapper) {
        this.userDomainServicePort = userDomainServicePort;
        this.applicationMapper = applicationMapper;
    }

    @Override
    public UserAggregate getUserByFilter(UserAggregate userAggregate) throws Exception {
        return userDomainServicePort.findUserByFilter(userAggregate);
    }
}
