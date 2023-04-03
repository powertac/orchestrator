package org.powertac.rachma.api.rest.v2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.rachma.broker.BrokerNotFoundException;
import org.powertac.rachma.file.FileNode;
import org.powertac.rachma.file.FileTreeBuilder;
import org.powertac.rachma.game.*;
import org.powertac.rachma.paths.PathProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v2/games")
public class GameRestControllerV2 {

    private final GameRepository games;
    private final GameFactory gameFactory;
    private final GameValidator validator;
    private final GameFileManager gameFileManager;
    private final GameDTOMapper gameMapper;
    private final FileTreeBuilder fileTreeBuilder;
    private final PathProvider paths;
    private final Logger logger;

    public GameRestControllerV2(GameRepository games,
                                GameFactory gameFactory,
                                GameValidator validator,
                                GameFileManager gameFileManager,
                                GameDTOMapper mapper, FileTreeBuilder fileTreeBuilder, PathProvider paths) {
        this.games = games;
        this.gameFactory = gameFactory;
        this.validator = validator;
        this.gameFileManager = gameFileManager;
        this.gameMapper = mapper;
        this.fileTreeBuilder = fileTreeBuilder;
        this.paths = paths;
        logger = LogManager.getLogger(GameRestControllerV2.class);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gameMapper.toDTO(games.findById(id)));
        } catch (Exception e) {
            logger.error("could not load game " + id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/")
    public ResponseEntity<?> findMany() {
        try {
            return ResponseEntity.ok(games.findAll().stream()
                .map(gameMapper::toDTO)
                .collect(Collectors.toList()));
        } catch (Exception e) {
            logger.error("could not load games", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/")
    public ResponseEntity<?> create(@RequestBody NewGameDTO newGameData) {
        try {
            Game game = gameFactory.createFromDTO(newGameData);
            validator.validate(game);
            createGameFileScaffold(game); // FIXME : move to GameRunner (JIT-approach)
            games.save(game);
            return ResponseEntity.ok().build();
        } catch (IOException | GameValidationException | BrokerNotFoundException e) {
            logger.error("unable to create new game", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/file-root")
    public ResponseEntity<FileNode> getRootNode(@PathVariable String id) {
        try {
            Game game = games.findById(id);
            Path gameRoot = paths.host().game(game).dir();
            return ResponseEntity.ok(fileTreeBuilder.build(gameRoot));
        } catch (Exception e) {
            logger.error("unable to deliver file root node for game with id=" + id, e);
            return ResponseEntity.internalServerError().build();
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
