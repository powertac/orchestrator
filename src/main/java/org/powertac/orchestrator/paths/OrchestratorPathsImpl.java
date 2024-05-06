package org.powertac.orchestrator.paths;

import org.powertac.orchestrator.baseline.Baseline;
import org.powertac.orchestrator.baseline.OrchestratorBaselinePathProvider;
import org.powertac.orchestrator.game.Game;
import org.powertac.orchestrator.game.GameRun;
import org.powertac.orchestrator.game.OrchestratorGamePathProvider;
import org.powertac.orchestrator.game.OrchestratorGameRunPathProvider;
import org.powertac.orchestrator.treatment.OrchestratorTreatmentPathProvider;
import org.powertac.orchestrator.treatment.Treatment;

import java.nio.file.Path;
import java.nio.file.Paths;

public class OrchestratorPathsImpl implements PathProvider.OrchestratorPaths {

    private final String basePath;

    public OrchestratorPathsImpl(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public Path games() {
        return Paths.get(basePath, "games");
    }

    @Override
    public Path config() {
        return Paths.get(basePath, "config");
    }

    @Override
    public Path processorsFile(String logProcessorImageId) {
        String imageId = logProcessorImageId.replace("sha256:", "");
        return Paths.get(
            config().toString(),
            String.format("%s.processors.json", imageId));
    }

    @Override
    public GamePaths game(Game game) {
        return new OrchestratorGamePathProvider(this, game);
    }

    @Override
    public GameRunPaths run(GameRun run) {
        return new OrchestratorGameRunPathProvider(this, run);
    }

    @Override
    public Path baselines() {
        return Paths.get(basePath, "baselines");
    }

    @Override
    public BaselinePaths baseline(Baseline baseline) {
        return new OrchestratorBaselinePathProvider(this, baseline);
    }

    @Override
    public Path treatments() {
        return Paths.get(basePath, "treatments");
    }

    @Override
    public TreatmentPaths treatment(Treatment treatment) {
        return new OrchestratorTreatmentPathProvider(this, treatment);
    }

}
