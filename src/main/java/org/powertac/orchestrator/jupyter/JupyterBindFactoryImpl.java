package org.powertac.orchestrator.jupyter;

import com.github.dockerjava.api.model.Bind;
import org.powertac.orchestrator.docker.AbstractBindFactory;
import org.powertac.orchestrator.game.Game;
import org.powertac.orchestrator.paths.PathProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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

}
