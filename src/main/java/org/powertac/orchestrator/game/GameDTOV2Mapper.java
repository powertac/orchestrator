package org.powertac.orchestrator.game;

import org.powertac.orchestrator.broker.Broker;
import org.powertac.orchestrator.broker.BrokerDTO;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GameDTOV2Mapper implements GameDTOMapper {

    @Override
    public GameDTO toDTO(Game game) {
        List<GameRunDTO> runDTOs = new HashSet<>(game.getRuns()).stream()
            .map(this::parseGameRunDTO)
            .collect(Collectors.toList());
        return GameDTO.builder()
            .id(game.getId())
            .name(game.getName())
            .config(parseConfigDTO(game))
            .createdAt(game.getCreatedAt().toEpochMilli())
            .cancelled(game.isCancelled())
            .runs(runDTOs)
            .baselineId(game.getBaseline() != null ? game.getBaseline().getId() : null)
            .treatmentId(game.getTreatment() != null ? game.getTreatment().getId() : null)
            .baseGameId(game.getBase() != null ? game.getBase().getId() : null)
            .build();
    }

    private GameConfigDTO parseConfigDTO(Game game) {
        return GameConfigDTO.builder()
            .brokerIds(game.getBrokerSet().getIds())
            .brokers(game.getBrokers().stream().map(this::brokerToDto).collect(Collectors.toSet()))
            .parameters(game.getServerParameters())
            .weather(game.getWeatherConfiguration())
            .seed(null) // FIXME : this should contain reference to game, if applicable
            .serverVersion(game.getServerVersion())
            .build();
    }

    private BrokerDTO brokerToDto(Broker broker) {
        return new BrokerDTO(broker.getId(), broker.getName(), broker.getVersion());
    }

    private GameRunDTO parseGameRunDTO(GameRun run) {
        return GameRunDTO.builder()
            .id(run.getId())
            .start(run.getStart() != null ? run.getStart().toEpochMilli() : null)
            .end(run.getEnd() != null ? run.getEnd().toEpochMilli() : null)
            .phase(run.getPhase().toString())
            .failed(run.hasFailed())
            .build();
    }

}
