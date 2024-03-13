package org.powertac.orchestrator.docker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.powertac.orchestrator.user.domain.User;

@Builder
@AllArgsConstructor
public class DockerContainer {

    @Getter
    private final String id;

    @Getter
    private final String name;

    @Getter
    @Setter
    private User owner;

    @Getter
    @Setter
    private DockerContainerState lastKnownState;

    // for backwards compatibility; use builder instead
    @Deprecated
    public DockerContainer(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public boolean isRunning() {
        if (lastKnownState.equals(DockerContainerState.UNKNOWN)) {
            throw new RuntimeException("unable to determine state of container with id=" + id);
        }
        return lastKnownState.equals(DockerContainerState.RUNNING);
    }

    public boolean canBeStarted() {
        if (lastKnownState.equals(DockerContainerState.UNKNOWN)) {
            throw new RuntimeException("unable to determine state of container with id=" + id);
        }
        return !lastKnownState.equals(DockerContainerState.NONE)
            && !lastKnownState.equals(DockerContainerState.DEAD);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DockerContainer container = (DockerContainer) o;
        if (!id.equals(container.id)) return false;
        return name.equals(container.name);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}
