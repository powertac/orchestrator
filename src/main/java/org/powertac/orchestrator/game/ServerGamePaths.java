package org.powertac.orchestrator.game;

import org.powertac.orchestrator.paths.PathProvider;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerGamePaths implements PathProvider.ContainerPaths.ServerPaths.GamePaths {

    private final PathProvider.ContainerPaths.ServerPaths parent;
    private final Game game;

    public ServerGamePaths(PathProvider.ContainerPaths.ServerPaths parent, Game game) {
        this.parent = parent;
        this.game = game;
    }

    @Override
    public Path bootstrap() {
        return Paths.get(
            parent.base().toString(),
            String.format("%s.bootstrap.xml", game.getId()));
    }

    @Override
    public Path weather() {
        return Paths.get(
            parent.base().toString(), "weather");
    }

    @Override
    public Path properties() {
        return Paths.get(
            parent.base().toString(),
            String.format("%s.server.properties", game.getId()));
    }

    @Override
    public Path seed() {
        return Paths.get(
            parent.base().toString(),
            String.format("%s.seed.state", game.getId()));
    }

}
