package org.powertac.orchestrator.server;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import org.powertac.orchestrator.docker.DockerContainer;
import org.powertac.orchestrator.game.Game;
import org.powertac.orchestrator.paths.PathProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BootstrapContainerCreatorImpl implements BootstrapContainerCreator {

    @Value("${services.simulationserver.default-image}")
    private String defaultImageTag;

    private final DockerClient docker;
    private final ServerContainerCommandCreator commandCreator;
    private final ServerContainerBindFactory bindFactory;
    private final PathProvider paths;

    @Autowired
    public BootstrapContainerCreatorImpl(DockerClient docker, ServerContainerCommandCreator commandCreator, ServerContainerBindFactory bindFactory, PathProvider paths) {
        this.docker = docker;
        this.commandCreator = commandCreator;
        this.bindFactory = bindFactory;
        this.paths = paths;
    }

    @Override
    public DockerContainer create(Game game, String networkId) throws DockerException {
        CreateContainerCmd create = docker.createContainerCmd(defaultImageTag);
        String name = getBootstrapContainerName(game);
        create.withName(name);
        create.withCmd(getCommand(game));
        create.withHostConfig(getHostConfig(game, networkId));
        CreateContainerResponse response = create.exec();
        return new DockerContainer(response.getId(), name);
    }

    @Override
    public String getBootstrapContainerName(Game game) {
        return String.format("boot.%s", game.getId());
    }

    private List<String> getCommand(Game game) {
        return commandCreator.createBootstrapCommand(
            paths.container().server().game(game).properties().toString(),
            paths.container().server().game(game).bootstrap().toString());
    }

    private HostConfig getHostConfig(Game game, String networkId) {
        HostConfig config = new HostConfig();
        config.withBinds(getBinds(game));
        config.withNetworkMode(networkId);
        return config;
    }

    private List<Bind> getBinds(Game game) {
        List<Bind> binds = new ArrayList<>();
        binds.add(bindFactory.createSimulationPropertiesBind(game));
        binds.add(bindFactory.createBootstrapBind(game));
        return binds;
    }

}
