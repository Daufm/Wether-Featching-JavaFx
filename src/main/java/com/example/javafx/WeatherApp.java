package com.example.javafx;



import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class WeatherApp extends Application {
    private static final String API_KEY = loadApiKey();

    private static String loadApiKey() {
        try (FileInputStream input = new FileInputStream("config.properties")) {
            Properties properties = new Properties();
            properties.load(input);
            return properties.getProperty("api.key");
        } catch (IOException e) {
            e.printStackTrace();
            return null; // or handle the error as needed
        }
    }




    private static final String API_URL = "http://api.openweathermap.org/data/2.5/weather";

    private Label cityLabel;
    private Label temperatureLabel;
    private Label weatherDescLabel;
    private Label humidityLabel;
    private Label windSpeedLabel;
    private ImageView weatherIcon;
    private TextField cityInput;
    private Button searchButton;
    private VBox weatherDataBox;
    private Label errorLabel;

    @Override
    public void start(Stage primaryStage) {
        // Main container
        VBox root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1e3c72, #2a5298);");

        // Title
        Label titleLabel = new Label("Weather Forecast");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.WHITE);

        // Search section
        HBox searchBox = createSearchBox();

        // Error label
        errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);
        errorLabel.setVisible(false);

        // Weather data section
        weatherDataBox = createWeatherDataBox();
        weatherDataBox.setVisible(false);

        root.getChildren().addAll(titleLabel, searchBox, errorLabel, weatherDataBox);

        // Create scene
        Scene scene = new Scene(root, 400, 600);
        primaryStage.setTitle("Weather Forecast");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private HBox createSearchBox() {
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER);

        cityInput = new TextField();
        cityInput.setPromptText("Enter city name");
        cityInput.setPrefWidth(200);
        cityInput.setStyle("-fx-background-radius: 20; -fx-background-color: white;");

        searchButton = new Button("Search");
        searchButton.setStyle("""
                -fx-background-color: #4CAF50;
                -fx-text-fill: white;
                -fx-background-radius: 20;
                -fx-cursor: hand;
                """);

        searchButton.setOnAction(e -> fetchWeatherData());

        searchBox.getChildren().addAll(cityInput, searchButton);
        return searchBox;
    }

    private VBox createWeatherDataBox() {
        VBox weatherBox = new VBox(15);
        weatherBox.setAlignment(Pos.CENTER);
        weatherBox.setPadding(new Insets(20));
        weatherBox.setStyle("""
                -fx-background-color: rgba(255, 255, 255, 0.2);
                -fx-background-radius: 15;
                """);

        cityLabel = createStyledLabel("", 24);
        temperatureLabel = createStyledLabel("", 40);
        weatherDescLabel = createStyledLabel("", 18);
        humidityLabel = createStyledLabel("", 16);
        windSpeedLabel = createStyledLabel("", 16);

        weatherIcon = new ImageView();
        weatherIcon.setFitWidth(100);
        weatherIcon.setFitHeight(100);

        weatherBox.getChildren().addAll(
                cityLabel,
                weatherIcon,
                temperatureLabel,
                weatherDescLabel,
                humidityLabel,
                windSpeedLabel
        );

        return weatherBox;
    }

    private Label createStyledLabel(String text, int fontSize) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.MEDIUM, fontSize));
        label.setTextFill(Color.WHITE);
        return label;
    }

    private void fetchWeatherData() {
        String city = cityInput.getText().trim();
        if (city.isEmpty()) {
            showError("Please enter a city name");
            return;
        }

        try {
            String urlString = String.format("%s?q=%s&appid=%s&units=metric", API_URL, city, API_KEY);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != 200) {
                showError("Failed to fetch weather data");
                return;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            updateUI(new JSONObject(response.toString()));

        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        }
    }

    private void updateUI(JSONObject weatherData) {
        try {
            String cityName = weatherData.getString("name");
            JSONObject main = weatherData.getJSONObject("main");
            JSONObject weather = weatherData.getJSONArray("weather").getJSONObject(0);
            JSONObject wind = weatherData.getJSONObject("wind");

            double temp = main.getDouble("temp");
            int humidity = main.getInt("humidity");
            double windSpeed = wind.getDouble("speed");
            String description = weather.getString("description");
            String iconCode = weather.getString("icon");

            // Update UI elements on JavaFX Application Thread
            javafx.application.Platform.runLater(() -> {
                cityLabel.setText(cityName);
                temperatureLabel.setText(String.format("%.1f°C", temp));
                weatherDescLabel.setText(description);
                humidityLabel.setText(String.format("Humidity: %d%%", humidity));
                windSpeedLabel.setText(String.format("Wind Speed: %.1f m/s", windSpeed));

                // Load weather icon
                String iconUrl = String.format("http://openweathermap.org/img/wn/%s@2x.png", iconCode);
                weatherIcon.setImage(new Image(iconUrl));

                errorLabel.setVisible(false);
                weatherDataBox.setVisible(true);
            });

        } catch (Exception e) {
            showError("Error parsing weather data");
        }
    }

    private void showError(String message) {
        javafx.application.Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            weatherDataBox.setVisible(false);
        });
    }

    public static void main(String[] args) {
        System.out.print(API_KEY);
        launch(args);
    }
}