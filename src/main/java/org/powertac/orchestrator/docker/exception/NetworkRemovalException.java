package org.powertac.orchestrator.docker.exception;

public class NetworkRemovalException extends ContainerNetworkException {

    public NetworkRemovalException(String message) {
        super(message);
    }

    public NetworkRemovalException(String message, Throwable previousException) {
        super(message, previousException);
    }

}
