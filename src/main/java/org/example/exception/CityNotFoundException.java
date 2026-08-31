package org.example.exception;

/**
 * Thrown when the requested city does not exist or the name is misspelled.
 * Maps to HTTP 400 / 404 from the weather API.
 */
public class CityNotFoundException extends WeatherApiException {

    private final String cityName;

    public CityNotFoundException(String cityName) {
        super("City not found: \"" + cityName + "\". Please check the spelling and try again.");
        this.cityName = cityName;
    }

    public String getCityName() {
        return cityName;
    }
}
