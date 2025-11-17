package kr.ac.dongyang.mobileproject.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBLogin {
    DBHelper db = new DBHelper();
    Connection conn = db.getConnection();
    public boolean login(String id, String pw) {
        try {
            String sql = "SELECT * FROM member WHERE id=? AND pw=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            pstmt.setString(2, pw);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return true;
            } else {
                return false;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
