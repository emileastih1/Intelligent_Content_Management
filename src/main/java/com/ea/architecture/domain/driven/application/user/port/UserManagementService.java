package com.ea.architecture.domain.driven.application.user.port;

import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;
import org.springframework.stereotype.Service;

/**
 * This class represents the first port connected to the presentation layer
 */
@Service
public interface UserManagementService {

    UserAggregate getUserByFilter(UserAggregate userAggregate) throws Exception;
}
