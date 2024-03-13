package org.powertac.orchestrator.docker.exception;

public class ContainerConflictException extends ContainerException {

    public ContainerConflictException(String message) {
        super(message);
    }

    public ContainerConflictException(String message, Throwable cause) {
        super(message, cause);
    }

}
