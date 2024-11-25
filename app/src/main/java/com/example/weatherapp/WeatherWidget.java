package com.example.weatherapp;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int appWidgetId : appWidgetIds) {
            new FetchWeatherTask(context, appWidgetManager, appWidgetId).execute();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), WeatherWidget.class.getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    private static class FetchWeatherTask extends AsyncTask<Void, Void, String[]> {
        private final Context context;
        private final AppWidgetManager appWidgetManager;
        private final int appWidgetId;

        public FetchWeatherTask(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
            this.context = context;
            this.appWidgetManager = appWidgetManager;
            this.appWidgetId = appWidgetId;
        }

        @Override
        protected String[] doInBackground(Void... voids) {
            try {
                SharedPreferences sharedPreferences = context.getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE);
                String city = sharedPreferences.getString("selected_city", "Kyiv");

                String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=7322f49103354ea7a87d971975e3bce2";
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                JSONObject jsonObject = new JSONObject(result.toString());
                JSONObject main = jsonObject.getJSONObject("main");

                double temperature = main.getDouble("temp") - 273.15;
                double feelsLike = main.getDouble("feels_like") - 273.15;
                String weatherCondition = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");

                return new String[]{
                        city,
                        String.format("%.2f", temperature),
                        String.format("%.2f", feelsLike),
                        weatherCondition
                };
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] weatherData) {
            if (weatherData != null) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
                views.setTextViewText(R.id.widget_city_name, weatherData[0]);
                views.setTextViewText(R.id.widget_temperature, weatherData[1] + " °C");
                views.setTextViewText(R.id.widget_feels_like, context.getString(R.string.feels_like) + ": " + weatherData[2] + " °C");
                views.setTextViewText(R.id.widget_weather_condition, weatherData[3]);

                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }
    }
}



