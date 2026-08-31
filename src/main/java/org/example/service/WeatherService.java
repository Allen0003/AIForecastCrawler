package org.example.service;

import org.example.client.WeatherApiClient;
import org.example.exception.DataParseException;
import org.example.model.WeatherInfo;
import org.example.model.WeatherResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Business-logic layer that sits on top of {@link WeatherApiClient}.
 *
 * Responsibilities:
 * <ul>
 *   <li>Input validation (blank / null city name)</li>
 *   <li>Calling the API client and forwarding typed exceptions</li>
 *   <li>Mapping the raw {@link WeatherResponseDTO} into the internal {@link WeatherInfo} domain object</li>
 * </ul>
 */
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final WeatherApiClient apiClient;

    public WeatherService(WeatherApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Fetches current weather for the given city and returns a clean domain object.
     *
     * @param cityName city name provided by the user
     * @return {@link WeatherInfo} containing the essential weather data
     * @throws IllegalArgumentException if the city name is blank
     * @throws org.example.exception.WeatherApiException (or any subtype) on API / network / parse errors
     */
    public WeatherInfo getWeather(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            log.warn("getWeather called with blank city name");
            throw new IllegalArgumentException("City name must not be empty.");
        }

        log.info("Requesting weather for city='{}'", cityName);

        // Delegate to client — any WeatherApiException propagates as-is to the caller
        WeatherResponseDTO dto = apiClient.fetchWeather(cityName);

        return toWeatherInfo(dto, cityName);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    /**
     * Converts a {@link WeatherResponseDTO} into the internal {@link WeatherInfo} object.
     */
    private WeatherInfo toWeatherInfo(WeatherResponseDTO dto, String cityName) {
        String city = dto.getCityName() != null ? dto.getCityName() : cityName;

        WeatherResponseDTO.Main main = dto.getMain();
        double temp     = main.getTemp();
        int    humidity = main.getHumidity() != null ? main.getHumidity() : 0;

        String condition = "N/A";
        List<WeatherResponseDTO.WeatherCondition> conditions = dto.getWeather();
        if (conditions != null && !conditions.isEmpty() && conditions.get(0).getDescription() != null) {
            condition = capitalize(conditions.get(0).getDescription());
        } else {
            log.warn("Weather condition description is missing for city='{}'", cityName);
        }

        log.info("Mapped WeatherInfo for city='{}': temp={}°C, humidity={}%, condition='{}'",
                city, temp, humidity, condition);

        return new WeatherInfo(city, temp, humidity, condition);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
