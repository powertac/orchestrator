package org.powertac.orchestrator.docker;

import org.powertac.orchestrator.docker.exception.ContainerConflictException;

import java.util.Collection;
import java.util.Optional;

public interface DockerContainerRepository {

    DockerContainer add(DockerContainer container) throws ContainerConflictException;

    Optional<DockerContainer> findById(String id);

    Optional<DockerContainer> findByName(String name);

    Collection<DockerContainer> findAll();

    boolean existsById(String id);

    boolean existsByName(String name);

    DockerContainer update(DockerContainer container);

    void remove(DockerContainer container);

    DockerContainer run(DockerContainer container);

}
