# Weather Information App (CS 1103 – Unit 8)

A Java Swing desktop app that shows live weather from the OpenWeatherMap API.

## Features
1. Real-time data from OpenWeatherMap (current weather + 5-day/3-hour forecast endpoints)
2. Location input: city name (`Paris`, `London,GB`) or coordinates (`48.85, 2.35`)
3. Temperature, feels-like, humidity, wind speed, pressure and conditions
4. Weather icons drawn with Java2D (work offline)
5. 24-hour forecast strip (eight 3-hour cards)
6. Unit conversion: °C / °F and wind in m/s, km/h, mph, knots (instant, no new request)
7. Error handling: empty input, bad coordinates, unknown city (404), bad API key (401), rate limit (429), no internet, bad server response
8. History of recent searches with timestamps (click a row to search again)
9. Dynamic background (night, dawn, day, dusk) based on the searched city's local sunrise/sunset

## Requirements
* JDK 17 or newer (tested with 21; the code uses records and pattern matching). No external libraries.
* A free API key: sign up at https://openweathermap.org/api (new keys can take up to 2 hours to activate).

## Set the API key (pick one)
* Environment variable `OPENWEATHER_API_KEY`
* A file named exactly `config.properties` in the project folder (the folder that contains `src/`) with the line `api.key=YOUR_KEY` (copy `config.properties.template`). The app also looks in the folder you launch it from and in your home folder, and accepts `config.properties.txt` in case your editor added `.txt`.
* Or just start the app – it asks for the key on the first search and saves it to `config.properties`.

## Run
macOS / Linux: `./run.sh`  Windows: `run.bat`

Manual: 
```
javac -d out src/*.java
java -cp out WeatherApp
```

### In VS Code
Open the `WeatherApp` folder, install the "Extension Pack for Java", open `src/WeatherApp.java` and click **Run** above `main`. Set the key with the environment variable or `config.properties` in the project root (the working directory).

## Offline tests and screenshots
The images in `docs/` were rendered by `ScreenshotTool` from sample OpenWeatherMap-format JSON (no live request). Add your own live-run screenshots there after testing with your key.

```
java -cp out SelfTest                                   # 32 checks, no internet needed
java -Djava.awt.headless=true -cp out ScreenshotTool docs   # renders sample screenshots to docs/
```

## Project layout
| File | Responsibility |
|---|---|
| `WeatherApp.java` | Swing GUI, event handling, SwingWorker, `main` |
| `WeatherService.java` | HTTP calls, input validation, JSON → objects, API key loading |
| `Json.java` | Small JSON parser (no libraries) |
| `WeatherData.java`, `ForecastEntry.java` | Immutable data records (metric units) |
| `UnitConverter.java` | Temperature and wind conversions and formatting |
| `TimeOfDay.java`, `BackgroundPanel.java` | Dynamic gradient background |
| `WeatherIcons.java` | Java2D condition icons |
| `SearchHistory.java` | Timestamped recent-search list |
| `WeatherException.java` | Typed errors for friendly messages |
| `SampleData.java`, `SelfTest.java`, `ScreenshotTool.java` | Offline testing helpers |
