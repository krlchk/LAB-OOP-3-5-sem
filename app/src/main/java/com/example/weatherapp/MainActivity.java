package com.example.weatherapp;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private TextView cityName;
    private Button search;
    private TextView show;
    private String url;

    private class GetWeather extends AsyncTask<String, Void, String> {
        private Context context;

        public GetWeather(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... urls) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
                Log.d(TAG, getString(R.string.weather_data_received));
                return result.toString();
            } catch (Exception e) {
                Log.e(TAG, getString(R.string.error_fetching_data), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONObject main = jsonObject.getJSONObject("main");

                    main.remove("sea_level");
                    main.remove("grnd_level");
                    main.remove("preassure");

                    String weatherInfo = main.toString();
                    weatherInfo = weatherInfo.replace("temp", getString(R.string.temperature))
                            .replace("feels_like", getString(R.string.feels_like))
                            .replace("humidity", getString(R.string.humidity))
                            .replace("{", "")
                            .replace("}", "")
                            .replace(",", "\n")
                            .replace(":", " : ");

                    weatherInfo = convertToCelsius(weatherInfo);

                    show.setText(weatherInfo);
                    Log.d(TAG, getString(R.string.weather_data_displayed));

                    updateCity(context, cityName.getText().toString());
                } catch (Exception e) {
                    Log.e(TAG, getString(R.string.error_parsing_data), e);
                    show.setText(getString(R.string.error_parsing_data));
                }
            } else {
                show.setText(getString(R.string.error_fetching_data));
                Log.d(TAG, getString(R.string.no_weather_data_found));
            }
        }

        private String convertToCelsius(String weatherInfo) {
            String[] lines = weatherInfo.split("\n");
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                if (line.contains(getString(R.string.temperature)) || line.contains(getString(R.string.feels_like))) {
                    String[] parts = line.split(" : ");
                    double kelvin = Double.parseDouble(parts[1].trim());
                    double celsius = kelvin - 273.15;
                    line = parts[0] + " : " + String.format("%.2f", celsius) + " °C";
                }
                sb.append(line).append("\n");
            }
            return sb.toString();
        }

        private void updateCity(Context context, String city) {
            SharedPreferences sharedPreferences = context.getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("selected_city", city);
            editor.apply();

            Intent intent = new Intent(context, WeatherWidget.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, WeatherWidget.class));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            context.sendBroadcast(intent);

            Log.d(TAG, getString(R.string.widget_updated_with_city) + ": " + city);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cityName = findViewById(R.id.cityName);
        search = findViewById(R.id.search);
        show = findViewById(R.id.weather);

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, getString(R.string.search_button_clicked));
                String city = cityName.getText().toString();
                if (!city.isEmpty()) {
                    url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=7322f49103354ea7a87d971975e3bce2";
                    Log.d(TAG, "URL: " + url);

                    try {
                        GetWeather task = new GetWeather(MainActivity.this);
                        task.execute(url).get();
                    } catch (ExecutionException | InterruptedException e) {
                        Log.e(TAG, getString(R.string.error_executing_task), e);
                        Toast.makeText(MainActivity.this, getString(R.string.error_fetching_data), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.enter_city_message), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, getString(R.string.empty_city_name));
                }
            }
        });
    }
}






