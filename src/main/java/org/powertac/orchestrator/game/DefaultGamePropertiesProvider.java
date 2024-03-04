package org.powertac.orchestrator.game;

import org.powertac.orchestrator.broker.Broker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Properties;

@Component
public class DefaultGamePropertiesProvider implements GamePropertiesProvider {

    @Value("${services.weatherserver.url}")
    private String defaultWeatherServerUrl;

    private final DateTimeFormatter baseTimeFormatter;

    public DefaultGamePropertiesProvider() {
        this.baseTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
    }

    @Override
    public Properties getServerProperties(Game game) {
        Properties properties = getDefaultServerProperties();
        for (Map.Entry<String, String> parameter : game.getServerParameters().entrySet()) {
            properties.setProperty(parameter.getKey(), parameter.getValue());
        }
        if (null != game.getWeatherConfiguration()) {
            String baseTime = baseTimeFormatter.format(game.getWeatherConfiguration().getStartTime());
            properties.setProperty("common.competition.simulationBaseTime", baseTime);
            properties.setProperty("server.weatherService.weatherLocation", game.getWeatherConfiguration().getLocation());
        }
        return properties;
    }

    @Override
    public Properties getBrokerProperties(Game game, Broker broker) {
        Properties properties = getDefaultBrokerProperties();
        properties.setProperty("samplebroker.core.powerTacBroker.username", broker.getHumanReadableIdentifier());
        return properties;
    }

    private Properties getDefaultServerProperties() {
        Properties defaultProperties = new Properties();
        defaultProperties.put("server.mode", "research");
        defaultProperties.put("server.competitionControlService.brokerPauseAllowed", "true");
        defaultProperties.put("server.competitionControlService.loginTimeout", "60000");
        defaultProperties.put("server.jmsManagementService.jmsBrokerUrl", "tcp://0.0.0.0:61616");
        defaultProperties.put("server.weatherService.serverUrl", defaultWeatherServerUrl);
        return defaultProperties;
    }

    private Properties getDefaultBrokerProperties() {
        Properties defaultProperties = new Properties();
        defaultProperties.put("samplebroker.core.jmsManagementService.jmsBrokerUrl", "tcp://powertac-server:61616");
        defaultProperties.put("samplebroker.core.powerTacBroker.retryTimeLimit", "60000");
        return defaultProperties;
    }

}
