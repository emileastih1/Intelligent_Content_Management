package com.ea.architecture.domain.driven.presentation;

import com.ea.architecture.domain.driven.application.config.security.RestSecurityConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(RestSecurityConfiguration.URL_BASE_PATH)
public abstract class BaseRestController {
    protected String getConnectedUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return  authentication == null ? "anonymous" : authentication.getName();
    }
}
