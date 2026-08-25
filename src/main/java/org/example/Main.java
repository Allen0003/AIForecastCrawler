package org.example;

import org.example.client.WeatherApiClient;
import org.example.exception.*;
import org.example.service.WeatherService;

import java.util.Scanner;

/**
 * CLI entry point for AIForecastCrawler.
 *
 * Usage:
 *   1. Set environment variable OPENWEATHER_API_KEY to your free API key
 *      (register at https://openweathermap.org/api)
 *   2. Run: java -jar target/AIForecastCrawler-1.0-SNAPSHOT.jar
 *   3. Enter a city name when prompted, or type "exit" to quit.
 *
 * Architecture overview:
 *   Main  →  WeatherService  →  WeatherApiClient  →  OpenWeatherMap API
 *
 * Error handling strategy:
 *   Each layer translates low-level failures into typed WeatherExceptions.
 *   Main catches them all and prints a user-friendly message to the console.
 */
public class Main {

    /** Environment variable name for the API key — never hard-code secrets in source. */
//    private static final String API_KEY_ENV = "f0a53348281f84e59f13d70ca385f4c4";

    public static void main(String[] args) {
        // ── Resolve API key (env-var → CLI arg fallback) ──────────────────────
        String apiKey = "f0a53348281f84e59f13d70ca385f4c4";
        if ((apiKey == null || apiKey.isBlank()) && args.length > 0) {
            apiKey = args[0];
        }

        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("[錯誤] 找不到 API 金鑰。");
//            System.err.println("  請設定環境變數: export " + API_KEY_ENV + "=<your_key>");
            System.err.println("  或以第一個參數傳入:  java -jar app.jar <your_key>");
            System.exit(1);
        }

        // ── Bootstrap service ─────────────────────────────────────────────────
        WeatherApiClient client  = new WeatherApiClient(apiKey);
        WeatherService   service = new WeatherService(client);

        // ── Interactive REPL ──────────────────────────────────────────────────
        Scanner scanner = new Scanner(System.in);
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("       AIForecastCrawler v1.0          ");
        System.out.println("  輸入城市名稱查詢即時天氣（輸入 exit 離開）");
        System.out.println("╚══════════════════════════════════════╝");

        while (true) {
            System.out.print("\n請輸入城市名稱: ");
            String city = scanner.nextLine().trim();

            if (city.equalsIgnoreCase("exit") || city.equalsIgnoreCase("quit")) {
                System.out.println("掰掰！");
                break;
            }

            if (city.isBlank()) {
                System.out.println("[提示] 城市名稱不可為空，請重新輸入。");
                continue;
            }

            try {
                String report = service.getFormattedWeather(city);
                System.out.println(report);

            // ── Typed error handlers (most specific → most general) ───────────

            } catch (CityNotFoundException e) {
                System.out.println("[錯誤] 找不到城市：「" + city + "」");
                System.out.println("  提示：請確認拼寫，例如 Taipei、London、Tokyo。");

            } catch (ApiAuthException e) {
                System.out.println("[錯誤] " + e.getMessage());

            } catch (NetworkException e) {
                System.out.println("[錯誤] 網路連線失敗，請確認網路狀態後重試。");
                System.out.println("  詳細原因：" + e.getMessage());

            } catch (WeatherParseException e) {
                System.out.println("[錯誤] 天氣資料解析失敗，API 回傳格式可能已變更。");

            } catch (IllegalArgumentException e) {
                System.out.println("[錯誤] 輸入有誤：" + e.getMessage());

            } catch (WeatherException e) {
                System.out.println("[錯誤] 查詢失敗：" + e.getMessage());
            }
        }

        scanner.close();
    }
}
