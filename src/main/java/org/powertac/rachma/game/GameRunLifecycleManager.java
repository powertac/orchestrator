package org.powertac.rachma.game;

import org.powertac.rachma.broker.Broker;
import org.powertac.rachma.docker.container.DockerContainer;
import org.powertac.rachma.docker.network.DockerNetwork;

import java.util.Map;

public interface GameRunLifecycleManager {

    void preparation(GameRun run);
    void bootstrap(GameRun run, DockerContainer container);
    void ready(GameRun run);
    void simulation(GameRun run, DockerNetwork network, DockerContainer serverContainer, Map<Broker, DockerContainer> broker);
    void done(GameRun run);
    void fail(GameRun run);

}
