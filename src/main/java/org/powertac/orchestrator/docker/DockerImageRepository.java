package org.powertac.orchestrator.docker;

import com.github.dockerjava.api.exception.DockerException;

import java.util.Collection;

public interface DockerImageRepository {

    void pull(String tag) throws DockerException;
    boolean exists(String tag) throws DockerException;
    Collection<DockerImage> findAll();
    DockerImage findByName(String imageName) throws DockerException;

}
