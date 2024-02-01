package org.powertac.orchestrator.api.rest.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.orchestrator.analysis.*;
import org.powertac.orchestrator.analysis.dtos.AnalyzerArtifactDTO;
import org.powertac.orchestrator.analysis.dtos.AnalyzerBrokerDTO;
import org.powertac.orchestrator.analysis.dtos.AnalyzerGameDTO;
import org.powertac.orchestrator.analysis.dtos.AnalyzerGameGroupDTO;
import org.powertac.orchestrator.artifact.ArtifactProducer;
import org.powertac.orchestrator.baseline.Baseline;
import org.powertac.orchestrator.baseline.BaselineRepository;
import org.powertac.orchestrator.broker.Broker;
import org.powertac.orchestrator.exec.PersistentTaskDTO;
import org.powertac.orchestrator.exec.TaskDTOMapper;
import org.powertac.orchestrator.exec.TaskScheduler;
import org.powertac.orchestrator.file.DownloadToken;
import org.powertac.orchestrator.file.DownloadTokenRepository;
import org.powertac.orchestrator.game.Game;
import org.powertac.orchestrator.logprocessor.LogProcessor;
import org.powertac.orchestrator.paths.PathProvider;
import org.powertac.orchestrator.security.JwtTokenService;
import org.powertac.orchestrator.treatment.Treatment;
import org.powertac.orchestrator.treatment.TreatmentRepository;
import org.powertac.orchestrator.user.domain.User;
import org.powertac.orchestrator.user.UserProvider;
import org.powertac.orchestrator.util.ID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v2/analysis")
public class AnalyzerRestController {

    private final AnalyzerProvider analyzerProvider;
    private final BaselineRepository baselineRepository;
    private final TreatmentRepository treatmentRepository;
    private final PathProvider paths;
    private final TaskScheduler taskScheduler;
    private final TaskDTOMapper dtoMapper;
    private final UserProvider userProvider;
    private final JwtTokenService tokenService;
    private final DownloadTokenRepository tokenRepository;
    private final Logger logger;

    public AnalyzerRestController(AnalyzerProvider analyzerProvider, BaselineRepository baselineRepository,
                                  TreatmentRepository treatmentRepository, PathProvider paths, TaskScheduler taskScheduler,
                                  TaskDTOMapper dtoMapper, UserProvider userProvider, JwtTokenService tokenService,
                                  DownloadTokenRepository tokenRepository) {
        this.analyzerProvider = analyzerProvider;
        this.baselineRepository = baselineRepository;
        this.treatmentRepository = treatmentRepository;
        this.paths = paths;
        this.taskScheduler = taskScheduler;
        this.dtoMapper = dtoMapper;
        this.userProvider = userProvider;
        this.tokenService = tokenService;
        this.tokenRepository = tokenRepository;
        logger = LogManager.getLogger(AnalyzerRestController.class);
    }

    @GetMapping("/")
    public ResponseEntity<Set<Analyzer>> getAvailableAnalyzers() {
        try {
            return ResponseEntity.ok(analyzerProvider.getAvailableAnalyzers());
        } catch (Exception e) {
            logger.error("unable to serve available analyzers", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/baselines/{baselineId}/{analyzerName}")
    public ResponseEntity<PersistentTaskDTO<AnalyzerTaskConfig>> analyzeBaseline(@PathVariable String baselineId, @PathVariable String analyzerName) {
        try {
            Optional<Baseline> baseline = baselineRepository.findById(baselineId);
            if (baseline.isEmpty()) {
                logger.error("cannot find baseline with id=" + baselineId);
                return ResponseEntity.notFound().build();
            }
            Optional<Analyzer> analyzer = analyzerProvider.get(analyzerName);
            if (analyzer.isEmpty()) {
                logger.error("cannot find analyzer with name=" + analyzerName);
                return ResponseEntity.notFound().build();
            }
            if (preconditionsMet(baseline.get().getGames(), analyzer.get())) {
                writeBaselineJson(baseline.get());
                AnalyzerTask task = AnalyzerTask.builder()
                    .id(ID.gen())
                    .creator(userProvider.getCurrentUser())
                    .createdAt(Instant.now())
                    .analyzerName(analyzerName)
                    .baseline(baseline.get())
                    .build();
                taskScheduler.add(task);
                return ResponseEntity.ok(dtoMapper.toDTO(task));
            } else {
                logger.error("not all preconditions are met for analyzer '" + analyzerName + "' and baseline with id=" + baselineId);
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            logger.error("unable to create new analysis task", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/baselines/{baselineId}/{analyzerName}")
    public ResponseEntity<AnalyzerArtifactDTO> getAnalyzerArtifact(@PathVariable String baselineId, @PathVariable String analyzerName) {
        try {
            Optional<Baseline> baseline = baselineRepository.findById(baselineId);
            if (baseline.isPresent()) {
                Path realpath = Paths.get(paths.local().baseline(baseline.get()).artifacts().toString(), baselineId + ".wholesale-prices-boxplot.png");
                if (Files.exists(realpath)) {
                    String relativeArtifactPath = "/baselines/" + baseline.get().getId() + "/artifacts/" + baselineId + ".wholesale-prices-boxplot.png";
                    DownloadToken token = findOrCreateDownloadToken(userProvider.getCurrentUser(), relativeArtifactPath);
                    return ResponseEntity.ok(new AnalyzerArtifactDTO(analyzerName, token.getToken()));
                } else {
                    return ResponseEntity.notFound().build();
                }
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("unable to serve analysis artifact", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/treatments/{treatmentId}/{analyzerName}")
    public ResponseEntity<PersistentTaskDTO<AnalyzerTaskConfig>> analyzeTreatment(@PathVariable String treatmentId, @PathVariable String analyzerName) {
        try {
            Optional<Treatment> treatment = treatmentRepository.findById(treatmentId);
            if (treatment.isEmpty()) {
                logger.error("cannot find treatment with id=" + treatmentId);
                return ResponseEntity.notFound().build();
            }
            Optional<Analyzer> analyzer = analyzerProvider.get(analyzerName);
            if (analyzer.isEmpty()) {
                logger.error("cannot find analyzer with name=" + analyzerName);
                return ResponseEntity.notFound().build();
            }
            if (preconditionsMet(treatment.get().getGames(), analyzer.get())) {
                writeTreatmentJson(treatment.get());
                AnalyzerTask task = AnalyzerTask.builder()
                    .id(ID.gen())
                    .creator(userProvider.getCurrentUser())
                    .createdAt(Instant.now())
                    .analyzerName(analyzerName)
                    .treatment(treatment.get())
                    .build();
                taskScheduler.add(task);
                return ResponseEntity.ok(dtoMapper.toDTO(task));
            } else {
                logger.error("not all preconditions are met for analyzer '" + analyzerName + "' and treatment with id=" + treatmentId);
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            logger.error("unable to create new analysis task", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/treatments/{treatmentId}/{analyzerName}")
    public ResponseEntity<AnalyzerArtifactDTO> getTreatmentAnalyzerArtifact(@PathVariable String treatmentId, @PathVariable String analyzerName) {
        try {
            Optional<Treatment> treatment = treatmentRepository.findById(treatmentId);
            if (treatment.isPresent()) {
                Path realpath = Paths.get(paths.local().treatment(treatment.get()).artifacts().toString(), treatmentId + ".wholesale-prices-boxplot.png");
                if (Files.exists(realpath)) {
                    String relativeArtifactPath = "/treatments/" + treatment.get().getId() + "/artifacts/" + treatmentId + ".wholesale-prices-boxplot.png";
                    DownloadToken token = findOrCreateDownloadToken(userProvider.getCurrentUser(), relativeArtifactPath);
                    return ResponseEntity.ok(new AnalyzerArtifactDTO(analyzerName, token.getToken()));
                } else {
                    return ResponseEntity.notFound().build();
                }
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("unable to serve analysis artifact", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private DownloadToken findOrCreateDownloadToken(User user, String path) {
        Optional<DownloadToken> download = tokenRepository.findByUserAndFilePath(user, path);
        String token = tokenService.createDownloadToken(user, path);
        return download.orElseGet(() -> tokenRepository.save(DownloadToken.builder()
            .user(user)
            .filePath(path)
            .token(token)
            .build()));
    }

    private boolean preconditionsMet(Collection<Game> games, Analyzer analyzer) {
        boolean allConditionsMet = true;
        for (ArtifactProducer producer : analyzer.getRequirements()) {
            if (producer instanceof LogProcessor) {
                allConditionsMet = allConditionsMet && games.stream()
                    .map(g -> getArtifactPath(g, (LogProcessor) producer))
                    .allMatch(Files::exists);
            } else {
                throw new RuntimeException("only log processors are supported as artifact producers at this time");
            }
        }
        return allConditionsMet;
    }

    private Path getArtifactPath(Game game, LogProcessor processor) {
        return Paths.get(
            paths.local().game(game).artifacts().toString(),
            String.format(processor.getFileNamePattern(), game.getId()));
    }

    private AnalyzerGameGroupDTO toGameGroupDTO(Baseline baseline) {
        return AnalyzerGameGroupDTO.builder()
            .id(baseline.getId())
            .name(baseline.getName())
            .type(AnalyzerGameGroupType.BASELINE)
            .games(baseline.getGames().stream().map(this::toGameDTO).collect(Collectors.toSet()))
            .build();
    }

    private AnalyzerGameGroupDTO toGameGroupDTO(Treatment treatment) {
        return AnalyzerGameGroupDTO.builder()
            .id(treatment.getId())
            .name(treatment.getName())
            .type(AnalyzerGameGroupType.TREATMENT)
            .games(treatment.getGames().stream().map(this::toGameDTO).collect(Collectors.toSet()))
            .build();
    }

    private AnalyzerGameDTO toGameDTO(Game game) {
        return AnalyzerGameDTO.builder()
            .id(game.getId())
            .name(game.getName())
            .brokers(game.getBrokers().stream().map(this::toBrokerDTO).collect(Collectors.toSet()))
            .fileRoot("/opt/powertac/analysis/data/games/" + game.getId())
            .build();
    }

    private AnalyzerBrokerDTO toBrokerDTO(Broker broker) {
        return AnalyzerBrokerDTO.builder()
            .id(broker.getId())
            .name(broker.getName())
            .version(broker.getVersion())
            .build();
    }

    private void writeBaselineJson(Baseline baseline) throws IOException {
        Path baselineDir = paths.local().baseline(baseline).dir();
        if (!Files.exists(baselineDir)) {
            Files.createDirectories(baselineDir);
        }
        Path gameGroupDTO = Paths.get(paths.local().baseline(baseline).dir().toString(), String.format("%s.baseline.json", baseline.getId()));
        new ObjectMapper().writeValue(gameGroupDTO.toFile(), toGameGroupDTO(baseline));
    }

    private void writeTreatmentJson(Treatment treatment) throws IOException {
        Path treatmentDir = paths.local().treatment(treatment).dir();
        if (!Files.exists(treatmentDir)) {
            Files.createDirectories(treatmentDir);
        }
        Path gameGroupDTO = Paths.get(paths.local().treatment(treatment).dir().toString(), String.format("%s.treatment.json", treatment.getId()));
        new ObjectMapper().writeValue(gameGroupDTO.toFile(), toGameGroupDTO(treatment));
    }

}
