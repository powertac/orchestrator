package org.powertac.orchestrator.logprocessor;

import org.powertac.orchestrator.game.Game;
import org.powertac.orchestrator.paths.PathProvider;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LogProcessorArtifactRepositoryImpl implements LogProcessorArtifactRepository {

    private final LogProcessorProvider processorProvider;
    private final PathProvider paths;
    private final LogProcessorTaskRepository taskRepository;

    public LogProcessorArtifactRepositoryImpl(LogProcessorProvider processorProvider, PathProvider paths,
                                              LogProcessorTaskRepository taskRepository) {
        this.processorProvider = processorProvider;
        this.paths = paths;
        this.taskRepository = taskRepository;
    }

    @Override
    public Collection<LogProcessorArtifact> findAllByGame(Game game) {
        Collection<LogProcessorArtifact> artifacts = new ArrayList<>();
        Collection<LogProcessorTask> tasks = taskRepository.findAllByGame(game);
        for (LogProcessor processor : processorProvider.getAvailableProcessors()) {
            Collection<LogProcessorTask> processorTasks = tasks.stream()
                .filter(t -> t.getProcessorIds().contains(processor.getName()))
                .toList();
            Path localFilePath = paths.host().game(game).logProcessorArtifact(processor);
            Path hostFilePath = Files.exists(localFilePath) ? paths.host().game(game).logProcessorArtifact(processor) : null;
            artifacts.add(new LogProcessorArtifact(processor, processorTasks, hostFilePath));
        }
        return artifacts;
    }

}
