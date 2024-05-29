package org.powertac.orchestrator.jupyter;

import lombok.Getter;
import lombok.Setter;
import org.powertac.orchestrator.analysis.scope.Scope;
import org.powertac.orchestrator.docker.DockerContainer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Getter
public class JupyterInstance {

    private final Scope scope;
    private final String host;
    private final Integer port;
    private final String token;

    @Setter
    private DockerContainer container;

    public JupyterInstance(Scope scope, String host, Integer port, String token) {
        this.scope = scope;
        this.host = host;
        this.port = port;
        this.token = token;
    }

    public String getId() {
        return scope.getId();
    }

    public boolean isRunning() {
        return container != null && container.isRunning();
    }

    public String getUri() {
        return String.format("http://%s:%s/lab", getHost(), getPort());
    }

    public boolean isReachable() {
        try {
            ResponseEntity<?> response = WebClient.create().get().uri(getUri())
                .retrieve().toBodilessEntity()
                .block(Duration.of(10, ChronoUnit.SECONDS));
            return response != null && !response.getStatusCode().isError();
        } catch (WebClientRequestException e) {
            return false;
        }
    }

}
