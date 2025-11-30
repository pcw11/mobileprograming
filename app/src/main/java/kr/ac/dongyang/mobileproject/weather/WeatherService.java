package kr.ac.dongyang.mobileproject.weather;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WeatherService {

    private static final String WEATHER_API_BASE_URL = "https://api.openweathermap.org/";

    private static Retrofit retrofit;

    public static WeatherApiService getWeatherApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(WEATHER_API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(WeatherApiService.class);
    }
}
