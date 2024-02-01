package org.powertac.orchestrator.analysis.dtos;

import lombok.Builder;
import lombok.Getter;
import org.powertac.orchestrator.analysis.AnalyzerGameGroupType;

import java.util.Set;

@Getter
@Builder
public class AnalyzerGameGroupDTO {

    private String id;
    private String name;
    private AnalyzerGameGroupType type;
    private Set<AnalyzerGameDTO> games;

}
