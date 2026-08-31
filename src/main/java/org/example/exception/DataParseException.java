package org.example.exception;

/**
 * Thrown when the JSON response from the API cannot be parsed,
 * or a required field is absent / has an unexpected type.
 */
public class DataParseException extends WeatherApiException {

    public DataParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataParseException(String message) {
        super(message);
    }
}
