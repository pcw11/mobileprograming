package kr.ac.dongyang.mobileproject;

import android.content.Context;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseConnector {

    private Context context;

    public DatabaseConnector(Context context) {
        this.context = context;
    }

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        // MySQL JDBC 드라이버 로드
        Class.forName("com.mysql.jdbc.Driver");

        // 데이터베이스 연결 URL 생성
        String url = "jdbc:mysql://" + BuildConfig.DB_HOST + "/" + BuildConfig.DB_NAME + "?useSSL=false&serverTimezone=UTC";

        // 데이터베이스 연결
        return DriverManager.getConnection(url, BuildConfig.DB_USER, BuildConfig.DB_PASSWORD);
    }

    public void updateLastWateredDate(long plantId, String newLastWateredDate) {
        new Thread(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE plants SET last_watered_date = ? WHERE id = ?")) {
                statement.setString(1, newLastWateredDate);
                statement.setLong(2, plantId);
                statement.executeUpdate();
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
