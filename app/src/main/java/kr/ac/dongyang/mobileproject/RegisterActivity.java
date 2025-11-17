package kr.ac.dongyang.mobileproject;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. register.xml을 화면에 표시
        setContentView(R.layout.register); // R.layout.register.xml

        // (추가) 이곳에 '중복확인' 버튼, '가입하기' 버튼 등의
        // 클릭 리스너 로직을 구현하면 됩니다.
    }
}