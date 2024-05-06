package org.powertac.orchestrator.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Service
public class ApiDrivenDockerImageRepository implements DockerImageRepository {

    private final DockerClient dockerClient;

    @Autowired
    public ApiDrivenDockerImageRepository(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public void pull(String tag) throws DockerException {
        try {
            dockerClient.pullImageCmd(tag)
                .exec(new PullImageResultCallback())
                .awaitCompletion();
        } catch (InterruptedException e) {
            throw new RuntimeException(String.format("unable to pull image '%s'", tag));
        }
    }

    @Override
    public boolean exists(String tag) throws DockerException {
        try {
            dockerClient.inspectImageCmd(tag).exec();
            return true;
        }
        catch (com.github.dockerjava.api.exception.NotFoundException e) {
            return false;
        }
    }

    @Override
    public Collection<DockerImage> findAll() {
        Set<DockerImage> images = new HashSet<>();
        for (Image original : dockerClient.listImagesCmd().exec()) {
            images.addAll(buildImages(original));
        }
        return images;
    }

    @Override
    public DockerImage findByName(String imageName) throws DockerException {
        InspectImageResponse response = dockerClient.inspectImageCmd(imageName).exec();
        return new DockerImage(response.getId(), imageName);
    }

    private boolean isValid(String tag) {
        return !tag.equals("<none>:<none>");
    }

    private Collection<DockerImage> buildImages(Image original) {
        Set<DockerImage> images = new HashSet<>();
        if (null == original.getRepoTags()) {
            return images;
        }
        for (String tag : original.getRepoTags()) {
            if (isValid(tag)) {
                images.add(new DockerImage(
                    original.getId(),
                    tag));
            }
        }
        return images;
    }

}
