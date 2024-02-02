package org.powertac.orchestrator;

import com.github.dockerjava.api.exception.DockerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.orchestrator.docker.DockerImageRepository;
import org.powertac.orchestrator.exec.Task;
import org.powertac.orchestrator.exec.TaskExecutor;
import org.powertac.orchestrator.exec.TaskScheduler;
import org.powertac.orchestrator.game.GameScheduler;
import org.powertac.orchestrator.paths.PathProvider;
import org.powertac.orchestrator.persistence.SchemaViewSeeder;
import org.powertac.orchestrator.persistence.SeederException;
import org.powertac.orchestrator.persistence.SeederManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class Orchestrator implements ApplicationRunner, ApplicationContextAware {

    @Value("${services.simulationserver.default-image}")
    private String defaultSimulationServerImage;

    private ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Orchestrator.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        runStartupTasks();
        runGames();
        runTasks();
    }

    private void runStartupTasks() throws SeederException, IOException {
        createRequiredDirectories();
        pullRequiredDockerImages();
        seedRequiredDatabaseViews();
        context.getBean(SeederManager.class).runSeeders();
    }

    private void runGames() {
        final GameScheduler scheduler = context.getBean(GameScheduler.class);
        final Logger logger = LogManager.getLogger(Orchestrator.class);
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                scheduler.runGames();
            } catch (InterruptedException e) {
                logger.error(e);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void runTasks() {
        final TaskScheduler taskScheduler = context.getBean(TaskScheduler.class);
        final TaskExecutor<Task> taskExecutor = (TaskExecutor<Task>) context.getBean("taskExecutor");
        final Logger logger = LogManager.getLogger("task runner");
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            Optional<Task> task = taskScheduler.next();
            if (taskExecutor.hasCapacity() && task.isPresent()) {
                if (taskExecutor.accepts(task.get())) {
                    try {
                        taskExecutor.exec(task.get());
                    } catch (Exception e) {
                        logger.error("error during task execution", e);
                    }
                } else {
                    // TODO : set task status to failed when no executor configured
                    logger.error("no executor configured for type " + task.getClass());
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void createRequiredDirectories() throws IOException {
        PathProvider paths = context.getBean(PathProvider.class);
        Path gamesDirectory = paths.local().games();
        if (!Files.exists(gamesDirectory)) {
            Files.createDirectories(gamesDirectory);
        }
    }

    private void pullRequiredDockerImages() throws DockerException {
        DockerImageRepository images = context.getBean(DockerImageRepository.class);
        if (!images.exists(defaultSimulationServerImage)) {
            images.pull(defaultSimulationServerImage);
        }
    }

    private void seedRequiredDatabaseViews() {
        SchemaViewSeeder viewSeeder = context.getBean(SchemaViewSeeder.class);
        viewSeeder.seedViews();
    }

}
