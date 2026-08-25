package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Mirrors the relevant fields of an OpenWeatherMap "current weather" JSON response.
 *
 * Example endpoint:
 *   https://api.openweathermap.org/data/2.5/weather?q={city}&appid={key}&units=metric&lang=zh_tw
 *
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} ensures that any extra fields
 * returned by the API do not cause a parse failure.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherResponse {

    /** City name as returned by the API (may differ from input). */
    private String name;

    /** Nested "main" block: temperature, humidity, pressure. */
    private Main main;

    /** Nested "wind" block. */
    private Wind wind;

    /** Array of weather condition objects (description, icon, etc.). */
    private List<WeatherCondition> weather;

    /** Nested "sys" block: country code, sunrise/sunset. */
    private Sys sys;

    /** Cloud coverage percentage. */
    private Clouds clouds;

    /** Visibility in metres. */
    private Integer visibility;

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getName()                     { return name; }
    public Main getMain()                       { return main; }
    public Wind getWind()                       { return wind; }
    public List<WeatherCondition> getWeather()  { return weather; }
    public Sys getSys()                         { return sys; }
    public Clouds getClouds()                   { return clouds; }
    public Integer getVisibility()              { return visibility; }

    // ── Inner classes (one per JSON object) ──────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Main {
        private Double temp;
        @JsonProperty("feels_like")  private Double feelsLike;
        @JsonProperty("temp_min")    private Double tempMin;
        @JsonProperty("temp_max")    private Double tempMax;
        private Integer pressure;
        private Integer humidity;

        public Double  getTemp()      { return temp; }
        public Double  getFeelsLike() { return feelsLike; }
        public Double  getTempMin()   { return tempMin; }
        public Double  getTempMax()   { return tempMax; }
        public Integer getPressure()  { return pressure; }
        public Integer getHumidity()  { return humidity; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Wind {
        private Double speed;
        private Integer deg;

        public Double  getSpeed() { return speed; }
        public Integer getDeg()   { return deg; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherCondition {
        private Integer id;
        private String main;
        private String description;
        private String icon;

        public Integer getId()          { return id; }
        public String  getMain()        { return main; }
        public String  getDescription() { return description; }
        public String  getIcon()        { return icon; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sys {
        private String country;
        private Long sunrise;
        private Long sunset;

        public String getCountry() { return country; }
        public Long   getSunrise() { return sunrise; }
        public Long   getSunset()  { return sunset; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Clouds {
        private Integer all;

        public Integer getAll() { return all; }
    }
}
