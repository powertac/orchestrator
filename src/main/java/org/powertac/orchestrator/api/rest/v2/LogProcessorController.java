package org.powertac.orchestrator.api.rest.v2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.orchestrator.baseline.Baseline;
import org.powertac.orchestrator.baseline.BaselineRepository;
import org.powertac.orchestrator.exec.PersistentTaskDTO;
import org.powertac.orchestrator.exec.TaskDTOMapper;
import org.powertac.orchestrator.exec.TaskScheduler;
import org.powertac.orchestrator.game.Game;
import org.powertac.orchestrator.game.GameRepository;
import org.powertac.orchestrator.logprocessor.*;
import org.powertac.orchestrator.paths.PathProvider;
import org.powertac.orchestrator.treatment.Treatment;
import org.powertac.orchestrator.treatment.TreatmentRepository;
import org.powertac.orchestrator.user.exception.UserNotFoundException;
import org.powertac.orchestrator.user.UserProvider;
import org.powertac.orchestrator.util.ID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v2/log-processors")
public class LogProcessorController {

    private final UserProvider userProvider;
    private final LogProcessorProvider processorProvider;
    private final GameRepository gameRepository;
    private final BaselineRepository baselineRepository;
    private final TreatmentRepository treatmentRepository;
    private final TaskScheduler taskScheduler;
    private final LogProcessorTaskRepository taskRepository;
    private final TaskDTOMapper dtoMapper;
    private final PathProvider paths;
    private final Logger logger;

    public LogProcessorController(UserProvider userProvider, LogProcessorProvider processorProvider,
                                  GameRepository gameRepository, BaselineRepository baselineRepository,
                                  TreatmentRepository treatmentRepository, TaskScheduler taskScheduler,
                                  LogProcessorTaskRepository taskRepository, TaskDTOMapper dtoMapper,
                                  PathProvider paths) {
        this.userProvider = userProvider;
        this.processorProvider = processorProvider;
        this.gameRepository = gameRepository;
        this.baselineRepository = baselineRepository;
        this.treatmentRepository = treatmentRepository;
        this.taskScheduler = taskScheduler;
        this.taskRepository = taskRepository;
        this.dtoMapper = dtoMapper;
        this.paths = paths;
        logger = LogManager.getLogger(LogProcessorController.class);
    }

    @GetMapping("/available")
    public ResponseEntity<Set<LogProcessor>> getAvailableProcessors() {
        try {
            return ResponseEntity.ok(processorProvider.getAvailableProcessors().stream().filter(p -> p.getFileNamePattern() != null).collect(Collectors.toSet()));
        } catch (Exception e) {
            logger.error("unable to serve available log processors");
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/")
    public ResponseEntity<PersistentTaskDTO<LogProcessorTaskConfig>> processLog(@RequestBody LogProcessorTaskConfig dto) {
        try {
            Game game = gameRepository.findById(dto.getGameId());
            if (game == null) {
                logger.error("unable to find game with id=" + dto.getGameId());
                return ResponseEntity.notFound().build();
            } else if (!dto.getProcessorNames().stream().allMatch(processorProvider::has)) {
                logger.error("one or more processors do not exist; " + String.join(", ", dto.getProcessorNames()));
                return ResponseEntity.badRequest().build();
            } else {
                LogProcessorTask task = createTask(game, dto.getProcessorNames());
                taskScheduler.add(task);
                return ResponseEntity.ok(dtoMapper.toDTO(task));
            }
        } catch (UserNotFoundException e) {
            logger.error("unable to determine current user", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<List<PersistentTaskDTO<LogProcessorTaskConfig>>> processLogs(@RequestBody Set<LogProcessorTaskConfig> configs) {
        try {
            List<LogProcessorTask> tasks = new ArrayList<>();
            for (LogProcessorTaskConfig config : configs) {
                Game game = gameRepository.findById(config.getGameId());
                if (game == null) {
                    logger.error("unable to find game with id=" + config.getGameId());
                    return ResponseEntity.notFound().build();
                } else if (!config.getProcessorNames().stream().allMatch(processorProvider::has)) {
                    logger.error("one or more processors do not exist; " + String.join(", ", config.getProcessorNames()));
                    return ResponseEntity.badRequest().build();
                } else {
                    tasks.add(createTask(game, config.getProcessorNames()));
                }
            }
            tasks.forEach(taskScheduler::add);
            return ResponseEntity.ok(tasks.stream()
                .map(dtoMapper::<LogProcessorTaskConfig>toDTO)
                .collect(Collectors.toList()));
        } catch (UserNotFoundException e) {
            logger.error("unable to determine current user", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/game/{id}")
    public ResponseEntity<Collection<PersistentTaskDTO<LogProcessorTaskConfig>>> getGameTasks(@PathVariable String id) {
        try {
            Game game = gameRepository.findById(id);
            if (null != game) {
                return ResponseEntity.ok(taskRepository.findAllByGame(game).stream()
                    .map(dtoMapper::<LogProcessorTaskConfig>toDTO)
                    .collect(Collectors.toSet()));
            } else {
                logger.error("no game found with id=" + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("unable to serve log processor tasks for game with id=" + id);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/game/{id}/artifacts")
    public ResponseEntity<Set<LogProcessorArtifactDTO>> getLogProcessorArtifacts(@PathVariable String id) {
        try {
            Game game = gameRepository.findById(id);
            if (null != game) {
                return ResponseEntity.ok(getGameArtifacts(game));
            } else {
                logger.error("unable to find game with id=" + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("unable to serve log processor status for game with id=" + id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/baseline/{baselineId}/artifacts")
    public ResponseEntity<Map<String, Set<LogProcessorArtifactDTO>>> getBaselineArtifacts(@PathVariable String baselineId) {
        try {
            Optional<Baseline> baseline = baselineRepository.findById(baselineId);
            if (baseline.isPresent()) {
                Map<String, Set<LogProcessorArtifactDTO>> artifacts = new HashMap<>();
                for (Game game : baseline.get().getGames()) {
                    artifacts.put(game.getId(), getGameArtifacts(game));
                }
                return ResponseEntity.ok(artifacts);
            } else {
                logger.error("unable to find baseline with id=" + baselineId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("unable to serve all log processor artifacts for baseline with id=" + baselineId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/treatments/{treatmentId}/artifacts")
    public ResponseEntity<Map<String, Set<LogProcessorArtifactDTO>>> getTreatmentArtifacts(@PathVariable String treatmentId) {
        try {
            Optional<Treatment> treatment = treatmentRepository.findById(treatmentId);
            if (treatment.isPresent()) {
                Map<String, Set<LogProcessorArtifactDTO>> artifacts = new HashMap<>();
                for (Game game : treatment.get().getGames()) {
                    artifacts.put(game.getId(), getGameArtifacts(game));
                }
                return ResponseEntity.ok(artifacts);
            } else {
                logger.error("unable to find treatment with id=" + treatmentId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("unable to serve all log processor artifacts for treatment with id=" + treatmentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private Set<LogProcessorArtifactDTO> getGameArtifacts(Game game) {
        Set<LogProcessorArtifactDTO> artifacts = new HashSet<>();
        String hostGameArtifactsDir = paths.host().game(game).artifacts().toString();
        String localGameArtifactsDir = paths.host().game(game).artifacts().toString();
        Set<LogProcessor> fileBasedProcessors = processorProvider.getAvailableProcessors().stream()
            .filter(p -> p.getFileNamePattern() != null)
            .collect(Collectors.toSet());
        for (LogProcessor processor : fileBasedProcessors) {
            String filename = String.format(processor.getFileNamePattern(), game.getId());
            // we use the local path for existence check
            if (Files.exists(Paths.get(localGameArtifactsDir, filename))) {
                artifacts.add(LogProcessorArtifactDTO.builder()
                    .processorName(processor.getName())
                    // ... and serve the host path for use with outside applications
                    .filePath(Paths.get(hostGameArtifactsDir, filename).toString())
                    .build());
            }
        }
        return artifacts;
    }

    private LogProcessorTask createTask(Game game, Set<String> processorNames) throws UserNotFoundException {
        return LogProcessorTask.builder()
            .id(ID.gen())
            .creator(userProvider.getCurrentUser())
            .createdAt(Instant.now())
            .game(game)
            .processorIds(processorNames)
            .build();
    }

}
