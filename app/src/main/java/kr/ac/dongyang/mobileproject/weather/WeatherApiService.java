package kr.ac.dongyang.mobileproject.weather;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {
    @GET("data/2.5/weather")
    Call<WeatherResponse> getWeatherData(
        @Query("lat") double latitude,
        @Query("lon") double longitude,
        @Query("appid") String apiKey,
        @Query("units") String units,
        @Query("lang") String lang
    );
}
