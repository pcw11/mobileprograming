package kr.ac.dongyang.mobileproject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;

import kr.ac.dongyang.mobileproject.weather.WeatherApiService;
import kr.ac.dongyang.mobileproject.weather.WeatherResponse;
import kr.ac.dongyang.mobileproject.weather.WeatherService;
import retrofit2.Call;
import retrofit2.Response;

public class WeatherCheckWorker extends Worker {

    private static final long NOTIFICATION_COOLDOWN_MS = TimeUnit.HOURS.toMillis(6); // 6시간 쿨다운

    public WeatherCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        boolean isWeatherEnabled = prefs.getBoolean("isWeatherNotificationEnabled", false);

        if (!isWeatherEnabled) {
            return Result.success();
        }

        String currentUserId = getApplicationContext().getSharedPreferences("AutoLoginPrefs", Context.MODE_PRIVATE).getString("USER_ID", null);
        if (currentUserId == null) {
            return Result.failure();
        }

        try (Connection conn = new DatabaseConnector(getApplicationContext()).getConnection()) {
            String sql = "SELECT id, region_name, nx, ny FROM saved_locations WHERE user_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    long locationId = rs.getLong("id");
                    String regionName = rs.getString("region_name");
                    double nx = rs.getDouble("nx");
                    double ny = rs.getDouble("ny");
                    checkWeatherForLocation(locationId, regionName, ny, nx, prefs);
                }
            }
        } catch (Exception e) {
            Log.e("WEATHER_WORKER_DB", "DB 오류", e);
            return Result.retry();
        }

        return Result.success();
    }

    private void checkWeatherForLocation(long locationId, String regionName, double lat, double lon, SharedPreferences prefs) {
        WeatherApiService weatherService = WeatherService.getWeatherApiService();
        String apiKey = BuildConfig.WEATHER_API_KEY;

        try {
            Response<WeatherResponse> response = weatherService.getWeatherData(lat, lon, apiKey, "metric", "kr").execute();
            if (response.isSuccessful() && response.body() != null) {
                WeatherResponse data = response.body();
                double temp = data.getMain().getTemp();
                String description = data.getWeather().get(0).getDescription().toLowerCase();

                checkAndNotify(locationId, regionName, temp, description, prefs);
            }
        } catch (Exception e) {
            Log.e("WEATHER_WORKER_API", "API 호출 오류 for " + regionName, e);
        }
    }

    private void checkAndNotify(long locationId, String regionName, double temp, String description, SharedPreferences prefs) {
        boolean lowTempEnabled = prefs.getBoolean("isLowTempEnabled", false);
        int lowTempThreshold = prefs.getInt("lowTempThreshold", 10);
        if (lowTempEnabled && temp < lowTempThreshold) {
            if (canNotify(prefs, "low_temp_" + locationId)) {
                String title = "저온 알림";
                String text = regionName + "의 현재 온도가 " + temp + "°C로 설정하신 온도보다 낮습니다.";
                sendNotification(title, text, (int) System.currentTimeMillis());
                updateNotificationTimestamp(prefs, "low_temp_" + locationId);
            }
            return; // 한 가지 조건만 처리
        }

        boolean highTempEnabled = prefs.getBoolean("isHighTempEnabled", false);
        int highTempThreshold = prefs.getInt("highTempThreshold", 30);
        if (highTempEnabled && temp > highTempThreshold) {
            if (canNotify(prefs, "high_temp_" + locationId)) {
                String title = "고온 알림";
                String text = regionName + "의 현재 온도가 " + temp + "°C로 설정하신 온도보다 높습니다.";
                sendNotification(title, text, (int) System.currentTimeMillis());
                updateNotificationTimestamp(prefs, "high_temp_" + locationId);
            }
            return;
        }

        boolean rainAlertEnabled = prefs.getBoolean("isRainAlertEnabled", false);
        if (rainAlertEnabled && description.contains("비")) {
            if (canNotify(prefs, "rain_" + locationId)) {
                String title = "강수 알림";
                String text = regionName + "에 비가 내립니다.";
                sendNotification(title, text, (int) System.currentTimeMillis());
                updateNotificationTimestamp(prefs, "rain_" + locationId);
            }
            return;
        }

        boolean snowAlertEnabled = prefs.getBoolean("isSnowAlertEnabled", false);
        if (snowAlertEnabled && description.contains("눈")) {
            if (canNotify(prefs, "snow_" + locationId)) {
                String title = "강설 알림";
                String text = regionName + "에 눈이 내립니다.";
                sendNotification(title, text, (int) System.currentTimeMillis());
                updateNotificationTimestamp(prefs, "snow_" + locationId);
            }
        }
    }

    private boolean canNotify(SharedPreferences prefs, String key) {
        long lastNotificationTime = prefs.getLong(key, 0);
        return System.currentTimeMillis() - lastNotificationTime > NOTIFICATION_COOLDOWN_MS;
    }

    private void updateNotificationTimestamp(SharedPreferences prefs, String key) {
        prefs.edit().putLong(key, System.currentTimeMillis()).apply();
    }

    private void sendNotification(String title, String text, int notificationId) {
        // Note: Make sure to create the notification channel in your Application class or MainActivity
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "weather_channel")
                .setSmallIcon(R.drawable.logo_circle) // Ensure this drawable exists
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.notify(notificationId, builder.build());
    }
}
