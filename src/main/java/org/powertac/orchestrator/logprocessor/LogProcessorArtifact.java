package org.powertac.orchestrator.logprocessor;

import java.nio.file.Path;
import java.util.Collection;

public record LogProcessorArtifact(LogProcessor processor,
                                   Collection<LogProcessorTask> tasks,
                                   Path filePath) {

    public boolean exists() {
        return filePath != null;
    }

}
