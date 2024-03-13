package org.powertac.orchestrator.jupyter;

import lombok.Getter;
import lombok.Setter;
import org.powertac.orchestrator.analysis.scope.Scope;
import org.powertac.orchestrator.docker.DockerContainer;

@Getter
public class JupyterInstance {

    private final Scope scope;
    private final Integer port;
    private final String token;

    @Getter
    @Setter
    private DockerContainer container;

    public JupyterInstance(Scope scope, Integer port, String token) {
        this.scope = scope;
        this.port = port;
        this.token = token;
    }

    public String getId() {
        return scope.getId();
    }

    public boolean isRunning() {
        return container != null && container.isRunning();
    }

}
