package org.powertac.orchestrator.jupyter;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.*;
import org.powertac.orchestrator.docker.ContainerCreator;
import org.powertac.orchestrator.docker.DockerContainer;
import org.powertac.orchestrator.user.UserProvider;
import org.powertac.orchestrator.user.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JupyterContainerCreator implements ContainerCreator<JupyterInstance> {

    @Value("${services.analysis.default-image}")
    private String defaultImageTag;

    private final DockerClient docker;
    private final UserProvider userProvider;
    private final JupyterBindFactory bindFactory;

    public JupyterContainerCreator(DockerClient docker, UserProvider userProvider, JupyterBindFactory bindFactory) {
        this.docker = docker;
        this.userProvider = userProvider;
        this.bindFactory = bindFactory;
    }

    @Override
    public DockerContainer createFor(JupyterInstance instance) {
        try {
            String name = getContainerName(instance);
            String id = docker.createContainerCmd(defaultImageTag)
                .withName(name)
                .withHostConfig(getHostConfig(instance))
                .withCmd(getCommand(instance))
                .exec().getId();
            return DockerContainer.builder()
                .id(id)
                .name(name)
                .owner(userProvider.getCurrentUser())
                .build();
        } catch (UserNotFoundException e) {
            throw new RuntimeException("could not determine owner of new jupyter container");
        }
    }

    private String getContainerName(JupyterInstance instance) {
        return String.format("analysis.%s", instance.getScope().getId());
    }

    private HostConfig getHostConfig(JupyterInstance instance) {
        return new HostConfig()
            .withBinds(getBinds(instance))
            .withPortBindings(
                new PortBinding(
                    new Ports.Binding("127.0.0.1", instance.getPort().toString()),
                    new ExposedPort(8888)));
    }

    private List<Bind> getBinds(JupyterInstance instance) {
        List<Bind> binds = new ArrayList<>();
        binds.add(bindFactory.createScopeFileBind(instance.getScope()));
        binds.addAll(bindFactory.createGameArtifactsBinds(instance.getScope().getGames()));
        return binds;
    }

    private List<String> getCommand(JupyterInstance instance) {
        List<String> command = new ArrayList<>();
        command.add("start-notebook.py");
        command.add("--IdentityProvider.token=" + instance.getToken());
        command.add("--ServerApp.terminals_enabled=False");
        return command;
    }

}
