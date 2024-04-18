package org.powertac.orchestrator.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.orchestrator.user.domain.User;
import org.powertac.orchestrator.user.domain.UserRepository;
import org.powertac.orchestrator.user.exception.UserNotFoundException;
import org.powertac.orchestrator.user.UserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/auth")
public class AuthenticationRestController {

    private final AuthenticationManager authManager;
    private final JwtTokenService tokenFactory;
    private final UserRepository users;
    private final UserProvider userProvider;
    private final Logger logger;

    public AuthenticationRestController(AuthenticationManager authManager, JwtTokenService tokenFactory,
                                        UserRepository users, UserProvider userProvider) {
        this.authManager = authManager;
        this.tokenFactory = tokenFactory;
        this.users = users;
        this.userProvider = userProvider;
        logger = LogManager.getLogger(AuthenticationRestController.class);
    }

    @GetMapping("/")
    public ResponseEntity<AuthenticationStateDTO> isAuthenticated() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Boolean isAuthenticated = authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
            return ResponseEntity.ok().body(new AuthenticationStateDTO(isAuthenticated));
        } catch (Exception e) {
            logger.error("unable to determine authentication state", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/")
    public ResponseEntity<String> login(@RequestBody AuthenticationRequest request) {
        try {
            Authentication auth = authManager.authenticate(getAuthentication(request));
            User user = (User) auth.getPrincipal();
            Instant expirationDate = Instant.now().plus(1, ChronoUnit.DAYS);
            String token = tokenFactory.createAuthToken(user, expirationDate);
            user.setToken(token);
            user.setTokenExpirationDate(expirationDate);
            users.save(user);
            return ResponseEntity.ok(token);
        } catch (BadCredentialsException e) {
            logger.error("login attempt with wrong credentials", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            logger.error(e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @DeleteMapping("/")
    public ResponseEntity<?> logout() {
        try {
            User user = userProvider.getCurrentUser();
            if (null != user) {
                user.setToken(null);
                user.setTokenExpirationDate(null);
                users.save(user);
            }
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(401).build();
        }
    }

    private UsernamePasswordAuthenticationToken getAuthentication(AuthenticationRequest request) {
        return new UsernamePasswordAuthenticationToken(
            request.getUsername(),
            request.getPassword());
    }

}
