package org.powertac.orchestrator.analysis.scope;

import org.powertac.orchestrator.game.Game;

import java.util.Collection;

public interface Scope {

    String getId();
    Collection<Game> getGames();

}
