package org.powertac.orchestrator.docker.exception;

public class ContainerCreationException extends ContainerException {

    public ContainerCreationException(String message) {
        super(message);
    }

    public ContainerCreationException(String message, Throwable previousException) {
        super(message, previousException);
    }

}
