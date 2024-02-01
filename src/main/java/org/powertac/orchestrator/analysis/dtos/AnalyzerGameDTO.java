package org.powertac.orchestrator.analysis.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class AnalyzerGameDTO {

    private String id;
    private String name;
    @JsonProperty("file_root")
    private String fileRoot;
    private Set<AnalyzerBrokerDTO> brokers;

}
