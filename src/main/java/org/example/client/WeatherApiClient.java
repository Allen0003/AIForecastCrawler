package org.example.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.exception.*;
import org.example.model.WeatherResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Low-level HTTP client responsible for:
 * <ul>
 *   <li>Building the request URL</li>
 *   <li>Sending the HTTP request via Java 17 {@link HttpClient}</li>
 *   <li>Mapping every error scenario to a typed {@link WeatherApiException}</li>
 *   <li>Delegating JSON deserialization to Jackson</li>
 * </ul>
 */
public class WeatherApiClient {

    private static final Logger log = LoggerFactory.getLogger(WeatherApiClient.class);

    /** Base URL for OpenWeatherMap current-weather endpoint. */
    private static final String BASE_URL =
            "https://api.openweathermap.org/data/2.5/weather";

    /** Connect + request timeout — spec requires 5 seconds. */
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * @param apiKey OpenWeatherMap API key (obtain free at openweathermap.org)
     */
    public WeatherApiClient(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("API key is null or blank — cannot initialize WeatherApiClient");
            throw new UnauthorizedApiKeyException(401);
        }
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetches current weather data for the given city name.
     *
     * @param city city name (e.g. "Taipei", "London")
     * @return parsed {@link WeatherResponseDTO}
     * @throws CityNotFoundException        if the city cannot be found (HTTP 400/404)
     * @throws UnauthorizedApiKeyException  if the API key is invalid or rate-limited (HTTP 401/429)
     * @throws ApiTimeoutException          on connection timeout or I/O failure
     * @throws DataParseException           if the response body cannot be deserialized
     */
    public WeatherResponseDTO fetchWeather(String city) {
        String encodedCity = URLEncoder.encode(city.trim(), StandardCharsets.UTF_8);
        String url = BASE_URL
                + "?q="     + encodedCity
                + "&appid=" + apiKey
                + "&units=metric"  // Celsius
                + "&lang=zh_tw";   // Chinese descriptions

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .header("Accept", "application/json")
                .build();

        log.info("Fetching weather for city='{}' from {}", city, BASE_URL);

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            log.error("Network error when contacting the weather API for city='{}': {}", city, e.getMessage(), e);
            throw new ApiTimeoutException(
                    "Network error when contacting the weather API: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Request interrupted for city='{}'", city, e);
            throw new ApiTimeoutException("Request was interrupted.", e);
        }

        int status = response.statusCode();
        String body = response.body();

        log.debug("Received HTTP {} for city='{}'", status, city);

        // ── Map HTTP error codes to typed exceptions ──────────────────────────
        switch (status) {
            case 200 -> { /* fall through to parse */ }
            case 400, 404 -> {
                log.error("City not found (HTTP {}) for city='{}'", status, city);
                throw new CityNotFoundException(city);
            }
            case 401 -> {
                log.error("Unauthorized API key (HTTP 401)");
                throw new UnauthorizedApiKeyException(401);
            }
            case 429 -> {
                log.error("API rate limit exceeded (HTTP 429)");
                throw new UnauthorizedApiKeyException(429);
            }
            default -> {
                log.error("Unexpected API response (HTTP {}) for city='{}'", status, city);
                throw new WeatherApiException("Unexpected API response (HTTP " + status + ").");
            }
        }

        return parseResponse(body, city);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Parses the JSON response body into a {@link WeatherResponseDTO}.
     * Guards against malformed JSON and missing required fields.
     */
    private WeatherResponseDTO parseResponse(String body, String city) {
        WeatherResponseDTO dto;
        try {
            dto = objectMapper.readValue(body, WeatherResponseDTO.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse weather API response for city='{}'. Raw body: {}", city, body, e);
            throw new DataParseException(
                    "Could not parse the weather API response. The format may have changed.", e);
        }

        // Guard required fields
        if (dto.getMain() == null) {
            log.error("Required field 'main' is absent in the API response for city='{}'. Raw body: {}", city, body);
            throw new DataParseException("Required field 'main' is absent in the API response.");
        }
        if (dto.getMain().getTemp() == null) {
            log.error("Required field 'main.temp' is absent in the API response for city='{}'. Raw body: {}", city, body);
            throw new DataParseException("Required field 'main.temp' is absent in the API response.");
        }

        log.info("Successfully parsed weather data for city='{}'", city);
        return dto;
    }
}
