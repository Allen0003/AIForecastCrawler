package org.example.model;

/**
 * Internal domain object used by the business logic and CLI display layer.
 * Decoupled from the raw API response structure.
 */
public class WeatherInfo {

    private final String city;
    private final double tempCelsius;
    private final int    humidity;
    private final String condition;

    public WeatherInfo(String city, double tempCelsius, int humidity, String condition) {
        this.city        = city;
        this.tempCelsius = tempCelsius;
        this.humidity    = humidity;
        this.condition   = condition;
    }

    public String getCity()        { return city; }
    public double getTempCelsius() { return tempCelsius; }
    public int    getHumidity()    { return humidity; }
    public String getCondition()   { return condition; }

    @Override
    public String toString() {
        return String.format(
                "╔══════════════════════════════════════╗%n" +
                "  📍 %s%n" +
                "╚══════════════════════════════════════╝%n" +
                "  天氣狀況  : %s%n" +
                "  溫度      : %.1f °C%n" +
                "  濕度      : %d %%%n",
                city, condition, tempCelsius, humidity);
    }
}
