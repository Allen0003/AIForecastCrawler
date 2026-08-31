# AIForecastCrawler - 天氣查詢與爬蟲工具規格書 (Spec & API Contract)


## 0. log 都先不記錄下來

## 1. 專案基本資訊
- **語言版本**：Java 17
- **建置工具**：Maven
- **根套件路徑**：`org.example`
- **核心架構**：Java CLI / Service + Kafka 事件驅動生產者 (Producer) + Kubernetes KEDA ScaledObject (Job 自動擴展)

---

## 2. Maven 依賴設定 (`pom.xml`)
專案必須包含以下相依性（Java 17 標準環境）：

| 套件群組 (GroupId) | 套件名稱 (ArtifactId) | 版本建議 | 說明 |
|--------------------|-----------------------|----------|----------------------------------|
| `com.fasterxml.jackson.core` | `jackson-databind` | `2.15.2` | 用於解析天氣 API 回傳的 JSON 資料 |
| `org.slf4j` | `slf4j-api` | `2.0.7` | 日誌對外介面 |
| `ch.qos.logback` | `logback-classic` | `1.4.11` | SLF4J 具體實作與 Logback 設定 |
| `org.apache.kafka` | `kafka-clients` | `3.4.0` | Kafka 生產者與消費者客戶端 |
| `org.junit.jupiter` | `junit-jupiter` | `5.9.3` | 單元測試 (Scope: test) |

---

## 3. 資料模型規格 (Data Models)

### 3.1 外部 API 回傳結構對應 (`WeatherResponseDTO`)
對應第三方天氣 API（如 OpenWeatherMap）的 JSON 格式：

| 欄位名稱      | Java 型別    | 說明                          |
|---------------|--------------|-------------------------------|
| `cityName`    | `String`     | 城市名稱 (對應 JSON 的 `name`) |
| `temperature` | `double`     | 目前氣溫 (對應 JSON 的 `main.temp`) |
| `humidity`    | `int`        | 濕度百分比 (對應 JSON 的 `main.humidity`) |
| `description` | `String`     | 天氣描述 (對應 JSON 的 `weather[0].description`) |

### 3.2 內部領域物件 (`WeatherInfo`)
給業務邏輯與 CLI 顯示使用的乾淨物件：

| 欄位名稱      | Java 型別    | 說明                          |
|---------------|--------------|-------------------------------|
| `city`        | `String`     | 查詢的城市名稱                |
| `tempCelsius` | `double`     | 攝氏溫度                      |
| `humidity`    | `int`        | 濕度                          |
| `condition`   | `String`     | 天氣狀況概述                  |

---

## 4. 類別結構與介面合約 (Architecture & Class Design)

套件路徑：`src/main/java/org/example/`

### 4.1 例外處理架構 (`exception` package)
為了達成強健的錯誤處理，定義自定義例外類別：
- `WeatherApiException`: 基礎例外，繼承 `RuntimeException`。
- `CityNotFoundException`: 城市不存在或輸入錯誤時觸發 (對應 HTTP 404/400)。
- `UnauthorizedApiKeyException`: API 金鑰無效或超限時觸發 (對應 HTTP 401/429)。
- `ApiTimeoutException`: 網路連線逾時或無法連線時觸發。
- `DataParseException`: JSON 解析失敗或欄位缺失時觸發。

### 4.2 核心服務 (`WeatherService`)
負責與外部 API 溝通、例外捕捉與日誌紀錄。

| 方法名稱 | 參數 | 回傳型別 | 說明 |
|----------|------|----------|----------------------------------|
| `getWeather(String cityName)` | `String cityName` | `WeatherInfo` | 呼叫 HTTP Client 取得該城市天氣，內含完整的 Try-Catch 與錯誤分類處理。 |

### 4.3 Kafka 訊息生產者 (`KafkaProducerService`) [NEW]
負責將需要爬蟲或查詢的城市任務發送至 Kafka Topic。

| 方法名稱 | 參數 | 回傳型別 | 說明 |
|----------|------|----------|----------------------------------|
| `sendWeatherTask(String cityName)` | `String cityName` | `void` | 將城市名稱推送到指定的 Kafka Topic，供後續 KEDA 觸發 Job。 |

### 4.4 自動化 Job 產生功能 (`JobProducerCli` / `TaskGenerator`) [NEW]
讓使用者或系統可以依據清單（例如多個城市）批次自動產生並發送任務至 Kafka，藉此驅動 Kubernetes 進行 Job 擴展。

| 方法名稱 | 參數 | 回傳型別 | 說明 |
|----------|------|----------|----------------------------------|
| `generateBatchTasks(List<String> cities)` | `List<String>` | `void` | 迴圈讀取城市清單，批次呼叫 `KafkaProducerService` 產生大量任務。 |

### 4.5 進入點 (`Main`)
- 提供命令列 (CLI) 介面。
- 支援互動模式：可選擇「單次查詢天氣」或「批次產生 Kafka 任務（觸發 KEDA Job）」。

---

## 5. 錯誤處理與日誌行為規範 (Error Handling & Logging Strategy)

1. **網路連線逾時**：設定 HttpClient 逾時時間為 5 秒。若發生逾時，記錄 `ERROR` 等級日誌，並向上拋出 `ApiTimeoutException`。
2. **HTTP 400/404**：拋出 `CityNotFoundException`。
3. **HTTP 401/429**：拋出 `UnauthorizedApiKeyException`。
4. **JSON 格式解析防禦**：當 Jackson 解析失敗時，捕捉 `JsonProcessingException`，記錄原始回應，並拋出 `DataParseException`。
5. **Kafka 連線例外**：當 Kafka Broker 不可用時，記錄 `ERROR` 日誌並進行必要的重試捕捉，避免批次任務崩潰。
6. **日誌規範**：所有例外拋出前，必須透過 SLF4J 記錄詳細堆疊追蹤。

---

## 6. Kubernetes KEDA 與 Kafka 部署規格 (KEDA ScaledObject Contract) [NEW]

為了配合 KEDA 根據 Kafka Queue 長度自動起 Job，需部署以下 Kubernetes 資源規格範本（存放於 `k8s/` 目錄）：

### 6.1 Kafka ScaledObject (`scaled-object.yaml`)
- **觸發器類型 (Trigger)**: `kafka`
- **Topic 名稱**: `weather-tasks`
- **Lag 閾值 (lagThreshold)**: `5` (當堆積超過 5 個訊息時開始自動起 Job)
- **Min Replica**: `0`
- **Max Replica**: `10`

### 6.2 Kubernetes Job 樣版對應
- 當 KEDA 偵測到 Kafka 有訊息時，會自動依據配置啟動對應的 Pod 執行本專案（執行 `WeatherService` 進行消費與爬蟲）。


