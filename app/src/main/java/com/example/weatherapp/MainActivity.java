package com.example.weatherapp;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import android.os.AsyncTask;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    TextView cityName;
    Button search;
    TextView show;
    String url;

    class getWeather extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line = "";
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
                Log.d(TAG, "Weather data received");
                return result.toString();
            } catch (Exception e) {
                Log.e(TAG, "Error in getting weather data", e);
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                if (result != null) {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONObject main = jsonObject.getJSONObject("main");

                    main.remove("sea_level");
                    main.remove("grnd_level");

                    String weatherInfo = main.toString();
                    weatherInfo = weatherInfo.replace("temp", "Temperature");
                    weatherInfo = weatherInfo.replace("feels_like", "Feels like");
                    weatherInfo = weatherInfo.replace("temp_min", "Minimal temperature");
                    weatherInfo = weatherInfo.replace("temp_max", "Maximum temperature");
                    weatherInfo = weatherInfo.replace("humidity", "Humidity");
                    weatherInfo = weatherInfo.replace("{","");
                    weatherInfo = weatherInfo.replace("}","");
                    weatherInfo = weatherInfo.replace(",","\n");
                    weatherInfo = weatherInfo.replace(":"," : ");

                    weatherInfo = convertToCelsius(weatherInfo);

                    show.setText(weatherInfo);
                    Log.d(TAG, "Weather data displayed");
                } else {
                    show.setText("Cannot find weather!");
                    Log.d(TAG, "No weather data found");
                }
            } catch(Exception e) {
                Log.e(TAG, "Error in parsing weather data", e);
                e.printStackTrace();
            }
        }

        private String convertToCelsius(String weatherInfo) {
            String[] lines = weatherInfo.split("\n");
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                if (line.contains("Temperature") || line.contains("Feels like") || line.contains("Minimal temperature") || line.contains("Maximum temperature")) {
                    String[] parts = line.split(" : ");
                    double kelvin = Double.parseDouble(parts[1]);
                    double celsius = kelvin - 273.15;
                    line = parts[0] + " : " + String.format("%.2f", celsius) + " °C";
                }
                sb.append(line).append("\n");
            }
            return sb.toString();
        }




    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cityName = findViewById(R.id.cityName);
        search = findViewById(R.id.search);
        show = findViewById(R.id.weather);

        final String[] temp = {""};

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Search button clicked");
                String city = cityName.getText().toString();
                if (!city.isEmpty()) {
                    url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=7322f49103354ea7a87d971975e3bce2";
                    Log.d(TAG, "URL: " + url);
                    try {
                        getWeather task = new getWeather();
                        temp[0] = task.execute(url).get();
                    } catch (ExecutionException | InterruptedException e) {
                        Log.e(TAG, "Error executing weather task", e);
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Enter city", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "City name is empty");
                }

                if (temp[0] == null) {
                    show.setText("Cannot find weather!");
                    Log.d(TAG, "Weather data is null");
                }
            }
        });
    }
}


