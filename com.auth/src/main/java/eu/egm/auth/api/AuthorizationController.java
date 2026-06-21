package eu.egm.auth.api;

import eu.egm.auth.model.AuthorizationCheckRequest;
import eu.egm.auth.model.AuthorizationCheckResponse;
import eu.egm.auth.model.UserProfile;
import eu.egm.auth.service.AuthorizationService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @PostMapping("/authz/check")
    public AuthorizationCheckResponse check(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody AuthorizationCheckRequest request) {
        return authorizationService.check(authorization, request);
    }

    @GetMapping("/auth/me")
    public UserProfile me(@AuthenticationPrincipal Jwt jwt) {
        return authorizationService.profile(jwt);
    }
}
