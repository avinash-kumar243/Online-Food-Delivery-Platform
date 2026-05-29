package com.quickbite.menu.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

public class RoleClaimJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter delegate = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>(delegate.convert(jwt));
        Stream.of("roles", "role", "authorities")
            .map(jwt::getClaim)
            .filter(Objects::nonNull)
            .flatMap(this::streamValues)
            .map(this::normalizeRole)
            .distinct()
            .map(SimpleGrantedAuthority::new)
            .forEach(authorities::add);

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    @SuppressWarnings("unchecked")
    private Stream<String> streamValues(Object claimValue) {
        if (claimValue instanceof String value) {
            return Stream.of(value.split(","));
        }
        if (claimValue instanceof Collection<?> values) {
            return values.stream().map(String::valueOf);
        }
        return Stream.empty();
    }

    private String normalizeRole(String role) {
        String normalized = role.trim().toUpperCase().replace('-', '_');
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
