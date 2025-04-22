package org.powertac.orchestrator.game;

import org.powertac.orchestrator.broker.Broker;
import org.powertac.orchestrator.logprocessor.LogProcessor;
import org.powertac.orchestrator.paths.PathProvider;

import java.nio.file.Path;
import java.nio.file.Paths;

public class OrchestratorGamePathProvider implements PathProvider.OrchestratorPaths.GamePaths {

    private final PathProvider.OrchestratorPaths parent;
    private final Game game;

    public OrchestratorGamePathProvider(PathProvider.OrchestratorPaths parent, Game game) {
        this.parent = parent;
        this.game = game;
    }

    @Override
    public Path bootstrap() {
        return Paths.get(dir().toString(), String.format("%s.bootstrap.xml", game.getId()));
    }

    @Override
    public Path weather ()
    {
        return Paths.get(dir().toString(), "weather");
    }

    @Override
    public Path dir() {
        return Paths.get(
            parent.games().toString(),
            game.getId());
    }

    @Override
    public Path runs() {
        return Paths.get(
            dir().toString(),
            "runs");
    }

    @Override
    public Path properties() {
        return Paths.get(
            dir().toString(),
            String.format("%s.server.properties", game.getId()));
    }

    @Override
    public Path seed() {
        return null != game.getSeed()
            ? parent.run(game.getSeed().getGame().getLatestSuccessfulRun()).state()
            :null;
    }

    @Override
    public Path archive() {
        return Paths.get(
            dir().toString(),
            String.format("%s.game.tar.gz", game.getId()));
    }

    @Override
    public Path artifacts() {
        return Paths.get(
            dir().toString(),
            "artifacts"
        );
    }

    @Override
    public Path logProcessorArtifact(LogProcessor processor) {
        return Paths.get(
            artifacts().toString(),
            String.format(processor.getFileNamePattern(), game.getId()));
    }

    @Override
    public Path scope() {
        return Paths.get(dir().toString(), String.format("%s.scope.json", game.getId()));
    }

    @Override
    public BrokerPaths broker(Broker broker) {
        // since the BrokerPaths interface has only one method at this time, it may be used as a function interface
        return () -> Paths.get(
            dir().toString(),
            // FIXME : this does not take the broker's version into account which WILL lead to configuration conflicts
            String.format("%s.%s.properties", game.getId(), broker.getName()));
    }

}
