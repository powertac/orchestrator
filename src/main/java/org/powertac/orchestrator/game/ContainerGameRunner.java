package org.powertac.orchestrator.game;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.orchestrator.application.DeploymentContext;
import org.powertac.orchestrator.broker.Broker;
import org.powertac.orchestrator.broker.BrokerContainerCreator;
import org.powertac.orchestrator.docker.*;
import org.powertac.orchestrator.docker.exception.ContainerException;
import org.powertac.orchestrator.server.BootstrapContainerCreator;
import org.powertac.orchestrator.server.SimulationContainerCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ContainerGameRunner implements GameRunner {

    @Value("${application.deployment.context}")
    private DeploymentContext deploymentContext;

    @Value("${services.weatherserver.default-container-name}")
    private String weatherServerContainerName;

    private final GameRunRepository runs;
    private final GameFileManager gameFileManager;
    private final BootstrapContainerCreator bootstrapContainerCreator;
    private final SimulationContainerCreator simulationContainerCreator;
    private final BrokerContainerCreator brokerContainerCreator;
    private final DockerContainerController controller;
    private final DockerNetworkRepository networks;
    private final GameRunLifecycleManager lifecycle;
    private final GameValidator gameValidator;
    private final GamePostConditionValidator postConditionValidator;
    private final DockerClient client;

    private final Map<Game, GameRun> activeRuns;
    private final Logger logger;

    @Autowired
    public ContainerGameRunner(GameRunRepository runs, GameFileManager gameFileManager,
                               BootstrapContainerCreator bootstrapContainerCreator,
                               SimulationContainerCreator simulationContainerCreator,
                               BrokerContainerCreator brokerContainerCreator,
                               DockerContainerController controller, DockerNetworkRepository networks,
                               GameRunLifecycleManager lifecycle, GameValidator gameValidator,
                               GamePostConditionValidator postConditionValidator, DockerClient client) {
        this.runs = runs;
        this.gameFileManager = gameFileManager;
        this.bootstrapContainerCreator = bootstrapContainerCreator;
        this.simulationContainerCreator = simulationContainerCreator;
        this.brokerContainerCreator = brokerContainerCreator;
        this.controller = controller;
        this.networks = networks;
        this.lifecycle = lifecycle;
        this.gameValidator = gameValidator;
        this.postConditionValidator = postConditionValidator;
        this.client = client;
        this.activeRuns = new ConcurrentHashMap<>();
        logger = LogManager.getLogger(ContainerGameRunner.class);
    }

    @Override
    public void run(Game game) {
        logger.info(String.format("running game[id=%s]", game.getId()));
        GameRun run = runs.create(game);
        activeRuns.put(game, run);
        try {
            prepare(run);
            bootstrap(run);
            simulate(run);
            lifecycle.done(run);
            logger.info(String.format("run for game[id=%s] completed successfully", game.getId()));
        } catch (Exception e) {
            lifecycle.fail(run, e);
            logger.error(String.format("run for game[id=%s] failed", game.getId()), e);
        } finally {
            activeRuns.remove(game);
        }
    }

    @Override
    public Collection<Game> getRunningGames() {
        return activeRuns.keySet();
    }

    @PreDestroy
    public void shutdown() {
        for (GameRun run : activeRuns.values()) {
            stop(run);
            activeRuns.remove(run.getGame());
        }
    }

    private void prepare(GameRun run) throws GameValidationException {
        try {
            removeExistingDockerResources(run.getGame());
            gameFileManager.createRunScaffold(run);
            lifecycle.preparation(run);
            gameValidator.validate(run.getGame());
            DockerNetwork network = networks.createNetwork(getNetworkName(run.getGame()));
            run.setNetwork(network);
            if (deploymentContext.equals(DeploymentContext.CONTAINER)) {
                connectWeatherServer(network.getId());
            }
            gameFileManager.createRunScaffold(run);
        } catch (IOException e) {
            throw new GameValidationException("could not create game file", e);
        }
    }

    private void bootstrap(GameRun run) throws GameRunException {
        if (run.shouldBootstrap()) {
            try {
                gameFileManager.createServerProperties(run.getGame());
                gameFileManager.createBootstrap(run.getGame());
                DockerContainer bootstrapContainer = bootstrapContainerCreator.create(run.getGame(), run.getNetwork().getId());
                lifecycle.bootstrap(run, bootstrapContainer);
                DockerContainerExitState exitState = controller.run(run.getBootstrapContainer());
                if (exitState.isErrorState()) {
                    gameFileManager.removeBootstrap(run.getGame());
                    throw new GameRunException("failed to create bootstrap for game with id=" + run.getGame().getId());
                }
            } catch (IOException| ContainerException| DockerException e) {
                try {
                    gameFileManager.removeBootstrap(run.getGame());
                } catch (IOException f) {
                    logger.error("unable to remove failed bootstrap file for game with id=" + run.getGame().getId());
                }
                throw new GameRunException("failed to create bootstrap for game with id=" + run.getGame().getId(), e);
            }
        }
        lifecycle.ready(run);
    }

    private void simulate(GameRun run) throws GameRunException {
        try {
            gameFileManager.createSimulationScaffold(run);
            gameFileManager.createServerProperties(run.getGame());
            for (Broker broker : run.getGame().getBrokers()) {
                gameFileManager.createBrokerProperties(run.getGame(), broker);
            }
            DockerContainer serverContainer = simulationContainerCreator.create(run, run.getNetwork());
            Map<Broker, DockerContainer> brokerContainers = createBrokerContainers(run, run.getNetwork());
            lifecycle.simulation(run, run.getNetwork(), serverContainer, brokerContainers);
            Map<DockerContainer, DockerContainerExitState> exitStates = controller.run(run.getSimulationContainers());
            if (!gameCompletedSuccessfully(run, brokerContainers, exitStates)) {
                throw new GameRunException("game run didn't meet one of the post conditions; check orchestrator log for details");
            }
        } catch (IOException e) {
            throw new GameRunException("could not create simulation files", e);
        } catch (ContainerException| DockerException e) {
            throw new GameRunException("simulation run failed due to container error", e);
        } finally {
            DockerNetwork network = run.getNetwork();
            if (null != network) {
                if (deploymentContext.equals(DeploymentContext.CONTAINER)) {
                    disconnectWeatherServer(network.getId());
                }
                networks.removeNetwork(network);
            }
        }
    }

    private void connectWeatherServer(String networkNameOrId) throws DockerException {
        client.connectToNetworkCmd()
            .withContainerId(weatherServerContainerName)
            .withNetworkId(networkNameOrId)
            .exec();
    }

    private void disconnectWeatherServer(String networkNameOrId) throws DockerException {
        client.disconnectFromNetworkCmd()
            .withContainerId(weatherServerContainerName)
            .withNetworkId(networkNameOrId)
            .exec();
    }

    private String getNetworkName(Game game) {
        return String.format("ptac.%s", game.getId());
    }

    private Map<Broker, DockerContainer> createBrokerContainers(GameRun run, DockerNetwork network) throws DockerException {
        Map<Broker, DockerContainer> brokerContainers = new HashMap<>();
        for (Broker broker : run.getGame().getBrokers()) {
            brokerContainers.put(broker, brokerContainerCreator.create(run, broker, network));
        }
        return brokerContainers;
    }

    private boolean gameCompletedSuccessfully(GameRun run, Map<Broker, DockerContainer> brokerContainers, Map<DockerContainer, DockerContainerExitState> exitStates) {
        Map<Broker, DockerContainerExitState> brokerToExitStates = new HashMap<>();
        for (Map.Entry<Broker, DockerContainer> brokerContainer : brokerContainers.entrySet()) {
            brokerToExitStates.put(brokerContainer.getKey(), exitStates.get(brokerContainer.getValue()));
        }
        return postConditionValidator.isValid(run, brokerToExitStates, Instant.now());
    }

    private synchronized void stop(GameRun run) {
        try {
            if (run.getPhase().equals(GameRunPhase.BOOTSTRAP)) {
                controller.forceRemove(run.getBootstrapContainer());
            } else if (run.getPhase().equals(GameRunPhase.SIMULATION)) {
                for (DockerContainer container : run.getSimulationContainers()) {
                    controller.forceRemove(container);
                    networks.removeNetwork(run.getNetwork());
                }
            }
        } finally {
            lifecycle.fail(run);
        }
    }

    private void removeExistingDockerResources(Game game) {
        removeBootstrapContainerIfExists(game);
        removeSimulationContainerIfExists(game);
        removeExistingBrokerContainers(game);
        removeNetworkIfExists(game);
    }

    private void removeBootstrapContainerIfExists(Game game) throws DockerException {
        String bootstrapContainerName = bootstrapContainerCreator.getBootstrapContainerName(game);
        if (controller.exists(bootstrapContainerName)) {
            controller.forceRemove(bootstrapContainerName);
        }
    }

    private void removeSimulationContainerIfExists(Game game) {
        String simulationContainerName = simulationContainerCreator.getSimulationContainerName(game);
        if (controller.exists(simulationContainerName)) {
            controller.forceRemove(simulationContainerName);
        }
    }

    private void removeExistingBrokerContainers(Game game) {
        for (GameRun run : game.getRuns()) {
            for (Broker broker : game.getBrokers()) {
                String brokerContainerName = brokerContainerCreator.getBrokerContainerName(run, broker);
                if (controller.exists(brokerContainerName)) {
                    controller.forceRemove(brokerContainerName);
                }
            }
        }
    }

    private void removeNetworkIfExists(Game game) {
        String networkName = getNetworkName(game);
        if (networks.exists(networkName)) {
            if (deploymentContext.equals(DeploymentContext.CONTAINER)) {
                disconnectWeatherServer(networkName);
            }
            networks.removeNetworkIfExists(networkName);
        }
    }

}
