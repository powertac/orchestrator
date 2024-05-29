package org.powertac.orchestrator.server;

import org.powertac.orchestrator.persistence.Seeder;
import org.powertac.orchestrator.persistence.SeederException;
import org.powertac.orchestrator.util.ID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimulationServerVersionSeeder implements Seeder {

    @Value("${services.simulationserver.default-image}")
    private String defaultImageTag;

    private final SimulationServerVersionRepository versionRepository;

    public SimulationServerVersionSeeder(SimulationServerVersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    @Override
    public void seed() throws SeederException {
        if (!versionRepository.existsByImageTag(defaultImageTag) && !versionRepository.existsByName("default")) {
            SimulationServerVersion defaultVersion = new SimulationServerVersion(ID.gen(), "default", defaultImageTag);
            versionRepository.save(defaultVersion);
        }
    }

}
