package org.powertac.orchestrator.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import org.apache.commons.lang3.NotImplementedException;
import org.powertac.orchestrator.docker.exception.ContainerConflictException;

import java.util.Collection;
import java.util.Optional;

@Deprecated
public class ApiDrivenDockerContainerRepository implements DockerContainerRepository {

    private final DockerClient docker;

    public ApiDrivenDockerContainerRepository(DockerClient docker) {
        this.docker = docker;
    }

    @Override
    public Optional<DockerContainer> findById(String id) throws DockerException {
        try {
            InspectContainerResponse response = docker.inspectContainerCmd(id).exec();
            return Optional.of(new DockerContainer(id, response.getName()));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<DockerContainer> findByName(String name) {
        throwDeprecationNotice();
        return null;
    }

    @Override
    public Collection<DockerContainer> findAll() {
        throwDeprecationNotice();
        return null;
    }

    @Override
    public void remove(DockerContainer container) {
        throwDeprecationNotice();
    }

    @Override
    public DockerContainer add(DockerContainer container) throws ContainerConflictException {
        throwDeprecationNotice();
        return null;
    }

    @Override
    public boolean existsById(String id) {
        throwDeprecationNotice();
        return false;
    }

    @Override
    public boolean existsByName(String name) {
        throwDeprecationNotice();
        return false;
    }

    @Override
    public DockerContainer update(DockerContainer container) {
        throwDeprecationNotice();
        return null;
    }

    @Override
    public DockerContainer run(DockerContainer container) {
        throwDeprecationNotice();
        return null;
    }

    private void throwDeprecationNotice() {
        throw new NotImplementedException(this.getClass() + " is deprecated; use " + ManagedDockerContainerRepository.class + " instead");
    }

}
