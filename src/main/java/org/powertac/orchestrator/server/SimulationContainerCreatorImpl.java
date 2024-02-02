package org.powertac.orchestrator.server;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.InternetProtocol;
import org.powertac.orchestrator.broker.Broker;
import org.powertac.orchestrator.docker.DockerContainer;
import org.powertac.orchestrator.docker.DockerNetwork;
import org.powertac.orchestrator.game.Game;
import org.powertac.orchestrator.game.GameRun;
import org.powertac.orchestrator.paths.PathProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SimulationContainerCreatorImpl implements SimulationContainerCreator {

    private final static String serverAlias = "powertac-server";
    private final static int defaultMessageBrokerPort = 61616;

    @Value("${services.simulationserver.default-image}")
    private String defaultImageTag;

    private final DockerClient docker;
    private final ServerContainerCommandCreator commandCreator;
    private final ServerContainerBindFactory bindFactory;
    private final PathProvider paths;

    @Autowired
    public SimulationContainerCreatorImpl(DockerClient docker, ServerContainerCommandCreator commandCreator, ServerContainerBindFactory bindFactory, PathProvider paths) {
        this.docker = docker;
        this.commandCreator = commandCreator;
        this.bindFactory = bindFactory;
        this.paths = paths;
    }

    @Override
    public DockerContainer create(GameRun run, DockerNetwork network) throws DockerException {
        CreateContainerCmd create = docker.createContainerCmd(defaultImageTag);
        String name = getSimulationContainerName(run.getGame());
        create.withName(name)
            .withCmd(getCommand(run.getGame()))
            .withExposedPorts(new ExposedPort(defaultMessageBrokerPort, InternetProtocol.TCP))
            .withAliases(serverAlias)
            .withHostConfig(getHostConfig(run, network));
        CreateContainerResponse response = create.exec();
        return new DockerContainer(response.getId(), name);
    }

    @Override
    public String getSimulationContainerName(Game game) {
        return String.format("sim.%s", game.getId());
    }

    private List<String> getCommand(Game game) {
        Set<String> brokerNames = game.getBrokers().stream()
            .map(Broker::getHumanReadableIdentifier)
            .collect(Collectors.toSet());
        Path seedFilePath = useSeed(game) ? paths.container().server().game(game).seed() : null;
        return commandCreator.createSimulationCommand(
            paths.container().server().game(game).properties().toString(),
            paths.container().server().game(game).bootstrap().toString(),
            seedFilePath != null ? seedFilePath.toString() : null,
            brokerNames);
    }

    private HostConfig getHostConfig(GameRun run, DockerNetwork network) {
        HostConfig config = new HostConfig();
        config.withBinds(getBinds(run));
        config.withNetworkMode(network.getId());
        return config;
    }

    private List<Bind> getBinds(GameRun run) {
        List<Bind> binds = new ArrayList<>();
        binds.add(bindFactory.createSimulationPropertiesBind(run.getGame()));
        binds.add(bindFactory.createBootstrapBind(run.getGame()));
        binds.add(bindFactory.createStateLogBind(run));
        binds.add(bindFactory.createTraceLogBind(run));
        if (useSeed(run.getGame())) {
            binds.add(bindFactory.createSeedBind(run.getGame()));
        }
        return binds;
    }

    private boolean useSeed(Game game) {
        return null != game.getSeed();
    }

}
