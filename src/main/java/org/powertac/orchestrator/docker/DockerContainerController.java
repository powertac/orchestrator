package org.powertac.orchestrator.docker;

import com.github.dockerjava.api.exception.DockerException;
import org.powertac.orchestrator.docker.exception.ContainerException;
import org.powertac.orchestrator.docker.exception.ContainerReflectionException;
import org.powertac.orchestrator.docker.exception.ContainerStartException;
import org.powertac.orchestrator.docker.exception.KillContainerException;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public interface DockerContainerController {

    void start(DockerContainer container) throws ContainerStartException;
    void kill(DockerContainer container) throws KillContainerException;
    DockerContainerExitState run(DockerContainer container) throws ContainerException;
    DockerContainerExitState run(DockerContainer container, Consumer<DockerContainerExitState> onComplete) throws ContainerException;
    Map<DockerContainer, DockerContainerExitState> run(Set<DockerContainer> containers) throws ContainerException;
    boolean isRunning(DockerContainer container) throws ContainerReflectionException;
    void remove(DockerContainer container) throws DockerException;
    void forceRemove(DockerContainer container) throws DockerException;
    void forceRemove(String name) throws DockerException;
    boolean exists(String name) throws DockerException;

    void copyResource(String containerId, String containerPath, String hostPath) throws IOException;
}
