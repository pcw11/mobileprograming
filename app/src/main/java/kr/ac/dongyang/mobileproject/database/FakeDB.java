package kr.ac.dongyang.mobileproject.database;

import android.widget.TextView;

public class FakeDB {
    public static boolean login(String id, String pw) {
        return true;
    }
    public static String getName(String id){
        return "김동양";
    }
    public static int getPlantCount(String id){
        return 10;
    }

}
