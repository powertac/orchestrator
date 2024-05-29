package org.powertac.orchestrator.server;

import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;

import java.util.Collection;

public interface SimulationServerVersionRepository extends CrudRepository<SimulationServerVersion, String> {

    @NonNull Collection<SimulationServerVersion> findAll();
    boolean existsByImageTag(String imageTag);
    boolean existsByName(String name);
    SimulationServerVersion findByName(String name);

}
