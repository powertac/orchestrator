package org.powertac.rachma.persistence;

import org.powertac.rachma.game.Game;
import org.powertac.rachma.game.GameRun;
import org.powertac.rachma.game.GameRunPhase;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;

public interface JpaGameRunRepository extends CrudRepository<GameRun, String> {

    Collection<GameRun> findAllByGameAndPhaseBetween(Game game, GameRunPhase start, GameRunPhase end);
    boolean existsByGameAndPhaseAndFailed(Game game, GameRunPhase phase, boolean failed);
    boolean existsByGameAndPhaseNot(Game game, GameRunPhase phase);

}
