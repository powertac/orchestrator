package org.powertac.orchestrator.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.orchestrator.util.ID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/simulation-servers")
public class SimulationServerRestController {

    private final SimulationServerVersionRepository versionRepository;
    private final Logger logger;

    public SimulationServerRestController(SimulationServerVersionRepository versionRepository) {
        this.versionRepository = versionRepository;
        logger = LogManager.getLogger(SimulationServerRestController.class);
    }

    @GetMapping("/versions")
    public ResponseEntity<Collection<SimulationServerVersion>> getAll() {
        try {
            Collection<SimulationServerVersion> versions = versionRepository.findAll();
            return ResponseEntity.ok(versions);
        } catch (Exception e) {
            logger.error("unable to server simulation server versions", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/versions")
    public ResponseEntity<SimulationServerVersion> createVersion(@RequestBody NewSimulationServerVersionDTO dto) {
        try {
            if (!versionRepository.existsByName(dto.name()) && !versionRepository.existsByImageTag(dto.imageTag())) {
                SimulationServerVersion newVersion = new SimulationServerVersion(ID.gen(), dto.name(), dto.imageTag());
                versionRepository.save(newVersion);
                return ResponseEntity.ok(newVersion);
            } else {
                logger.error("simulation server version with name or tag already exists (name={}, imageTag={})", dto.name(), dto.imageTag());
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            logger.error("unable to add new version (name={}, imageTag={})", dto.name(), dto.imageTag(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

}
