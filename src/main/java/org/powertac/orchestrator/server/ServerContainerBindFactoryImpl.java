package org.powertac.orchestrator.server;

import com.github.dockerjava.api.model.Bind;
import org.powertac.orchestrator.docker.AbstractBindFactory;
import org.powertac.orchestrator.paths.PathProvider;
import org.powertac.orchestrator.game.Game;
import org.powertac.orchestrator.game.GameRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServerContainerBindFactoryImpl extends AbstractBindFactory implements ServerContainerBindFactory {

    private final PathProvider paths;

    @Autowired
    public ServerContainerBindFactoryImpl(PathProvider paths) {
        this.paths = paths;
    }

    @Override
    public Bind createSimulationPropertiesBind(Game game) {
        return bind(
            paths.host().game(game).properties(),
            paths.container().server().game(game).properties());
    }

    @Override
    public Bind createBootstrapBind(Game game) {
        return bind(
            paths.host().game(game).bootstrap(),
            paths.container().server().game(game).bootstrap());
    }

    @Override
        public Bind createWeatherBind(Game game) {
                return bind(
                paths.host().game(game).weather(),
                paths.container().server().game(game).weather());
        }

    @Override
    public Bind createSeedBind(Game game) {
        return bind(
            paths.host().game(game).seed(),
            paths.container().server().game(game).seed());
    }

    @Override
    public Bind createStateLogBind(GameRun run) {
        return bind(
            paths.host().run(run).state(),
            paths.container().server().run(run).state());
    }

    @Override
    public Bind createTraceLogBind(GameRun run) {
        return bind(
            paths.host().run(run).trace(),
            paths.container().server().run(run).trace());
    }

}
