package org.powertac.orchestrator.analysis.dtos;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalyzerBrokerDTO {

    private String id;
    private String name;
    private String version;

}
