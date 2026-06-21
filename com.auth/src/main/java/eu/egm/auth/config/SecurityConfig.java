package eu.egm.auth.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/openapi/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/authz/check").permitAll()
                        .requestMatchers("/api/auth/admin/**").hasRole("auth_admin")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakAuthoritiesConverter());
        return converter;
    }

    /**
     * Keycloak stores realm and client roles in nested token claims. The gateway
     * and service code use normal Spring authorities, so roles are flattened to
     * ROLE_* and OAuth scopes remain SCOPE_*.
     */
    static class KeycloakAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        private final JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Set<GrantedAuthority> authorities = new HashSet<>(scopes.convert(jwt));
            authorities.addAll(extractRealmRoles(jwt).stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toSet()));
            authorities.addAll(extractClientRoles(jwt).stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toSet()));
            return authorities;
        }

        private Set<String> extractRealmRoles(Jwt jwt) {
            Object realmAccess = jwt.getClaims().get("realm_access");
            if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
                return Set.of();
            }
            return extractRoles(realmAccessMap.get("roles"));
        }

        private Set<String> extractClientRoles(Jwt jwt) {
            Object resourceAccess = jwt.getClaims().get("resource_access");
            if (!(resourceAccess instanceof Map<?, ?> resourceAccessMap)) {
                return Set.of();
            }
            Set<String> roles = new HashSet<>();
            for (Object clientAccess : resourceAccessMap.values()) {
                if (clientAccess instanceof Map<?, ?> clientAccessMap) {
                    roles.addAll(extractRoles(clientAccessMap.get("roles")));
                }
            }
            return roles;
        }

        private Set<String> extractRoles(Object roles) {
            if (!(roles instanceof Collection<?> values)) {
                return Set.of();
            }
            return values.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toSet());
        }
    }
}
