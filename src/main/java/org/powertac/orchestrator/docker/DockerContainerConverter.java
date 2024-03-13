package org.powertac.orchestrator.docker;

import com.github.dockerjava.api.exception.DockerException;
import org.apache.logging.log4j.LogManager;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Component
@Converter
public class DockerContainerConverter implements AttributeConverter<DockerContainer, String> {

    private final DockerContainerRepository containerRepository;

    public DockerContainerConverter(DockerContainerRepository containerRepository) {
        this.containerRepository = containerRepository;
    }

    @Override
    public String convertToDatabaseColumn(DockerContainer dockerContainer) {
        return dockerContainer.getId();
    }

    @Override
    @Nullable
    public DockerContainer convertToEntityAttribute(String id) {
        try {
            return containerRepository.findById(id).orElse(null);
        } catch (DockerException e) {
            LogManager.getLogger(DockerContainerConverter.class).warn("could not find container for id=" + id);
            return null;
        }
    }

}
