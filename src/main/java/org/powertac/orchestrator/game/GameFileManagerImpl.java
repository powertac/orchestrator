package org.powertac.orchestrator.game;

import org.powertac.orchestrator.broker.Broker;
import org.powertac.orchestrator.file.FileRole;
import org.powertac.orchestrator.file.FileWriter;
import org.powertac.orchestrator.paths.PathProvider;
import org.powertac.orchestrator.util.BrokerCompatiblePropertiesWriter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GameFileManagerImpl implements GameFileManager {

    private final PathProvider paths;
    private final FileWriter fileWriter;
    private final GamePropertiesProvider properties;

    public GameFileManagerImpl(PathProvider paths, FileWriter fileWriter, GamePropertiesProvider properties) {
        this.paths = paths;
        this.fileWriter = fileWriter;
        this.properties = properties;
    }

    @Override
    public void createScaffold(Game game) throws IOException { // TODO : too fuzzy -> refactor
        createGameDirectory(game);
        createServerProperties(game);
        for (Broker broker : game.getBrokers()) {
            createBrokerProperties(game, broker);
        }
    }

    @Override
    public void removeAllGameFiles(Game game) throws IOException {
        Path gameDirectory = paths.local().game(game).dir();
        if (Files.exists(gameDirectory)) {
            List<Path> files = Files.walk(gameDirectory)
                // the order is important here; files must be removed before removing their parent directory
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
            for (Path file : files) {
                Files.deleteIfExists(file);
            }
        }
    }

    @Override
    public void createRunScaffold(GameRun run) throws IOException { // TODO : too fuzzy -> refactor
        fileWriter.createDirectoryIfNotExists(paths.local().game(run.getGame()).runs());
        fileWriter.createDirectoryIfNotExists(paths.local().run(run).dir());
        fileWriter.createFileIfNotExists(paths.local().run(run).log());
    }

    @Override
    public void createSimulationScaffold(GameRun run) throws IOException { // TODO : too fuzzy -> refactor
        fileWriter.createDirectoryIfNotExists(paths.local().run(run).serverLogs());
        fileWriter.createFileIfNotExists(paths.local().run(run).state());
        fileWriter.createFileIfNotExists(paths.local().run(run).trace());
    }

    @Override
    public void createBootstrap(Game game) throws IOException {
        Path bootstrapPath = paths.local().game(game).bootstrap();
        fileWriter.createFileIfNotExists(bootstrapPath);
    }

    @Override
    public void removeBootstrap(Game game) throws IOException {
        Path bootstrapPath = paths.local().game(game).bootstrap();
        Files.deleteIfExists(bootstrapPath);
    }

    @Override
    public Map<FileRole, String> getFiles(Game game) {
        Map<FileRole, String> files = new HashMap<>();
        if (Files.exists(paths.local().game(game).properties())) {
            files.put(FileRole.PROPERTIES, paths.host().game(game).properties().toString());
        }
        if (Files.exists(paths.local().game(game).bootstrap())) {
            files.put(FileRole.BOOTSTRAP, paths.host().game(game).bootstrap().toString());
        }
        if (null != game.getSeed() && Files.exists(paths.local().game(game).seed())) {
            files.put(FileRole.SEED, paths.host().game(game).seed().toString());
        }
        return files;
    }

    @Override
    public void createServerProperties(Game game) throws IOException {
        Path propertiesPath = paths.local().game(game).properties();
        BrokerCompatiblePropertiesWriter.write(
            propertiesPath.toString(),
            properties.getServerProperties(game));
    }

    @Override
    public void createBrokerProperties(Game game, Broker broker) throws IOException {
        Path propertiesPath = paths.local().game(game).broker(broker).properties();
        BrokerCompatiblePropertiesWriter.write(
            propertiesPath.toString(),
            properties.getBrokerProperties(game, broker));
    }

    private void createGameDirectory(Game game) throws IOException {
        fileWriter.createDirectoryIfNotExists(paths.local().game(game).dir());
    }

}
