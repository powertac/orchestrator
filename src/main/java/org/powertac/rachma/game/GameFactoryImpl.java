package org.powertac.rachma.game;

import org.powertac.rachma.file.File;
import org.powertac.rachma.file.FileRole;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

@Component
public class GameFactoryImpl implements GameFactory {

    private final GameRepository gameRepository;

    public GameFactoryImpl(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public Game createFromSpec(GameSpec spec) {
        return new Game(
            UUID.randomUUID().toString(),
            spec.getName(),
            spec.getBrokers(),
            spec.getServerParameters(),
            getFile(spec.getBaseGameId(), FileRole.BOOTSTRAP),
            getFile(spec.getBaseGameId(), FileRole.SEED),
            Instant.now(),
            new ArrayList<>(),
            false);
    }

    // TODO : use file repository
    // TODO : should throw not found exception
    private File getFile(String baseGameId, FileRole role) {
        if (null == baseGameId) {
            return null;
        }
        Game baseGame = gameRepository.findById(baseGameId);
        return new File(UUID.randomUUID().toString(), role, baseGame);
    }



}
