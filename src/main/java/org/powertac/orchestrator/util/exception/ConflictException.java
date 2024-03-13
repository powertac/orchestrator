package org.powertac.orchestrator.util.exception;

public class ConflictException extends Exception {

    public ConflictException(Throwable cause) {
        super(cause);
    }

    public ConflictException(String message) {
        super(message);
    }

}
