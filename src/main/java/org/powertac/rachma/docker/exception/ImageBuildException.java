package org.powertac.rachma.docker.exception;

public class ImageBuildException extends Exception {

    public ImageBuildException(String message) {
        super(message);
    }

    public ImageBuildException(String message, Throwable previousException) {
        super(message, previousException);
    }

}
