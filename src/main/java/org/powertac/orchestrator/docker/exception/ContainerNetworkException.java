package org.powertac.orchestrator.docker.exception;

public class ContainerNetworkException extends ContainerException {

    public ContainerNetworkException(String message) {
        super(message);
    }

    public ContainerNetworkException(String message, Throwable previousException) {
        super(message, previousException);
    }

}
