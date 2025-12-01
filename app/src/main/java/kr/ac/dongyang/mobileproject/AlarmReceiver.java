package kr.ac.dongyang.mobileproject;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "watering_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Watering Alarms", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // DB에서 오늘 물 줘야 할 식물 목록 가져오기
        List<String> plantsToWater = getPlantsToWater(context);

        if (!plantsToWater.isEmpty()) {
            Intent notificationIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

            String notificationContent = String.join(", ", plantsToWater) + "에게 물을 줄 시간입니다.";

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.logo_circle) // 알림 아이콘을 적절히 변경해야 합니다.
                    .setContentTitle("식물 물주기 알림")
                    .setContentText(notificationContent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            notificationManager.notify(1, builder.build());
        }
    }

    private List<String> getPlantsToWater(Context context) {
        List<String> plantsToWater = new ArrayList<>();
        // MainActivity의 checkWateringNeeded 로직과 유사하게 구현
        // 여기서는 간단하게 SharedPreferences에서 사용자 ID를 가져와야 합니다.
        String currentUserId = context.getSharedPreferences("AutoLoginPrefs", Context.MODE_PRIVATE).getString("USER_ID", null);

        if (currentUserId == null) {
            return plantsToWater; // 사용자가 없으면 아무것도 안함
        }

        try (Connection conn = new DatabaseConnector(context).getConnection()) {
            String sql = "SELECT nickname, last_watered_date, watering_cycle FROM plants WHERE user_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String today = sdf.format(new Date());

                while (rs.next()) {
                    String nickname = rs.getString("nickname");
                    String lastWateredDateStr = rs.getString("last_watered_date");
                    int waterCycle = rs.getInt("watering_cycle");

                    try {
                        Date lastWatered = sdf.parse(lastWateredDateStr);
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(lastWatered);
                        calendar.add(Calendar.DAY_OF_YEAR, waterCycle);
                        String nextWateringDay = sdf.format(calendar.getTime());

                        if (today.equals(nextWateringDay)) {
                            plantsToWater.add(nickname);
                        }
                    } catch (Exception e) {
                        Log.e("ALARM_RECEIVER_DB", "날짜 파싱 오류", e);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("ALARM_RECEIVER_DB", "DB 연결 오류", e);
        }
        return plantsToWater;
    }
}
