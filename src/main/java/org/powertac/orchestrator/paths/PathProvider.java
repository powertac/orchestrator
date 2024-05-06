package org.powertac.orchestrator.paths;

import org.powertac.orchestrator.baseline.Baseline;
import org.powertac.orchestrator.broker.Broker;
import org.powertac.orchestrator.game.Game;
import org.powertac.orchestrator.game.GameRun;
import org.powertac.orchestrator.treatment.Treatment;
import org.powertac.orchestrator.util.Versioned;

import java.nio.file.Path;

public interface PathProvider extends Versioned {

    @Override
    default String getVersion() {
        return "1.1";
    }

    OrchestratorPaths host();
    OrchestratorPaths local();
    ContainerPaths container();

    interface OrchestratorPaths {
        Path config();
        Path processorsFile(String logProcessorImageId);
        Path games();
        Path baselines();
        Path treatments();
        GamePaths game(Game game);
        GameRunPaths run(GameRun run);
        BaselinePaths baseline(Baseline baseline);
        TreatmentPaths treatment(Treatment treatment);

        interface GamePaths {
            Path bootstrap();
            Path dir();
            Path runs();
            Path properties();
            Path seed();
            Path archive();
            Path artifacts();
            Path scope();
            BrokerPaths broker(Broker broker);

            interface BrokerPaths {
                Path properties();
            }
        }

        interface GameRunPaths {
            Path dir();
            Path log();
            Path serverLogs();
            Path state();
            Path trace();
            BrokerPaths broker(Broker broker);

            interface BrokerPaths {
                Path dir();
            }
        }

        interface BaselinePaths {
            Path dir();
            Path artifacts();
            Path manifest();
            Path scope();
        }

        interface TreatmentPaths {
            Path dir();
            Path artifacts();
            Path scope();
        }
    }

    interface ContainerPaths {
        ServerPaths server();
        BrokerPaths broker(Broker broker);

        interface ServerPaths {
            Path base();
            GamePaths game(Game game);
            GameRunPaths run(GameRun run);

            interface GamePaths {
                Path bootstrap();
                Path properties();
                Path seed();
            }

            interface GameRunPaths {
                Path state();
                Path trace();
            }
        }

        interface BrokerPaths {
            Path base();
            GamePaths game(Game game);
            GameRunPaths run(GameRun run);

            interface GamePaths {
                Path properties();
            }

            interface GameRunPaths {
                Path logs();
                Path data();
            }
        }
    }

}
