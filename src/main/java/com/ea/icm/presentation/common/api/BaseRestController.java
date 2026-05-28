package com.ea.icm.presentation.common.api;

import com.ea.icm.application.config.security.RestSecurityConfiguration;
import com.ea.icm.presentation.exception.ApiResponseUnauthorized;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@RequestMapping(RestSecurityConfiguration.URL_BASE_PATH)
@ApiResponseUnauthorized
public abstract class BaseRestController {
    protected String getConnectedUser() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .orElse("anonymous");
    }
}
