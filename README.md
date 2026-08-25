# 🌤️ AIForecastCrawler

A lightweight, robust Java 17 / Maven-based weather API crawler and CLI tool. 

This project was built entirely through **Vibe Coding**—leveraging AI agents (Kiro IDE) to automate file scaffolding, dependency management, error handling, and build processes while minimizing manual boilerplate coding.

---

## 🚀 Features

* **AI-Native Architecture**: Scaffolded and structured by AI agents following clean separation of concerns (`client`, `service`, `model`, `exception`).
* **Robust Exception Handling**: Custom business exceptions (`ApiAuthException`, `CityNotFoundException`, `NetworkException`, `WeatherParseException`).
* **Dual API Key Resolution**: Supports both operating system environment variables and CLI fallback parameters.
* **JSON Parsing**: Integrated with Jackson for seamless API response parsing.
* **Logging**: Configured with SLF4J and Logback.

---

## 🛠️ Prerequisites

* **Java 17** (JDK 17)
* **Maven 3.5.0+**
* An active API key from [OpenWeatherMap](https://openweathermap.org/)

---

## ⚙️ How to Run

1. **Clone the repository**:
   ```bash
   git clone [https://github.com/你的帳號/AIForecastCrawler.git](https://github.com/你的帳號/AIForecastCrawler.git)
   cd AIForecastCrawler
