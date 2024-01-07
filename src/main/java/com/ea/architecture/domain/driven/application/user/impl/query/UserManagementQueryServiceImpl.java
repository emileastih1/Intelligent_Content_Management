package com.ea.architecture.domain.driven.application.user.impl.query;

import com.ea.architecture.domain.driven.application.mapper.ApplicationMapper;
import com.ea.architecture.domain.driven.application.user.port.query.UserManagementQueryService;
import com.ea.architecture.domain.driven.domain.user.repository.UserDomainQueryServicePort;
import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;
import org.springframework.stereotype.Service;

/**
 * This class represents the application Service
 * It should remain as thin as possible and should not contain any domain logic
 * It should handle transactions, authorizations and route calls down to the domain layer
 */
@Service
public class UserManagementQueryServiceImpl implements UserManagementQueryService {

    UserDomainQueryServicePort userDomainQueryServicePort;
    ApplicationMapper applicationMapper;

    public UserManagementQueryServiceImpl(UserDomainQueryServicePort userDomainQueryServicePort, ApplicationMapper applicationMapper) {
        this.userDomainQueryServicePort = userDomainQueryServicePort;
        this.applicationMapper = applicationMapper;
    }

    @Override
    public UserAggregate getUserByFilter(UserAggregate userAggregate) throws Exception {
        return userDomainQueryServicePort.findUserByFilter(userAggregate);
    }
}
