package org.example.service;

import org.example.client.WeatherApiClient;
import org.example.model.WeatherResponse;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Business-logic layer that sits on top of {@link WeatherApiClient}.
 *
 * Responsibilities:
 * <ul>
 *   <li>Input validation (blank / null city name)</li>
 *   <li>Calling the API client and forwarding typed exceptions</li>
 *   <li>Formatting the raw {@link WeatherResponse} into a human-readable string</li>
 * </ul>
 *
 * All error handling is centralised here so {@code Main} stays lean.
 */
public class WeatherService {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final WeatherApiClient apiClient;

    public WeatherService(WeatherApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Fetches and formats current weather for the given city.
     *
     * @param city city name provided by the user
     * @return formatted weather report string
     * @throws IllegalArgumentException if the city name is blank
     * @throws org.example.exception.WeatherException (or any subtype) on API / network / parse errors
     */
    public String getFormattedWeather(String city) {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City name must not be empty.");
        }

        // Delegate to client — any WeatherException propagates as-is to the caller
        WeatherResponse response = apiClient.fetchWeather(city);

        return format(response);
    }

    // ── Formatting ────────────────────────────────────────────────────────────

    /**
     * Converts a {@link WeatherResponse} into a multi-line, human-readable report.
     */
    private String format(WeatherResponse r) {
        StringBuilder sb = new StringBuilder();

        String cityLine = r.getName() != null ? r.getName() : "Unknown";
        if (r.getSys() != null && r.getSys().getCountry() != null) {
            cityLine += ", " + r.getSys().getCountry();
        }

        sb.append("╔══════════════════════════════════════╗\n");
        sb.append(String.format("  📍 %s%n", cityLine));
        sb.append("╚══════════════════════════════════════╝\n");

        // Weather condition description
        List<WeatherResponse.WeatherCondition> conditions = r.getWeather();
        if (conditions != null && !conditions.isEmpty()) {
            String desc = conditions.get(0).getDescription();
            sb.append(String.format("  天氣狀況  : %s%n", capitalize(desc)));
        }

        // Temperature block
        WeatherResponse.Main m = r.getMain();
        sb.append(String.format("  溫度      : %.1f °C%n", m.getTemp()));
        if (m.getFeelsLike() != null) {
            sb.append(String.format("  體感溫度  : %.1f °C%n", m.getFeelsLike()));
        }
        if (m.getTempMin() != null && m.getTempMax() != null) {
            sb.append(String.format("  溫度區間  : %.1f ~ %.1f °C%n", m.getTempMin(), m.getTempMax()));
        }
        if (m.getHumidity() != null) {
            sb.append(String.format("  濕度      : %d %%%n", m.getHumidity()));
        }
        if (m.getPressure() != null) {
            sb.append(String.format("  氣壓      : %d hPa%n", m.getPressure()));
        }

        // Wind
        if (r.getWind() != null && r.getWind().getSpeed() != null) {
            sb.append(String.format("  風速      : %.1f m/s%n", r.getWind().getSpeed()));
        }

        // Cloud coverage
        if (r.getClouds() != null && r.getClouds().getAll() != null) {
            sb.append(String.format("  雲量      : %d %%%n", r.getClouds().getAll()));
        }

        // Visibility
        if (r.getVisibility() != null) {
            sb.append(String.format("  能見度    : %d m%n", r.getVisibility()));
        }

        // Sunrise / Sunset
        if (r.getSys() != null) {
            if (r.getSys().getSunrise() != null) {
                String sunrise = TIME_FMT.format(Instant.ofEpochSecond(r.getSys().getSunrise()));
                sb.append(String.format("  日出      : %s%n", sunrise));
            }
            if (r.getSys().getSunset() != null) {
                String sunset = TIME_FMT.format(Instant.ofEpochSecond(r.getSys().getSunset()));
                sb.append(String.format("  日落      : %s%n", sunset));
            }
        }

        return sb.toString();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
