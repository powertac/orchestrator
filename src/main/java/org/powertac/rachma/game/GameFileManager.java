package org.powertac.rachma.game;

import org.powertac.rachma.broker.Broker;
import org.powertac.rachma.file.FileRole;

import java.io.IOException;
import java.util.Map;

public interface GameFileManager {

    void createScaffold(Game game) throws IOException;
    void removeAllGameFiles(Game game) throws IOException;
    void createRunScaffold(GameRun run) throws IOException;
    void createSimulationScaffold(GameRun run) throws IOException;
    void createBootstrap(Game game) throws IOException;
    void removeBootstrap(Game game) throws IOException;
    Map<FileRole, String> getFiles(Game game);
    void createServerProperties(Game game) throws IOException;
    void createBrokerProperties(Game game, Broker broker) throws IOException;
}
