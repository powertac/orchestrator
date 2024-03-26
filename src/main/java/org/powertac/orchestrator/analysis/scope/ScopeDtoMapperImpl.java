package org.powertac.orchestrator.analysis.scope;

import org.powertac.orchestrator.game.GameDTOMapper;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ScopeDtoMapperImpl implements ScopeDtoMapper {

    private final GameDTOMapper gameMapper;

    public ScopeDtoMapperImpl(GameDTOMapper gameMapper) {
        this.gameMapper = gameMapper;
    }

    @Override
    public ScopeDTO toDto(Scope scope) {
        return new ScopeDTO(
            scope.getId(),
            scope.getName(),
            scope.getGames().stream().map(gameMapper::toDTO).collect(Collectors.toList()),
            scope.getScopeType().toString());
    }

}
