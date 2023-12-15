package com.ea.architecture.domain.driven.presentation.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Profile("BasicAuth")
@Configuration
public class BasicAuthSecurity {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authz) -> authz
                        .anyRequest().authenticated()
                )
                .httpBasic(withDefaults());
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/ignore1", "/ignore2");
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Load users and passwords from application.yml
        InMemoryUserDetailsManager userDetailsManager = new InMemoryUserDetailsManager();

        // Add users with their roles and encoded passwords
        userDetailsManager.createUser(
                User.withUsername("UserRead").password("{noop}password1").roles("demo-api:READ").build());
        userDetailsManager.createUser(
                User.withUsername("UserWrite").password("{noop}password2").roles("demo-api:WRITE").build());
        return userDetailsManager;
    }

}
