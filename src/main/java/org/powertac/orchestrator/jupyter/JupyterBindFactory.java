package org.powertac.orchestrator.jupyter;

import com.github.dockerjava.api.model.Bind;
import org.powertac.orchestrator.game.Game;

import java.util.Collection;
import java.util.List;

public interface JupyterBindFactory {

    List<Bind> createGameArtifactsBinds(Collection<Game> games);

}
