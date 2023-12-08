package com.ea.architecture.domain.driven.domain.user.repository;

import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;
import org.springframework.stereotype.Service;

/**
 * This class represents the second port connected to the infrastructure layer
 */
@Service
public interface UserDomainServicePort {

    UserAggregate findUser(UserAggregate userAggregate);
}
