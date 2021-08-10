package org.powertac.rachma.game;

import java.util.Collection;

public interface GameRepository {

    Collection<Game> findAll();
    Game findById(String id);
    Game findFirstQueued();
    void save(Game game);
}