package org.powertac.orchestrator.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.powertac.orchestrator.file.DownloadToken;
import org.powertac.orchestrator.file.DownloadTokenRepository;
import org.powertac.orchestrator.user.domain.RegistrationToken;
import org.powertac.orchestrator.user.domain.RegistrationTokenRepository;
import org.powertac.orchestrator.user.domain.User;
import org.powertac.orchestrator.user.domain.UserRepository;
import org.powertac.orchestrator.user.exception.InvalidRegistrationTokenException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class JwtTokenServiceImpl implements JwtTokenService {

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final UserRepository users;
    private final RegistrationTokenRepository registrationTokens;
    private final DownloadTokenRepository downloadTokens;

    public JwtTokenServiceImpl(Algorithm algorithm, JWTVerifier verifier, UserRepository users,
                               RegistrationTokenRepository registrationTokens,
                               DownloadTokenRepository downloadTokens) {
        this.algorithm = algorithm;
        this.verifier = verifier;
        this.users = users;
        this.registrationTokens = registrationTokens;
        this.downloadTokens = downloadTokens;
    }

    @Override
    public String createAuthToken(User user, Instant expirationDate) {
        return JWT.create()
            .withIssuer("powertac")
            .withClaim("username", user.getUsername())
            .withClaim("expiration", expirationDate.getEpochSecond())
            .sign(algorithm);
    }

    @Override
    public String createRegistrationToken(Instant expirationDate) {
        return JWT.create()
            .withIssuer("powertac")
            .withClaim("expiration", expirationDate.getEpochSecond())
            .sign(algorithm);
    }

    @Override
    public String createDownloadToken(User user, String filePath) {
        return JWT.create()
            .withIssuer("powertac")
            .withClaim("username", user.getUsername())
            .withClaim("filepath", filePath)
            .sign(algorithm);
    }

    @Override
    public User getVerifiedUser(String token) throws JWTVerificationException, TokenVerificationException {
        DecodedJWT jwt = verifier.verify(token);
        User user = users.findByToken(token).orElse(null);
        if (null == user) {
            throw new NoTokenHolderException(String.format("no holder for token '%s' found", token));
        }
        verifyUsernameClaim(jwt, user.getUsername());
        verifyExpirationDateClaim(jwt, user.getTokenExpirationDate());
        verifyExpirationDate(user.getTokenExpirationDate());
        return user;
    }

    @Override
    public RegistrationToken getVerifiedRegistrationToken(String token) throws InvalidRegistrationTokenException {
        DecodedJWT jwt = verifier.verify(token);
        Optional<RegistrationToken> registrationToken = registrationTokens.findByToken(token);
        if (registrationToken.isPresent()) {
            try {
                verifyExpirationDateClaim(jwt, registrationToken.get().getExpirationDate());
                verifyExpirationDate(registrationToken.get().getExpirationDate());
                if (registrationToken.get().getClaimedBy() != null) {
                    throw new TokenVerificationException("registration token has already been claimed");
                }
                return registrationToken.get();
            } catch (JWTVerificationException|TokenVerificationException e) {
                throw new InvalidRegistrationTokenException("invalid registration token", e);
            }
        } else {
            throw new InvalidRegistrationTokenException(String.format("no registration token found for token '%s'", token));
        }
    }

    @Override
    public DownloadToken getVerifiedDownloadToken(String token) throws TokenVerificationException {
        DecodedJWT jwt = verifier.verify(token);
        Optional<DownloadToken> download = downloadTokens.findByToken(token);
        if (download.isPresent()) {
            if (!jwt.getClaim("username").asString().equals(download.get().getUser().getUsername())) {
                throw new TokenVerificationException("download token user doesn't match actual user; token=" + token);
            }
            if (!jwt.getClaim("filepath").asString().equals(download.get().getFilePath())) {
                throw new TokenVerificationException("download token file path doesn't match actual file path; token=" + token);
            }
            return download.get();
        } else {
            throw new TokenVerificationException("download token " + token + "does not exist");
        }
    }

    private void verifyUsernameClaim(DecodedJWT jwt, String username) throws ClaimMismatchException {
        String claimedUsername = jwt.getClaim("username").asString();
        if (!claimedUsername.equals(username)) {
            throw new ClaimMismatchException(
                String.format("claimed username '%s' does not match token holder's username '%s'",
                    claimedUsername,
                    username));
        }
    }

    private void verifyExpirationDateClaim(DecodedJWT jwt, Instant expirationDate) throws ClaimMismatchException {
        Instant claimedExpirationDate = Instant.ofEpochSecond(jwt.getClaim("expiration").asLong());
        // "cast" saved expiration date to resolution of seconds to avoid inconsistencies (persistence layer(s)
        // might or might not save milli-/nanoseconds)
        Instant expirationDateWithoutNanos = Instant.ofEpochSecond(expirationDate.getEpochSecond());
        if (!claimedExpirationDate.equals(expirationDateWithoutNanos)) {
            throw new ClaimMismatchException(
                String.format("claimed expiration date '%s' does not match token holder's expiration date '%s'",
                    claimedExpirationDate,
                    expirationDateWithoutNanos));
        }
    }

    private void verifyExpirationDate(Instant expirationDate) throws ExpiredTokenException {
        if (Instant.now().isAfter(expirationDate)) {
            throw new ExpiredTokenException("token has expired");
        }
    }

}
