package org.example.exception;

/**
 * Thrown when a network-level failure occurs, such as a connection timeout,
 * DNS resolution failure, or unexpected socket closure.
 */
public class NetworkException extends WeatherException {

    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
