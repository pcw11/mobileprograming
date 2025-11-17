package kr.ac.dongyang.mobileproject;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class FindPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. findpw.xml을 화면에 표시
        setContentView(R.layout.findpw); // R.layout.findpw.xml

        // (추가) 이곳에 '인증' 버튼, '변경하기' 버튼 등의
        // 클릭 리스너 로직을 구현하면 됩니다.
    }
}