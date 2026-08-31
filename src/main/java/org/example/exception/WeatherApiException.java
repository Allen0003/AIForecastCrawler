package org.example.exception;

/**
 * Base exception for all weather-related errors.
 * All domain-specific exceptions extend this class so callers can catch
 * either at a fine-grained level or broadly with a single catch block.
 */
public class WeatherApiException extends RuntimeException {

    public WeatherApiException(String message) {
        super(message);
    }

    public WeatherApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
