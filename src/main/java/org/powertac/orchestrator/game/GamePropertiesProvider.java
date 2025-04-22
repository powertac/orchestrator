package org.powertac.orchestrator.game;

import org.powertac.orchestrator.broker.Broker;
import org.powertac.orchestrator.paths.PathProvider;

import java.util.Properties;

public interface GamePropertiesProvider {

    Properties getServerProperties(Game game, PathProvider paths);
    Properties getBrokerProperties(Game game, Broker broker);

}
