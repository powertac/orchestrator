package org.powertac.orchestrator.jupyter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.orchestrator.analysis.scope.Scope;
import org.powertac.orchestrator.baseline.Baseline;
import org.powertac.orchestrator.baseline.BaselineRepository;
import org.powertac.orchestrator.docker.DockerContainer;
import org.powertac.orchestrator.game.Game;
import org.powertac.orchestrator.game.GameRepository;
import org.powertac.orchestrator.treatment.Treatment;
import org.powertac.orchestrator.treatment.TreatmentRepository;
import org.powertac.orchestrator.util.exception.ConflictException;
import org.powertac.orchestrator.util.exception.CreationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/jupyter")
public class JupyterRestController {

    private final JupyterInstanceRepository instances;
    private final GameRepository games;
    private final BaselineRepository baselines;
    private final TreatmentRepository treatments;
    private final Logger logger;

    public JupyterRestController(JupyterInstanceRepository instances, GameRepository games,
                                 BaselineRepository baselines, TreatmentRepository treatments) {
        this.instances = instances;
        this.games = games;
        this.baselines = baselines;
        this.treatments = treatments;
        logger = LogManager.getLogger(JupyterRestController.class);
    }

    @GetMapping("/{scopeId}")
    public ResponseEntity<JupyterInstanceDTO> getInstance(@PathVariable String scopeId) {
        try {
            Optional<JupyterInstance> instance = instances.find(scopeId);
            return instance.isPresent()
                ? ResponseEntity.ok(instance.map(this::toDto).get())
                : ResponseEntity.ok(null);
        } catch (Exception e) {
            logger.error("cannot retrieve instance with id=" + scopeId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/games/{gameId}")
    public ResponseEntity<JupyterInstanceDTO> startForGame(@PathVariable String gameId) {
        try {
            Game game = games.findById(gameId);
            if (game == null) {
                logger.error("unable to find game with id=" + gameId);
                return ResponseEntity.badRequest().build();
            }
            JupyterInstance instance = findOrRunInstance(game);
            return ResponseEntity.ok(toDto(instance));
        } catch (Exception e) {
            logger.error("unable to start jupyter server for game with id=" + gameId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/baselines/{baselineId}")
    public ResponseEntity<JupyterInstanceDTO> startForBaseline(@PathVariable String baselineId) {
        try {
            Optional<Baseline> baseline = baselines.findById(baselineId);
            if (baseline.isEmpty()) {
                logger.error("unable to find baseline with id=" + baselineId);
                return ResponseEntity.badRequest().build();
            }
            JupyterInstance instance = findOrRunInstance(baseline.get());
            return ResponseEntity.ok(toDto(instance));
        } catch (Exception e) {
            logger.error("unable to start jupyter server for baseline with id=" + baselineId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/treatments/{treatmentId}")
    public ResponseEntity<JupyterInstance> startForTreatment(@PathVariable String treatmentId) {
        try {
            Optional<Treatment> treatment = treatments.findById(treatmentId);
            if (treatment.isEmpty()) {
                logger.error("unable to find treatment with id=" + treatment);
                return ResponseEntity.badRequest().build();
            }
            JupyterInstance instance = findOrRunInstance(treatment.get());
            return ResponseEntity.ok(instance);
        } catch (Exception e) {
            logger.error("unable to start jupyter server for treatment with id=" + treatmentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{instanceId}")
    public ResponseEntity<?> remove(@PathVariable String instanceId) {
        try {
            Optional<JupyterInstance> instance = instances.find(instanceId);
            if (instance.isEmpty()) {
                logger.warn("can't stop container; no jupyter instance exists for config with id=" + instanceId);
                return ResponseEntity.badRequest().build();
            }
            instances.remove(instance.get());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("unable to stop jupyter instance for config with id=" + instanceId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private JupyterInstance findOrRunInstance(Scope scope) throws CreationException, ConflictException {
        Optional<JupyterInstance> instance = instances.find(scope.getId());

        // return running instance or remove stopped existing instance
        if (instance.isPresent()) {
            if (instance.get().isRunning()) {
                return instance.get();
            }
            instances.remove(instance.get());
        }

        // create & run new instance
        JupyterInstance newInstance = instances.create(scope);
        try {
            return instances.run(newInstance);
        } catch (ConflictException e) {
            instances.remove(newInstance);
            throw e;
        }
    }

    private JupyterInstanceDTO toDto(JupyterInstance instance) {
        return new JupyterInstanceDTO(
            instance.getScope().getId(),
            instance.getPort(),
            instance.getToken(),
            instance.getContainer().isRunning());
    }

}
