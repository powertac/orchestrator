package org.powertac.orchestrator.analysis.scope;

import org.powertac.orchestrator.game.GameDTO;

import java.util.Collection;

public record ScopeDTO(String id,
                       String name,
                       Collection<GameDTO> games,
                       String type) {}
