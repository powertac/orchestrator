package org.powertac.orchestrator.game;

import org.powertac.orchestrator.baseline.Baseline;
import org.powertac.orchestrator.broker.BrokerNotFoundException;
import org.powertac.orchestrator.broker.BrokerSet;
import org.powertac.orchestrator.server.SimulationServerVersionNotFoundException;
import org.powertac.orchestrator.weather.WeatherConfiguration;

import java.util.Map;

public interface GameFactory {

    Game createFromDTO(NewGameDTO newGameData) throws BrokerNotFoundException, SimulationServerVersionNotFoundException;
    Game createFromSpec(GameSpec spec);
    Game createGame(String name, BrokerSet brokers, WeatherConfiguration weather, Map<String, String> parameters, Baseline baseline);
    Game createFromConfig(GameConfig config);

}
