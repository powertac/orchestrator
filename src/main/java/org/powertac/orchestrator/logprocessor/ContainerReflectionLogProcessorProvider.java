package org.powertac.orchestrator.logprocessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.powertac.orchestrator.docker.DockerContainer;
import org.powertac.orchestrator.docker.DockerContainerController;
import org.powertac.orchestrator.docker.DockerContainerExitState;
import org.powertac.orchestrator.docker.DockerImageRepository;
import org.powertac.orchestrator.paths.PathProvider;
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.HashSet;
import java.util.Set;

public class ContainerReflectionLogProcessorProvider implements LogProcessorProvider {

    @Value("${services.logprocessor.default-image}")
    private String defaultImageTag;

    private final DockerImageRepository images;
    private final LogProcessorReflectionContainerCreator containerCreator;
    private final DockerContainerController containerController;
    private final PathProvider paths;

    private Set<LogProcessor> availableProcessors;
    private String cachedProcessorsImageId;
    private ObjectMapper objectMapper;

    public ContainerReflectionLogProcessorProvider(DockerImageRepository images,
                                                   LogProcessorReflectionContainerCreator containerCreator,
                                                   DockerContainerController containerController, PathProvider paths) {
        this.images = images;
        this.containerCreator = containerCreator;
        this.containerController = containerController;
        this.paths = paths;
    }

    @Override
    public Set<LogProcessor> getAvailableProcessors() {
        regenerateProcessorCacheIfRequired();
        return new HashSet<>(availableProcessors);
    }

    @Override
    public boolean has(String processorName) {
        return get(processorName) != null;
    }

    @Override
    public LogProcessor get(String processorName) {
        regenerateProcessorCacheIfRequired();
        return availableProcessors.stream()
            .filter(p -> p.getName().equals(processorName))
            .findFirst().orElse(null);
    }

    private synchronized void regenerateProcessorCacheIfRequired() {
        try {
            String currentImageId = images.findByName(defaultImageTag).getId();
            if (currentImageId.equals(cachedProcessorsImageId)) {
                return;
            }
            Path processorsFile = paths.local().processorsFile(currentImageId);
            Files.deleteIfExists(processorsFile); // removing existing file (which shouldn't exist - but mistakes happen)
            Files.createFile(processorsFile); // making sure target file exists; otherwise docker can't mount file to file
            DockerContainer logProcessorContainer = containerCreator.create(currentImageId);
            DockerContainerExitState exit = containerController.run(logProcessorContainer);
            if (exit.isErrorState()) {
                throw new RuntimeException("unable to regenerate processors cache; container exited with code " + exit.getExitCode());
            }
            LogProcessor[] processors = getObjectMapper().readValue(processorsFile.toFile(), LogProcessor[].class);
            availableProcessors = Set.of(processors);
            cachedProcessorsImageId = currentImageId;
        } catch (Exception e) {
            throw new RuntimeException("unable to regenerate processors cache", e);
        }
    }

    private synchronized ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            // objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            SimpleModule deserializationModule = new SimpleModule();
            deserializationModule.addDeserializer(LogProcessor.class, new LogProcessorDeserializer());
            objectMapper.registerModule(deserializationModule);
        }
        return objectMapper;
    }

}
