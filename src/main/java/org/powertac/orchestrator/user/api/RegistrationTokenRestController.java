package org.powertac.orchestrator.user.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.orchestrator.security.JwtTokenService;
import org.powertac.orchestrator.user.exception.InvalidRegistrationTokenException;
import org.powertac.orchestrator.user.exception.UserNotFoundException;
import org.powertac.orchestrator.user.UserProvider;
import org.powertac.orchestrator.user.domain.RegistrationToken;
import org.powertac.orchestrator.user.domain.RegistrationTokenRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/registrations")
public class RegistrationTokenRestController {

    private final static Duration expirationPeriod = Duration.ofDays(7);

    private final JwtTokenService tokenService;
    private final UserProvider userProvider;
    private final RegistrationTokenRepository registrationTokens;
    private final Logger logger;

    public RegistrationTokenRestController(JwtTokenService tokenService, UserProvider userProvider,
                                           RegistrationTokenRepository registrationTokens) {
        this.tokenService = tokenService;
        this.userProvider = userProvider;
        this.registrationTokens = registrationTokens;
        logger = LogManager.getLogger(RegistrationTokenRestController.class);
    }

    @PostMapping("/")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<RegistrationToken> createRegistrationToken() {
        try {
            Instant expiration = Instant.now().plus(expirationPeriod);
            String tokenString = tokenService.createRegistrationToken(expiration);
            RegistrationToken token = RegistrationToken.builder()
                .token(tokenString)
                .issuedBy(userProvider.getCurrentUser())
                .issuedAt(Instant.now())
                .expirationDate(expiration)
                .build();
            token = registrationTokens.save(token);
            // FIXME : ID is not automatically updated on this object
            return ResponseEntity.ok(token);
        } catch (UserNotFoundException e) {
            logger.error("no valid issuer found", e);
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            logger.error("unable to create new registration token", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // FIXME : this should be a POST Request to prevent man-in-the-middle-attacks (?)
    @GetMapping("/{tokenString}")
    public ResponseEntity<?> verifyRegistrationToken(@PathVariable String tokenString) {
        try {
            tokenService.getVerifiedRegistrationToken(tokenString);
            return ResponseEntity.noContent().build();
        } catch (InvalidRegistrationTokenException e) {
            logger.error(e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Iterable<RegistrationToken>> getRegistrations() {
        Iterable<RegistrationToken> tokens = registrationTokens.findAll();
        return ResponseEntity.ok(tokens);
    }

}
