package org.powertac.rachma.broker;

import org.powertac.rachma.game.Game;
import org.powertac.rachma.game.GameRun;
import org.powertac.rachma.paths.PathProvider;

import java.nio.file.Path;
import java.nio.file.Paths;

public class BrokerContainerPaths implements PathProvider.ContainerPaths.BrokerPaths {

    private final static String basePath = "/powertac/broker";

    private final Broker broker;

    public BrokerContainerPaths(Broker broker) {
        this.broker = broker;
    }

    @Override
    public Path base() {
        return Paths.get(basePath);
    }

    @Override
    public GamePaths game(Game game) {
        // since the GamePaths interface has only one method at this time, it may be used as a function interface
        return () -> Paths.get(
            base().toString(),
            String.format("%s.%s.properties", game.getId(), broker.getName()));
    }

    @Override
    public GameRunPaths run(GameRun run) {
        // since the GamePaths interface has only one method at this time, it may be used as a function interface
        return () -> Paths.get(
            base().toString(),
            "log");
    }

}