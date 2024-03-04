package org.powertac.orchestrator.logprocessor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import org.powertac.orchestrator.docker.ContainerCreator;
import org.powertac.orchestrator.docker.DockerContainer;
import org.powertac.orchestrator.game.Game;
import org.powertac.orchestrator.game.GameRun;
import org.powertac.orchestrator.paths.PathProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class LogProcessorContainerCreator implements ContainerCreator<LogProcessorTask> {

    private final static String containerRootPath = "/opt/powertac/log-processor";
    private final static String containerResultsPath = containerRootPath + "/results";

    @Value("${services.logprocessor.default-image}")
    private String defaultImageTag;

    private final DockerClient docker;
    private final PathProvider paths;

    public LogProcessorContainerCreator(DockerClient docker, PathProvider paths) {
        this.docker = docker;
        this.paths = paths;
    }

    @Override
    public DockerContainer createFor(LogProcessorTask task) {
        String containerName = getContainerName(task.getGame().getId());
        String containerId = docker.createContainerCmd(defaultImageTag)
            .withName(containerName)
            .withHostConfig(getHostConfig(task.getGame()))
            .withCmd(getCommand(task))
            .exec().getId();
        return new DockerContainer(containerId, containerName);
    }

    private String getContainerName(String gameId) {
        return String.format("logprocessor.%s", gameId);
    }

    private HostConfig getHostConfig(Game game) {
        Optional<GameRun> run = game.getSuccessfulRun();
        return new HostConfig()
            .withBinds(
                new Bind(
                    paths.host().run(run.get()).state().toString(),
                    new Volume(containerRootPath + "/game.state")),
                new Bind(
                    paths.host().game(game).artifacts().toString(),
                    new Volume(containerResultsPath)));
    }

    private List<String> getCommand(LogProcessorTask task) {
        List<String> command = new ArrayList<>();
        command.add("game.state");
        command.add("--game");
        command.add(task.getGame().getId());
        command.add("--out");
        command.add(containerResultsPath);
        command.add("--processors");
        command.add(String.join(",", task.getProcessorIds()));
        return command;
    }

}
