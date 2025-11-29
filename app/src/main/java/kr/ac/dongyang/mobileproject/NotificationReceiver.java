package kr.ac.dongyang.mobileproject;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "plant_watering_notification_channel";
    private static final int NOTIFICATION_ID = 100;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        boolean isNotificationEnabled = prefs.getBoolean("isNotificationEnabled", false);

        if (!isNotificationEnabled) {
            return;
        }

        String userId = intent.getStringExtra("USER_ID");
        if (userId == null || userId.isEmpty()) {
            return;
        }

        new Thread(() -> {
            List<String> plantsToWater = getPlantsToWater(context, userId);

            if (!plantsToWater.isEmpty()) {
                sendNotification(context, userId, plantsToWater);
            }
        }).start();
    }

    private List<String> getPlantsToWater(Context context, String userId) {
        List<String> plantNames = new ArrayList<>();
        try (Connection conn = new DatabaseConnector(context).getConnection()) {
            String sql = "SELECT nickname, watering_cycle, last_watered_date FROM plants WHERE user_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    String nickname = rs.getString("nickname");
                    int wateringCycle = rs.getInt("watering_cycle");
                    String lastWateredDateStr = rs.getString("last_watered_date");

                    if (wateringCycle > 0 && lastWateredDateStr != null && !lastWateredDateStr.isEmpty()) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            Date lastWateredDate = sdf.parse(lastWateredDateStr);

                            Calendar nextWateringCal = Calendar.getInstance();
                            if (lastWateredDate != null) {
                                nextWateringCal.setTime(lastWateredDate);
                            }
                            nextWateringCal.add(Calendar.DAY_OF_YEAR, wateringCycle);

                            Calendar todayCal = Calendar.getInstance();

                            if (nextWateringCal.get(Calendar.YEAR) < todayCal.get(Calendar.YEAR) ||
                                (nextWateringCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                                 nextWateringCal.get(Calendar.DAY_OF_YEAR) <= todayCal.get(Calendar.DAY_OF_YEAR))) {
                                plantNames.add(nickname);
                            }
                        } catch (Exception e) {
                            Log.e("NotificationReceiver", "Error checking watering date for " + nickname, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("NotificationReceiver", "Database error", e);
        }
        return plantNames;
    }

    private void sendNotification(Context context, String userId, List<String> plantsToWater) {
        createNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("USER_ID", userId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String title = "식물 물주기 알림";
        String content;

        if (plantsToWater.size() == 1) {
            content = "오늘 " + plantsToWater.get(0) + "에 물을 줘야해요!";
        } else {
            content = "오늘 " + plantsToWater.get(0) + " 등 " + plantsToWater.size() + "개의 식물에 물을 줘야 해요!";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_settings) // TODO: 적절한 흑백 아이콘으로 교체해야 합니다.
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            Log.e("NotificationReceiver", "Notification permission not granted", e);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Plant Watering Notifications";
            String description = "Notifications for watering your plants";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
