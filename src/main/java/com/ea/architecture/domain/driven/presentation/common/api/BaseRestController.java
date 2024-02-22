package com.ea.architecture.domain.driven.presentation.common.api;

import com.ea.architecture.domain.driven.application.config.security.RestSecurityConfiguration;
import com.ea.architecture.domain.driven.presentation.exception.ApiResponseUnauthorized;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(RestSecurityConfiguration.URL_BASE_PATH)
@ApiResponseUnauthorized
public abstract class BaseRestController {
    protected String getConnectedUser(){
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return  authentication == null ? "anonymous" : authentication.getName();
    }
}
