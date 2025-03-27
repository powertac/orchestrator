package org.powertac.orchestrator.game;

import org.powertac.orchestrator.broker.Broker;
import org.powertac.orchestrator.paths.PathProvider;

import java.nio.file.Path;
import java.nio.file.Paths;

public class OrchestratorGameRunPathProvider implements PathProvider.OrchestratorPaths.GameRunPaths {

    private final PathProvider.OrchestratorPaths parent;
    private final GameRun run;

    public OrchestratorGameRunPathProvider(PathProvider.OrchestratorPaths parent, GameRun run) {
        this.parent = parent;
        this.run = run;
    }

    @Override
    public Path dir() {
        return Paths.get(
            parent.game(run.getGame()).runs().toString(),
            run.getId());
    }

    @Override
    public Path log() {
        return Paths.get(
            dir().toString(),
            String.format("%s.run.log", run.getId()));
    }

    @Override
    public Path state() {
        return Paths.get(
            serverLogs().toString(),
            String.format("%s.state", run.getId()));
    }

    @Override
    public Path trace() {
        return Paths.get(
            serverLogs().toString(),
            String.format("%s.trace", run.getId()));
    }

    @Override
    public Path bootstrapTrace() {
        return Paths.get(
            dir().toString(),
            String.format("%s.bootstrap.trace", run.getId())
        );
    }

    @Override
    public BrokerPaths broker(Broker broker) {
        return () -> Paths.get(
                dir().toString(),
                broker.getHumanReadableIdentifier());
    }

    @Override
    public Path serverLogs() {
        return Paths.get(
            dir().toString(),
            "server");
    }

}
