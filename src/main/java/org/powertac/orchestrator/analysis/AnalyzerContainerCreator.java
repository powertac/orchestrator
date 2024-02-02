package org.powertac.orchestrator.analysis;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import org.powertac.orchestrator.baseline.Baseline;
import org.powertac.orchestrator.docker.ContainerCreator;
import org.powertac.orchestrator.docker.DockerContainer;
import org.powertac.orchestrator.paths.PathProvider;
import org.powertac.orchestrator.treatment.Treatment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class AnalyzerContainerCreator implements ContainerCreator<AnalyzerTask> {

    private final static String containerRootPath = "/opt/powertac/analysis";
    private final static String containerDataPath = containerRootPath + "/data";
    private final static String containerArtifactsPath = containerRootPath + "/artifacts";

    @Value("${services.analysis.default-image}")
    private String defaultImageTag;

    private final DockerClient docker;
    private final PathProvider paths;

    public AnalyzerContainerCreator(DockerClient docker, PathProvider paths) {
        this.docker = docker;
        this.paths = paths;
    }

    @Override
    public DockerContainer createFor(AnalyzerTask task) {
        String containerName = getContainerName(task.getId());
        CreateContainerCmd createContainer = docker.createContainerCmd(defaultImageTag)
            .withName(containerName)
            .withCmd(getCommand());
        if (task.getBaseline() != null) {
            createContainer.withHostConfig(getHostConfig(task.getBaseline()));
        } else {
            createContainer.withHostConfig(getHostConfig(task.getTreatment()));
        }
        String containerId = createContainer.exec().getId();
        return new DockerContainer(containerId, containerName);
    }

    private String getContainerName(String gameId) {
        return String.format("analysis.%s", gameId);
    }

    private HostConfig getHostConfig(Baseline baseline) {
        List<Bind> binds = new ArrayList<>();
        binds.add(new Bind(
            Paths.get(paths.host().baseline(baseline).dir().toString(), String.format("%s.baseline.json", baseline.getId())).toString(),
            new Volume(containerDataPath + "/group.json")));
        binds.add(new Bind(
            paths.host().baseline(baseline).artifacts().toString(),
            new Volume(containerArtifactsPath)));
        baseline.getGames().forEach(g -> binds.add(new Bind(
            paths.host().game(g).artifacts().toString(),
            new Volume(containerDataPath + "/games/" + g.getId())
        )));
        return new HostConfig().withBinds(binds);
    }

    private HostConfig getHostConfig(Treatment treatment) {
        List<Bind> binds = new ArrayList<>();
        binds.add(new Bind(
            Paths.get(paths.host().treatment(treatment).dir().toString(), String.format("%s.treatment.json", treatment.getId())).toString(),
            new Volume(containerDataPath + "/group.json")));
        binds.add(new Bind(
            paths.host().treatment(treatment).artifacts().toString(),
            new Volume(containerArtifactsPath)));
        treatment.getGames().forEach(g -> binds.add(new Bind(
            paths.host().game(g).artifacts().toString(),
            new Volume(containerDataPath + "/games/" + g.getId())
        )));
        return new HostConfig().withBinds(binds);
    }

    private List<String> getCommand() {
        List<String> command = new ArrayList<>();
        command.add("poetry");
        command.add("run");
        command.add("python");
        command.add("powertac/group_wholesale_prices_boxplot.py");
        command.add(containerDataPath + "/group.json");
        return command;
    }

}
