package org.powertac.orchestrator.jupyter;

import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import org.apache.commons.lang3.NotImplementedException;
import org.powertac.orchestrator.analysis.scope.Scope;
import org.powertac.orchestrator.baseline.Baseline;
import org.powertac.orchestrator.docker.AbstractBindFactory;
import org.powertac.orchestrator.game.Game;
import org.powertac.orchestrator.paths.PathProvider;
import org.powertac.orchestrator.treatment.Treatment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JupyterBindFactoryImpl extends AbstractBindFactory implements JupyterBindFactory {

    private final static String defaultContainerWorkDir = "/home/jovyan";

    private final PathProvider paths;

    @Autowired
    public JupyterBindFactoryImpl(PathProvider paths) {
        this.paths = paths;
    }

    @Override
    public Bind createScopeFileBind(Scope scope) {
        return new Bind(
            resolveScopeFilePath(scope).toString(),
            new Volume(Paths.get(defaultContainerWorkDir, "scope.json").toString()),
            AccessMode.ro);
    }

    @Override
    public List<Bind> createGameArtifactsBinds(Collection<Game> games) {
        return games.stream()
            .map(this::createBind)
            .collect(Collectors.toList());
    }

    private Bind createBind(Game game) {
        return bind(
            paths.host().game(game).artifacts(),
            Paths.get(defaultContainerWorkDir, "data", game.getId()));
    }

    private Path resolveScopeFilePath(Scope scope) {
        // TODO - duplicate code; move to interface level
        return switch (scope) {
            case Game g -> paths.local().game(g).scope();
            case Baseline b -> paths.local().baseline(b).scope();
            case Treatment t -> paths.local().treatment(t).scope();
            default -> throw new NotImplementedException("scope file path resolution not implemented for type " + scope.getClass());
        };
    }

}
