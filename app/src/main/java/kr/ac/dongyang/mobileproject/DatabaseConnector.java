package kr.ac.dongyang.mobileproject;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseConnector {

    private Context context;

    public DatabaseConnector(Context context) {
        this.context = context;
    }

    // 콜백 인터페이스 정의
    public interface DatabaseCallback {
        void onComplete(boolean success);
    }

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        // MySQL JDBC 드라이버 로드
        Class.forName("com.mysql.jdbc.Driver");

        // 데이터베이스 연결 URL 생성
        String url = "jdbc:mysql://" + BuildConfig.DB_HOST + "/" + BuildConfig.DB_NAME + "?useSSL=false&serverTimezone=UTC";

        // 데이터베이스 연결
        return DriverManager.getConnection(url, BuildConfig.DB_USER, BuildConfig.DB_PASSWORD);
    }

    public void updateLastWateredDate(long plantId, String newLastWateredDate, DatabaseCallback callback) {
        new Thread(() -> {
            Handler handler = new Handler(Looper.getMainLooper());
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE plants SET last_watered_date = ? WHERE plant_id = ?")) {

                statement.setString(1, newLastWateredDate);
                statement.setLong(2, plantId);
                int affectedRows = statement.executeUpdate();

                // 메인 스레드에서 콜백 실행
                handler.post(() -> callback.onComplete(affectedRows > 0));

            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
                // 메인 스레드에서 콜백 실행 및 오류 메시지 표시
                handler.post(() -> {
                    Toast.makeText(context, "데이터베이스 오류: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    callback.onComplete(false);
                });
            }
        }).start();
    }
}
