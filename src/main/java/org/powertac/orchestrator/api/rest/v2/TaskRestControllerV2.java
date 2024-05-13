package org.powertac.orchestrator.api.rest.v2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.orchestrator.exec.PersistentTaskDTO;
import org.powertac.orchestrator.exec.PersistentTaskRepository;
import org.powertac.orchestrator.exec.TaskDTOMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/v2/tasks")
public class TaskRestControllerV2 {

    private final PersistentTaskRepository taskRepository;
    private final TaskDTOMapper mapper;
    private final Logger logger;

    public TaskRestControllerV2(PersistentTaskRepository taskRepository, TaskDTOMapper mapper) {
        this.taskRepository = taskRepository;
        this.mapper = mapper;
        logger = LogManager.getLogger(TaskRestControllerV2.class);
    }

    @GetMapping("/")
    public ResponseEntity<Collection<PersistentTaskDTO<?>>> getTasks() {
        return ResponseEntity.ok(
            StreamSupport.stream(taskRepository.findAll().spliterator(), false)
                .map(mapper::toDTO)
                .collect(Collectors.toUnmodifiableList()));
    }

    @GetMapping("/since/{timestamp}")
    public ResponseEntity<Collection<PersistentTaskDTO<?>>> getTasksSince(@PathVariable String timestamp) {
        try {
            Instant lastUpdate = Instant.parse(timestamp);
            Collection<PersistentTaskDTO<?>> updatedTasks = taskRepository.findTaskUpdatedSince(lastUpdate)
                .stream().map(mapper::toDTO)
                .collect(Collectors.toUnmodifiableList());
            return ResponseEntity.ok(updatedTasks);
        } catch (Exception e) {
            logger.error("unable to find updated tasks since {}", timestamp, e);
            return ResponseEntity.badRequest().build();
        }
    }

}
