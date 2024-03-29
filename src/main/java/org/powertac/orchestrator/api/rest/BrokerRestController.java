package org.powertac.orchestrator.api.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.orchestrator.broker.Broker;
import org.powertac.orchestrator.broker.BrokerConflictException;
import org.powertac.orchestrator.broker.BrokerRepository;
import org.powertac.orchestrator.broker.BrokerValidationException;
import org.powertac.orchestrator.docker.DockerImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("brokers")
public class BrokerRestController {

    private final BrokerRepository brokers;
    private final DockerImageRepository images;
    private final Logger logger;

    @Autowired
    public BrokerRestController(BrokerRepository brokers, DockerImageRepository images) {
        this.brokers = brokers;
        this.images = images;
        this.logger = LogManager.getLogger(BrokerRestController.class);
    }

    @GetMapping("/")
    public ResponseEntity<Collection<Broker>> getBrokers() {
        return ResponseEntity.ok(brokers.findAll());
    }

    @PostMapping("/")
    public ResponseEntity<?> createBroker(@RequestBody Broker broker) {
        try {
            broker.setEnabled(images.exists(broker.getImageTag()));
            brokers.save(broker);
            return ResponseEntity.ok().build();
        } catch (BrokerConflictException|BrokerValidationException e) {
            logger.error("error creating broker", e);
            return ResponseEntity.badRequest().build();
        }
    }

}
