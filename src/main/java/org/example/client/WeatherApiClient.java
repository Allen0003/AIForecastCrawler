package org.example.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.exception.*;
import org.example.model.WeatherResponse;

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
 *   <li>Mapping every error scenario to a typed {@link WeatherException}</li>
 *   <li>Delegating JSON deserialization to Jackson</li>
 * </ul>
 *
 * This class is intentionally thin — it knows nothing about business rules;
 * those live in {@link org.example.service.WeatherService}.
 */
public class WeatherApiClient {

    /** Base URL for OpenWeatherMap current-weather endpoint. */
    private static final String BASE_URL =
            "https://api.openweathermap.org/data/2.5/weather";

    /** Connect + request timeout — prevents indefinite hangs. */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * @param apiKey OpenWeatherMap API key (obtain free at openweathermap.org)
     */
    public WeatherApiClient(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiAuthException(401);
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
     * @return parsed {@link WeatherResponse}
     * @throws CityNotFoundException  if the city cannot be found (HTTP 400/404)
     * @throws ApiAuthException       if the API key is invalid or rate-limited (HTTP 401/429)
     * @throws NetworkException       on connection timeout or I/O failure
     * @throws WeatherParseException  if the response body cannot be deserialized
     */
    public WeatherResponse fetchWeather(String city) {
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

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            // Covers: ConnectException, SocketTimeoutException, UnknownHostException, etc.
            throw new NetworkException(
                    "Network error when contacting the weather API: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupted flag
            throw new NetworkException("Request was interrupted.", e);
        }

        int status = response.statusCode();
        String body = response.body();

        // ── Map HTTP error codes to typed exceptions ──────────────────────────
        switch (status) {
            case 200 -> { /* fall through to parse */ }
            case 400, 404 -> throw new CityNotFoundException(city);
            case 401       -> throw new ApiAuthException(401);
            case 429       -> throw new ApiAuthException(429);
            default        -> throw new WeatherException(
                                    "Unexpected API response (HTTP " + status + ").");
        }

        return parseResponse(body);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Parses the JSON response body into a {@link WeatherResponse}.
     * Guards against malformed JSON and missing required fields.
     */
    private WeatherResponse parseResponse(String body) {
        WeatherResponse wr;
        try {
            wr = objectMapper.readValue(body, WeatherResponse.class);
        } catch (JsonProcessingException e) {
            throw new WeatherParseException(
                    "Could not parse the weather API response. The format may have changed.", e);
        }

        // Guard required fields — API occasionally omits them for obscure locations
        if (wr.getMain() == null) {
            throw new WeatherParseException("Required field 'main' is absent in the API response.");
        }
        if (wr.getMain().getTemp() == null) {
            throw new WeatherParseException("Required field 'main.temp' is absent in the API response.");
        }

        return wr;
    }
}
