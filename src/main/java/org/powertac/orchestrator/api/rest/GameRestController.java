package org.powertac.orchestrator.api.rest;

import org.powertac.orchestrator.game.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

@RestController
@RequestMapping("/games")
public class GameRestController {

    @Value("${application.api.default-page-size}")
    private int defaultPageSize;

    private final GameRepository games;
    private final GameRunRepository gameRuns;
    private final GameFactory gameFactory;
    private final GameValidator validator;
    private final GameFileManager gameFileManager;

    public GameRestController(GameRepository games, GameRunRepository gameRuns, GameFactory gameFactory, GameValidator validator, GameFileManager gameFileManager) {
        this.games = games;
        this.gameRuns = gameRuns;
        this.gameFactory = gameFactory;
        this.validator = validator;
        this.gameFileManager = gameFileManager;
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getGameCount() {
        return ResponseEntity.ok().body(games.count());
    }

    @GetMapping("/")
    public ResponseEntity<Collection<Game>> getGames(@RequestParam(required = false) Integer start, @RequestParam(required = false) Integer limit) {
        Collection<Game> top = this.games.findTop(
            start != null && start > 0 ? start : 0,
            limit != null ? limit : defaultPageSize);
        return ResponseEntity.ok().body(top);
    }

    @Deprecated
    @PostMapping("/")
    public ResponseEntity<?> createGame(@RequestBody GameSpec spec) {
        try {
            Game game = gameFactory.createFromSpec(spec);
            validator.validate(game);
            createGameFileScaffold(game); // FIXME : move to GameRunner (JIT-approach)
            games.save(game);
            return ResponseEntity.ok().build();
        } catch (IOException|GameValidationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Game> getGame(@PathVariable("id") String id) {
        Game game = this.games.findById(id);
        if (null != game) {
            return ResponseEntity.ok().body(game);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/rerun")
    @Transactional
    public ResponseEntity<?> rerunGame(@PathVariable("id") String id) {
        Game game = this.games.findById(id);
        if (null == game) {
            return ResponseEntity.notFound().build();
        } else if (game.isRunning()) {
            return ResponseEntity.badRequest().build();
        } else {
            // TODO : this doesn't work in conjunction with run logs in UI
            // TODO : workaround -> tell in UI that runs will be removed
            gameRuns.delete(game.getRuns());
            game.setRuns(new ArrayList<>());
            this.games.save(game);
            return ResponseEntity.ok().build();
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteGame(@PathVariable("id") String id) {
        Game game = this.games.findById(id);
        if (null == game) {
            return ResponseEntity.notFound().build();
        } else if (game.isRunning()) {
            return ResponseEntity.badRequest().build();
        } else {
            try {
                if (null != game.getBaseline()) {
                    // TODO : add message
                    return ResponseEntity.badRequest().build();
                }
                gameFileManager.removeAllGameFiles(game);
                games.delete(game);
                return ResponseEntity.ok().build();
            } catch (IOException e) {
                return ResponseEntity.status(500).build();
            }
        }
    }

    private void createGameFileScaffold(Game game) throws IOException { // FIXME : move to GameRunner (JIT-approach)
        try {
          gameFileManager.createScaffold(game);
        } catch (IOException e) {
            gameFileManager.removeAllGameFiles(game);
            throw e;
        }
    }

}
