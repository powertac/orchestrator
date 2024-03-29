package org.powertac.orchestrator.user.domain;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RegistrationTokenRepository extends CrudRepository<RegistrationToken, Long> {

    Optional<RegistrationToken> findByToken(String token);

}
