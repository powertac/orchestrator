package org.powertac.orchestrator.baseline;

import org.powertac.orchestrator.paths.PathProvider;

import java.nio.file.Path;
import java.nio.file.Paths;

public class OrchestratorBaselinePathProvider implements PathProvider.OrchestratorPaths.BaselinePaths {

    private final PathProvider.OrchestratorPaths parent;
    private final Baseline baseline;

    public OrchestratorBaselinePathProvider(PathProvider.OrchestratorPaths parent, Baseline baseline) {
        this.parent = parent;
        this.baseline = baseline;
    }

    @Override
    public Path dir() {
        return Paths.get(parent.baselines().toString(), baseline.getId());
    }

    @Override
    public Path artifacts() {
        return Paths.get(dir().toString(), "artifacts");
    }

    @Override
    public Path manifest() {
        return Paths.get(dir().toString(), String.format("%s.games.csv", baseline.getId()));
    }

    @Override
    public Path scope() {
        return Paths.get(dir().toString(), String.format("%s.scope.json", baseline.getId()));
    }

}
