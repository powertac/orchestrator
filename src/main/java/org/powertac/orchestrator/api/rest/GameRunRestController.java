package org.powertac.orchestrator.api.rest;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.orchestrator.paths.PathProvider;
import org.powertac.orchestrator.game.GameRun;
import org.powertac.orchestrator.game.GameRunRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/runs")
public class GameRunRestController {

    private final GameRunRepository runRepository;
    private final PathProvider paths;
    private final Logger logger;

    public GameRunRestController(GameRunRepository runRepository, PathProvider paths) {
        this.runRepository = runRepository;
        this.paths = paths;
        this.logger = LogManager.getLogger(GameRunRestController.class);
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<String> getLogs(@PathVariable("id") String id) {
        GameRun run = runRepository.find(id);
        if (null == run) {
            logger.error(String.format("could not find run[%s]", id));
            return ResponseEntity.notFound().build();
        }
        Path logPath = paths.local().run(run).log();
        if (!Files.exists(logPath)) {
            logger.error(String.format("log file %s does not exist", logPath));
            return ResponseEntity.notFound().build();
        }
        try {
            return ResponseEntity.ok(Files.readString(logPath));
        } catch (IOException e) {
            logger.error(String.format("could not read log file %s", logPath), e);
            return ResponseEntity.status(500).build();
        }
    }

    @Deprecated
    @GetMapping("/{id}/files")
    public ResponseEntity<List<String>> getFilesMeta(@PathVariable("id") String id) {
        GameRun run = runRepository.find(id);
        if (null == run) {
            logger.error(String.format("could not find run[%s]", id));
            return ResponseEntity.notFound().build();
        } else {
            PathProvider.OrchestratorPaths.GameRunPaths runPaths = paths.local().run(run);
            List<String> fileTypes = new ArrayList<>();
            if (Files.exists(runPaths.log())) {
                fileTypes.add("log");
            }
            if (Files.exists(runPaths.state())) {
                fileTypes.add("state");
            }
            if (Files.exists(runPaths.trace())) {
                fileTypes.add("trace");
            }
            return ResponseEntity.ok(fileTypes);
        }
    }

    @GetMapping("/failed")
    public ResponseEntity<Collection<GameRun>> getFailedRuns() {
        Collection<GameRun> failedRuns = runRepository.findFailed();
        return ResponseEntity.ok(failedRuns);
    }

}
