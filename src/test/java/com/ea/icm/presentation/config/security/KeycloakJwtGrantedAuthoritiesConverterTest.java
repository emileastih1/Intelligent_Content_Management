package com.ea.icm.presentation.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtGrantedAuthoritiesConverterTest {

    private final KeycloakJwtGrantedAuthoritiesConverter converter =
            new KeycloakJwtGrantedAuthoritiesConverter();

    private Jwt buildJwt(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "RS256");
        claims.forEach(builder::claim);
        return builder.build();
    }

    @Test
    void givenJwtWithReadAndWriteRoles_whenConvert_thenReturnsBothAuthorities() {
        Map<String, Object> clientRoles = Map.of("roles", List.of("READ", "WRITE"));
        Map<String, Object> resourceAccess = Map.of("intelligent-content-management-api", clientRoles);
        Jwt jwt = buildJwt(Map.of("resource_access", resourceAccess));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_READ", "ROLE_WRITE");
    }

    @Test
    void givenJwtWithMissingResourceAccessClaim_whenConvert_thenReturnsEmpty() {
        Jwt jwt = buildJwt(Map.of("sub", "user123"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void givenJwtWithWrongClientId_whenConvert_thenReturnsEmpty() {
        Map<String, Object> clientRoles = Map.of("roles", List.of("READ"));
        Map<String, Object> resourceAccess = Map.of("other-client", clientRoles);
        Jwt jwt = buildJwt(Map.of("resource_access", resourceAccess));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void givenJwtWithEmptyRolesArray_whenConvert_thenReturnsEmpty() {
        Map<String, Object> clientRoles = Map.of("roles", List.of());
        Map<String, Object> resourceAccess = Map.of("intelligent-content-management-api", clientRoles);
        Jwt jwt = buildJwt(Map.of("resource_access", resourceAccess));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }
}
