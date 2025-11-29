package kr.ac.dongyang.mobileproject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        // MySQL JDBC 드라이버 로드
        Class.forName("com.mysql.jdbc.Driver");

        // 데이터베이스 연결 URL 생성
        String url = "jdbc:mysql://" + BuildConfig.DB_HOST + "/" + BuildConfig.DB_NAME + "?useSSL=false&serverTimezone=UTC";

        // 데이터베이스 연결
        return DriverManager.getConnection(url, BuildConfig.DB_USER, BuildConfig.DB_PASSWORD);
    }
}
