package com.ea.icm.presentation.common.api;

import com.ea.icm.application.config.security.RestSecurityConfiguration;
import com.ea.icm.presentation.exception.ApiResponseUnauthorized;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@RequestMapping(RestSecurityConfiguration.URL_BASE_PATH)
@ApiResponseUnauthorized
public abstract class BaseRestController {
    protected String getConnectedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            Object preferredUsername = jwtToken.getToken().getClaim("preferred_username");
            if (preferredUsername != null) {
                return preferredUsername.toString();
            }
        }
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .orElse("anonymous");
    }
}
