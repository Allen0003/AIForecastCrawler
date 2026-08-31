package org.example.exception;

/**
 * Thrown when the API key is missing, invalid (HTTP 401),
 * or the request rate limit has been exceeded (HTTP 429).
 */
public class UnauthorizedApiKeyException extends WeatherApiException {

    private final int httpStatus;

    public UnauthorizedApiKeyException(int httpStatus) {
        super(buildMessage(httpStatus));
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    private static String buildMessage(int status) {
        return switch (status) {
            case 401 -> "API key is invalid or missing. Please verify your OPENWEATHER_API_KEY.";
            case 429 -> "API rate limit exceeded. Please wait before making another request.";
            default  -> "Authentication / authorization error (HTTP " + status + ").";
        };
    }
}
