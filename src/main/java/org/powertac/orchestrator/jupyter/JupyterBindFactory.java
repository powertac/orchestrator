package org.powertac.orchestrator.jupyter;

import com.github.dockerjava.api.model.Bind;
import org.powertac.orchestrator.analysis.scope.Scope;
import org.powertac.orchestrator.game.Game;

import java.util.Collection;
import java.util.List;

public interface JupyterBindFactory {

    Bind createScopeFileBind(Scope scope);
    List<Bind> createGameArtifactsBinds(Collection<Game> games);

}
