package org.powertac.orchestrator.server;

import com.github.dockerjava.api.model.Bind;
import org.powertac.orchestrator.game.Game;
import org.powertac.orchestrator.game.GameRun;

public interface ServerContainerBindFactory {

    Bind createSimulationPropertiesBind(Game game);
    Bind createBootstrapBind(Game game);
    Bind createWeatherBind(Game game);
    Bind createSeedBind(Game game);
    Bind createStateLogBind(GameRun run);
    Bind createTraceLogBind(GameRun run);

}
