package kr.ac.dongyang.mobileproject.database;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBHelper {
//TODO DB작업 및 로그인 페이지 작업 필요
    private static final String DB_URL = "jdbc:mysql://YOUR_SERVER_IP:3306/YOUR_DATABASE?useUnicode=true&characterEncoding=utf8";
    private static final String USER = "YOUR_DB_USERNAME";
    private static final String PASS = "YOUR_DB_PASSWORD";

    public static Connection getConnection() {
        Connection conn = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");  // MySQL 5.x용
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return conn;
    }
}

