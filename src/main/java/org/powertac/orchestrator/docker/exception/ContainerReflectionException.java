package org.powertac.orchestrator.docker.exception;

public class ContainerReflectionException extends ContainerException {

    public ContainerReflectionException(String message) {
        super(message);
    }

    public ContainerReflectionException(String message, Throwable cause) {
        super(message, cause);
    }

}
