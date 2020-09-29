package org.powertac.rachma.docker.exception;

public class ImageNotFoundException extends Exception {

    public ImageNotFoundException(String message) {
        super(message);
    }

    public ImageNotFoundException(String message, Throwable previousException) {
        super(message, previousException);
    }

}
