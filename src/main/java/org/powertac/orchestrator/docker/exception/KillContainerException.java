package org.powertac.orchestrator.docker.exception;

public class KillContainerException extends ContainerException {

    public KillContainerException(String message) {
        super(message);
    }

    public KillContainerException(String message, Throwable cause) {
        super(message, cause);
    }

}


