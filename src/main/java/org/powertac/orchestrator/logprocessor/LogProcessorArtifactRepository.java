package org.powertac.orchestrator.logprocessor;

import org.powertac.orchestrator.game.Game;

import java.util.Collection;

public interface LogProcessorArtifactRepository {

    Collection<LogProcessorArtifact> findAllByGame(Game game);

}
