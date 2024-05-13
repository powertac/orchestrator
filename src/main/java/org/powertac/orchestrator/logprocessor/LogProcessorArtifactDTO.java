package org.powertac.orchestrator.logprocessor;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.powertac.orchestrator.exec.PersistentTaskDTO;

import java.util.Collection;

@Getter
@Builder
@AllArgsConstructor
public class LogProcessorArtifactDTO {

    private String processorName;
    @Nullable private String filePath;
    private boolean exists;
    private Collection<PersistentTaskDTO<Object>> tasks;


}
