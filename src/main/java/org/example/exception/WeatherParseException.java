package org.example.exception;

/**
 * Thrown when the JSON response from the API cannot be parsed,
 * or a required field is absent / has an unexpected type.
 */
public class WeatherParseException extends WeatherException {

    public WeatherParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public WeatherParseException(String message) {
        super(message);
    }
}
