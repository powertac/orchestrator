package org.powertac.orchestrator.game;

import lombok.Getter;
import org.powertac.orchestrator.weather.WeatherConfiguration;

import java.util.Map;
import java.util.Set;

public class NewGameDTO {

    @Getter
    private String name;

    @Getter
    private Set<String> brokerIds;

    @Getter
    private Map<String, String> parameters;

    @Getter
    private WeatherConfiguration weather;

    @Getter
    private String serverVersionId;

}
