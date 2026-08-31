package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Maps the relevant fields of an OpenWeatherMap "current weather" JSON response.
 *
 * Example endpoint:
 *   https://api.openweathermap.org/data/2.5/weather?q={city}&appid={key}&units=metric&lang=zh_tw
 *
 * Only the fields required by the spec are surfaced; all unknown fields are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherResponseDTO {

    /** City name — maps to JSON field {@code name}. */
    @JsonProperty("name")
    private String cityName;

    /** Nested "main" block: temperature, humidity. */
    private Main main;

    /** Array of weather condition objects (description, etc.). */
    private List<WeatherCondition> weather;

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getCityName()                      { return cityName; }
    public Main getMain()                            { return main; }
    public List<WeatherCondition> getWeather()       { return weather; }

    // ── Inner classes ─────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Main {
        /** Current temperature in Celsius — maps to JSON field {@code main.temp}. */
        private Double temp;

        /** Humidity percentage — maps to JSON field {@code main.humidity}. */
        private Integer humidity;

        public Double  getTemp()     { return temp; }
        public Integer getHumidity() { return humidity; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherCondition {
        /** Weather description — maps to JSON field {@code weather[0].description}. */
        private String description;

        public String getDescription() { return description; }
    }
}
