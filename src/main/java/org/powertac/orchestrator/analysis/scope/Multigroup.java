package org.powertac.orchestrator.analysis.scope;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.powertac.orchestrator.game.Game;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
public class Multigroup implements Scope {

    @Getter
    private String id;

    @Getter
    private final Collection<GameGroup> groups;

    @Override
    public Collection<Game> getGames() {
        final Set<Game> games = new HashSet<>();
        getGroups().stream()
            .map(GameGroup::getGames)
            .forEach(games::addAll);
        return games;
    }

}
