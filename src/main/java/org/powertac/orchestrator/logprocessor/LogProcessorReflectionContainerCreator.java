package org.powertac.orchestrator.logprocessor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import org.powertac.orchestrator.docker.DockerContainer;
import org.powertac.orchestrator.paths.PathProvider;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class LogProcessorReflectionContainerCreator {

    private final static String containerName = "logprocessor.reflection";
    private final static String containerProcessorsFilePath = "/etc/powertac/processors.json";

    private final DockerClient docker;
    private final PathProvider paths;

    public LogProcessorReflectionContainerCreator(DockerClient docker, PathProvider paths) {
        this.docker = docker;
        this.paths = paths;
    }

    public DockerContainer create(String logProcessorImageId) {
        String containerId = docker.createContainerCmd(logProcessorImageId)
            .withName(containerName)
            .withHostConfig(getHostConfig(logProcessorImageId))
            .withCmd(getCommand())
            .exec().getId();
        return new DockerContainer(containerId, containerName);
    }

    private HostConfig getHostConfig(String imageId) {
        return new HostConfig()
            .withBinds(
                new Bind(
                    paths.host().processorsFile(imageId).toString(),
                    new Volume(containerProcessorsFilePath)));
    }

    private List<String> getCommand() {
        List<String> command = new ArrayList<>();
        command.add("list");
        command.add(containerProcessorsFilePath);
        return command;
    }

}
