package org.powertac.orchestrator.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.orchestrator.docker.exception.ContainerConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ManagedDockerContainerRepository implements DockerContainerRepository {

    private final DockerClient docker;

    // key must always be id (NOT NAME)
    private final ConcurrentMap<String, DockerContainer> containers;
    private final Logger logger;

    @Autowired
    public ManagedDockerContainerRepository(DockerClient docker) {
        this.docker = docker;
        containers = new ConcurrentHashMap<>();
        logger = LogManager.getLogger(ManagedDockerContainerRepository.class);
    }

    @Override
    public synchronized DockerContainer add(DockerContainer container) throws ContainerConflictException {
        Optional<DockerContainer> existingByName = findByName(container.getName());
        if (existingByName.isPresent() && !container.equals(existingByName.get())) {
            throw new ContainerConflictException("conflict: container with name='" + container.getName() + "' already exists");
        }
        Optional<DockerContainer> existingById = findById(container.getId());
        if (existingById.isPresent() && !container.equals(existingById.get())) {
            throw new ContainerConflictException("conflict: container with id='" + container.getId() + "' already exists");
        }
        updateState(container);
        containers.put(container.getId(), container);
        return container;
    }

    @Override
    public Optional<DockerContainer> findById(String id) {
        if (containers.containsKey(id)) {
            DockerContainer container = containers.get(id);
            updateState(container);
            return Optional.of(container);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<DockerContainer> findByName(String name) {
        return containers.values().stream()
            .filter(c -> c.getName().equals(name))
            .findAny();
    }

    @Override
    public Collection<DockerContainer> findAll() {
        Set<DockerContainer> updatedContainers = new HashSet<>();
        for (DockerContainer container : containers.values()) {
            updateState(container);
            updatedContainers.add(container);
        }
        return updatedContainers;
    }

    @Override
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    @Override
    public boolean existsByName(String name) {
        return containers.values().stream().anyMatch(c -> c.getName().equals(name));
    }

    @Override
    public DockerContainer run(DockerContainer container) {
        updateState(container);
        if (container.isRunning()) {
            return container;
        } else if (container.canBeStarted()) {
            docker.startContainerCmd(container.getId()).exec();
            updateState(container);
            return container;
        } else {
            throw new RuntimeException("unable to run container with id=" + container.getId());
        }
    }

    @Override
    public DockerContainer update(DockerContainer container) {
        updateState(container);
        return container;
    }

    @Override
    public void remove(DockerContainer container) {
        DockerContainerState state = getCurrentState(container.getId());

        // if the container is in any active state -> stop it
        if (state.equals(DockerContainerState.RUNNING)
            || state.equals(DockerContainerState.PAUSED)
            || state.equals(DockerContainerState.RESTARTING)) {
            try {
                docker.stopContainerCmd(container.getId()).exec();
            } catch (NotModifiedException|NotFoundException e) {
                // not modified: container was stopped in the meantime - do nothing
                // not found: container was removed in the meantime - do nothing
            } finally {
                // state will have changed -> reload it
                state = getCurrentState(container.getId());
            }
        }

        // we remove the container if it still exists
        if (!state.equals(DockerContainerState.NONE)) {
            try {
                docker.removeContainerCmd(container.getId()).exec();
            } catch (NotFoundException e) {
                // not found: container was removed in the meantime - do nothing
            }
        }

        // finally remove from map of managed containers
        containers.remove(container.getId());
    }

    @PreDestroy
    public void removeAll() {
        for(DockerContainer container : containers.values()) {
            try {
                remove(container);
            } catch (Exception e) {
                logger.error("unable to remove container with name=" + container.getName() + "; PLEASE REMOVE CONTAINER MANUALLY", e);
            }
        }
    }

    private synchronized void updateState(DockerContainer container) {
        DockerContainerState currentState = getCurrentState(container.getId());
        container.setLastKnownState(currentState);
    }

    private DockerContainerState getCurrentState(String containerId) {
        try {
            InspectContainerResponse response = docker.inspectContainerCmd(containerId).exec();
            InspectContainerResponse.ContainerState state = response.getState();
            if (state.getRunning() != null && state.getRunning()) {
                return DockerContainerState.RUNNING;
            } else if (state.getRestarting() != null && state.getRestarting()) {
                return DockerContainerState.RESTARTING;
            } else if (state.getDead() != null && state.getDead()) {
                return DockerContainerState.DEAD;
            } else if (state.getPaused() != null && state.getPaused()) {
                return DockerContainerState.PAUSED;
            } else if (state.getExitCodeLong() != null) {
                return DockerContainerState.EXITED;
            } else {
                return DockerContainerState.CREATED;
            }
        } catch (NotFoundException e) {
            return DockerContainerState.NONE;
        } catch (Exception e) {
            logger.warn("unable to determine state of container with id=" + containerId, e);
            return DockerContainerState.UNKNOWN;
        }
    }

}
