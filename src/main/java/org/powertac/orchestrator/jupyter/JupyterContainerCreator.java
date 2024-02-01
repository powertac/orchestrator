package org.powertac.orchestrator.jupyter;

import org.apache.commons.lang3.NotImplementedException;
import org.powertac.orchestrator.docker.ContainerCreator;
import org.powertac.orchestrator.docker.DockerContainer;

public class JupyterContainerCreator implements ContainerCreator<JupyterConfig> {

    @Override
    public DockerContainer createFor(JupyterConfig entity) {
        throw new NotImplementedException("Jupyter containers are not yet implemented");
    }

}
