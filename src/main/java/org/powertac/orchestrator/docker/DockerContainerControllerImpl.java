package org.powertac.orchestrator.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.orchestrator.docker.exception.ContainerException;
import org.powertac.orchestrator.docker.exception.ContainerReflectionException;
import org.powertac.orchestrator.docker.exception.ContainerStartException;
import org.powertac.orchestrator.docker.exception.KillContainerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Service
public class DockerContainerControllerImpl implements DockerContainerController {

    private final static int runPoolSize = 10;
    private final static int pollingIntervalMillis = 1000;
    private final static int killTimeoutMillis = 30000;

    private final DockerClient docker;

    private final ExecutorService runPool;
    private final Logger logger;

    @Autowired
    public DockerContainerControllerImpl(DockerClient docker) {
        this.docker = docker;
        runPool = Executors.newFixedThreadPool(runPoolSize);
        logger = LogManager.getLogger(DockerContainerControllerImpl.class);
    }

    @Override
    public void start(DockerContainer container) throws ContainerStartException {
        try {
            docker.startContainerCmd(container.getId()).exec();
        }
        catch (DockerException e) {
            logger.error("could not start container with name=" + container.getName());
            throw new ContainerStartException("could not start container with name=" + container.getName());
        }
    }

    @Override
    public void kill(DockerContainer container) throws KillContainerException {
        try {
            docker.killContainerCmd(container.getId()).exec();
        }
        catch (DockerException e) {
            // TODO : kill should always return without exception; should an exception occur it should be logged and
            //        the respective container should be added to the list of orphaned resources
            throw new KillContainerException("could not kill container with name=" + container.getName());
        }
    }

    @Override
    public DockerContainerExitState run(DockerContainer container) throws ContainerException {
        return run(container, (exitState) -> {});
    }

    @Override
    public DockerContainerExitState run(DockerContainer container, Consumer<DockerContainerExitState> onComplete) throws ContainerException {
        try {
            Future<DockerContainerExitState> exitFuture = runPool.submit(createSynchronousRun(container));
            DockerContainerExitState exitState = exitFuture.get();
            // todo - move resource handling up!
            onComplete.accept(exitState);
            return exitState;
        } catch (Exception e) {
            kill(container);
            throw new ContainerException("container run failed", e);
        } finally {
            // todo - move resource handling up!
            remove(container);
        }
    }

    @Override
    public Map<DockerContainer, DockerContainerExitState> run(Set<DockerContainer> containers) throws ContainerException {
        try {
            CompletionService<DockerContainerExitState> completionService = new ExecutorCompletionService<>(runPool);
            Map<Future<DockerContainerExitState>, DockerContainer> containerMap = startSynchronousSet(completionService, containers);
            return getContainerExitStates(completionService, containerMap);
        } catch (InterruptedException|ExecutionException e) {
            for (DockerContainer container : containers) {
                kill(container);
            }
            throw new ContainerException("synchronous container run failed", e);
        } finally {
            for (DockerContainer container : containers) {
                remove(container);
            }
        }
    }

    @Override
    public boolean isRunning(DockerContainer container) throws ContainerReflectionException {
        return isRunningState(getState(container));
    }

    @Override
    public void remove(DockerContainer container) throws DockerException {
        remove(container.getId(), false);
    }

    @Override
    public void forceRemove(DockerContainer container) throws DockerException {
        remove(container.getId(), true);
    }

    @Override
    public void forceRemove(String name) throws DockerException {
        remove(name, true);
    }

    @Override
    public boolean exists(String name) throws DockerException {
        try {
            docker.inspectContainerCmd(name).exec();
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    public void copyResource(String containerId, String containerPath, String hostPath) throws IOException {
        CopyArchiveFromContainerCmd copyCmd = docker.copyArchiveFromContainerCmd(containerId, containerPath);
        try (
            InputStream in = copyCmd.exec();
            InputStreamReader reader = new InputStreamReader(in);
            BufferedReader buffReader = new BufferedReader(reader);
            BufferedWriter out = Files.newBufferedWriter(Paths.get(hostPath));
        ) {
            String line;
            while ((line = buffReader.readLine()) != null) {
                out.write(line);
                out.newLine();
            }
        }
    }

    private Map<Future<DockerContainerExitState>, DockerContainer> startSynchronousSet(CompletionService<DockerContainerExitState> completionService, Set<DockerContainer> containers) {
        Map<Future<DockerContainerExitState>, DockerContainer> exitFutures = new HashMap<>();
        for (DockerContainer container : containers) {
            exitFutures.put(completionService.submit(createSynchronousRun(container)), container);
        }
        return exitFutures;
    }

    private Map<DockerContainer, DockerContainerExitState> getContainerExitStates(CompletionService<DockerContainerExitState> completionService, Map<Future<DockerContainerExitState>, DockerContainer> containerMap)
            throws InterruptedException, ExecutionException {
        boolean completed = false;
        Thread killTimer = null;
        Map<DockerContainer, DockerContainerExitState> exitStates = new HashMap<>();
        while (!completed) {
            Future<DockerContainerExitState> future = completionService.take();
            exitStates.put(containerMap.get(future), future.get());
            // set kill timer once the first container finished its run;
            // after the timeout all remaining running containers will be shut down (killed)
            if (null == killTimer) {
                // FIXME : kill server as well !!!
                killTimer = new Thread(getKillTimer(containerMap.values()));
                killTimer.start();
            }
            completed = exitStates.size() == containerMap.size();
            // TODO : add Thread sleep interval
        }
        killTimer.interrupt();
        return exitStates;
    }

    private Callable<DockerContainerExitState> createSynchronousRun(DockerContainer container) {
        return () -> {
            start(container);
            while (true) {
                InspectContainerResponse.ContainerState state = getState(container);
                if (!isRunningState(state)) {
                    int exitCode = getExitCode(state);
                    if (exitCode != 0) {
                        logger.error(String.format("container '%s' exited with non-null code %s", container.getName(), exitCode));
                    }
                    return new DockerContainerExitState(
                        getExitCode(state),
                        state.getFinishedAt());
                }
                Thread.sleep(pollingIntervalMillis);
            }
        };
    }

    private InspectContainerResponse.ContainerState getState(DockerContainer container) throws ContainerReflectionException {
        try {
            InspectContainerResponse inspection = docker.inspectContainerCmd(container.getId()).exec();
            InspectContainerResponse.ContainerState state = inspection.getState();
            if (null == state) {
                throw new ContainerReflectionException("failed to get state of container with id=" + container.getId());
            }
            return state;
        } catch (DockerException e) {
            throw new ContainerReflectionException("failed to get inspection response for container with id=" + container.getId(), e);
        }
    }

    private boolean isRunningState(InspectContainerResponse.ContainerState state) throws ContainerReflectionException {
        Boolean isRunning = state.getRunning();
        if (null == isRunning) {
            throw new ContainerReflectionException("failed to get container run state");
        }
        return isRunning;
    }

    private int getExitCode(InspectContainerResponse.ContainerState state) throws ContainerReflectionException {
        Long exitCode = state.getExitCodeLong();
        if (exitCode == null) {
            throw new ContainerReflectionException("failed to get container exit state");
        }
        return exitCode.intValue();
    }

    private Runnable getKillTimer(Collection<DockerContainer> containers) {
        return () -> {
            try {
                Thread.sleep(killTimeoutMillis);
                for (DockerContainer container : containers) {
                    try {
                        kill(container);
                    } catch (KillContainerException e) {
                        logger.error(String.format("failed to kill container[id=%s]", container.getId()), e);
                    }
                }
            } catch (InterruptedException e) {
                logger.error("kill timer was interrupted; this may lead to an inconsistent application state", e);
            }
        };
    }

    private void remove(String containerReference, boolean withForce) throws DockerException {
        docker.removeContainerCmd(containerReference)
            .withForce(withForce)
            .withRemoveVolumes(true)
            .exec();
    }

    private String getLog(DockerContainer container) throws ContainerReflectionException {
        try {
            StringBuilder logBuilder = new StringBuilder();
            ResultCallbackTemplate<ResultCallback<Frame>, Frame> cb = new ResultCallbackTemplate<>() {
                @Override public void onNext(Frame frame) {
                    logBuilder.append(new String(frame.getPayload()));
                }
            };
            docker.logContainerCmd(container.getId())
                .withStdOut(true)
                .withStdErr(true)
                .exec(cb)
                .awaitCompletion();
            return logBuilder.toString();
        } catch (InterruptedException e) {
            logger.error("could not read log for container " + container.getName(), e);
            throw new ContainerReflectionException("could not read log for container " + container.getName(), e);
        }
    }

}
