package kr.ac.dongyang.mobileproject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    // 1.5초 딜레이
    private static final int SPLASH_DELAY = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. splash.xml을 화면에 표시
        setContentView(R.layout.splash); // R.layout.splash.xml

        // 2. Handler를 사용하여 1.5초 후에 LoginActivity로 이동
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // 3. LoginActivity로 이동하는 Intent 생성
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);

                // 4. SplashActivity를 종료 (뒤로가기 버튼으로 돌아오지 않도록)
                finish();
            }
        }, SPLASH_DELAY);
    }
}