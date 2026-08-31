package org.example.exception;

/**
 * Thrown when a network-level failure occurs, such as a connection timeout,
 * DNS resolution failure, or unexpected socket closure.
 */
public class ApiTimeoutException extends WeatherApiException {

    public ApiTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiTimeoutException(String message) {
        super(message);
    }
}
