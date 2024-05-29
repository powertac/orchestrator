package org.powertac.orchestrator.game;

import org.powertac.orchestrator.baseline.Baseline;
import org.powertac.orchestrator.broker.Broker;
import org.powertac.orchestrator.broker.BrokerNotFoundException;
import org.powertac.orchestrator.broker.BrokerRepository;
import org.powertac.orchestrator.broker.BrokerSet;
import org.powertac.orchestrator.file.File;
import org.powertac.orchestrator.file.FileRole;
import org.powertac.orchestrator.server.SimulationServerVersion;
import org.powertac.orchestrator.server.SimulationServerVersionNotFoundException;
import org.powertac.orchestrator.server.SimulationServerVersionRepository;
import org.powertac.orchestrator.util.ID;
import org.powertac.orchestrator.weather.WeatherConfiguration;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class GameFactoryImpl implements GameFactory {

    private final GameRepository gameRepository;
    private final BrokerRepository brokerRepository;
    private final SimulationServerVersionRepository serverVersionRepository;

    public GameFactoryImpl(GameRepository gameRepository, BrokerRepository brokerRepository,
                           SimulationServerVersionRepository serverVersionRepository) {
        this.gameRepository = gameRepository;
        this.brokerRepository = brokerRepository;
        this.serverVersionRepository = serverVersionRepository;
    }

    @Override
    public Game createFromDTO(NewGameDTO newGameData) throws BrokerNotFoundException, SimulationServerVersionNotFoundException {
        Set<Broker> brokers = brokerIdsToSet(newGameData.getBrokerIds());
        Optional<SimulationServerVersion> serverVersion = serverVersionRepository.findById(newGameData.getServerVersionId());
        if (serverVersion.isEmpty()) {
            throw new SimulationServerVersionNotFoundException("no server version found for id=" + newGameData.getServerVersionId());
        }
        return Game.builder()
            .id(ID.gen())
            .name(newGameData.getName())
            .brokerSet(createBrokerSet(brokers))
            .weatherConfiguration(newGameData.getWeather())
            .serverParameters(newGameData.getParameters())
            .serverVersion(serverVersion.get())
            .createdAt(Instant.now())
            .build();
    }

    @Override
    @Deprecated
    public Game createFromSpec(GameSpec spec) {
        return Game.builder()
            .id(ID.gen())
            .name(spec.getName())
            .brokerSet(createBrokerSet(spec.getBrokers()))
            .serverParameters(spec.getServerParameters())
            .bootstrap(createFile(spec.getBaseGameId(), FileRole.BOOTSTRAP))
            .seed(createFile(spec.getBaseGameId(), FileRole.SEED))
            .weatherConfiguration(spec.getWeather())
            .createdAt(Instant.now())
            .build();
    }

    @Override
    public Game createGame(String name, BrokerSet brokers, WeatherConfiguration weather, Map<String, String> parameters, Baseline baseline) {
        return Game.builder()
            .id(UUID.randomUUID().toString())
            .name(name)
            .brokerSet(brokers)
            .serverParameters(parameters)
            .createdAt(Instant.now())
            .weatherConfiguration(weather)
            .baseline(baseline)
            .build();
    }

    @Override
    public Game createFromConfig(GameConfig config) {
        return Game.builder()
            .id(ID.gen())
            .brokerSet(config.getBrokers())
            .serverParameters(config.getParameters())
            .weatherConfiguration(config.getWeather())
            .serverVersion(config.getServerVersion())
            .createdAt(Instant.now())
            .build();
    }

    // TODO : use file repository
    // TODO : should throw not found exception
    private File createFile(String baseGameId, FileRole role) {
        if (null == baseGameId) {
            return null;
        }
        Game baseGame = gameRepository.findById(baseGameId);
        return new File(ID.gen(), role, baseGame, "", new HashSet<>());
    }

    // TODO : use broker set repo
    private BrokerSet createBrokerSet(Set<Broker> brokers) {
        return new BrokerSet(
            UUID.randomUUID().toString(),
            new HashSet<>(brokers));
    }

    private Set<Broker> brokerIdsToSet(Set<String> ids) throws BrokerNotFoundException {
        Set<Broker> brokers = new HashSet<>();
        for (String id : ids) {
            Optional<Broker> broker = brokerRepository.findById(id);
            if (broker.isPresent()) {
                brokers.add(broker.get());
            } else {
                throw new BrokerNotFoundException(String.format("could not find broker with id=%s", id));
            }
        }
        return brokers;
    }

    private WeatherConfiguration copyWeatherConfig(WeatherConfiguration config) {
        return new WeatherConfiguration(config.getLocation(), config.getStartTime());
    }

}
