package org.powertac.orchestrator.server;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SimulationServerVersion {

    @Id private String id;
    private String name;
    private String imageTag;

}

