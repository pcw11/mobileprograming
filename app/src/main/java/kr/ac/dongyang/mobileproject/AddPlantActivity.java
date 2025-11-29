package kr.ac.dongyang.mobileproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class AddPlantActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView ivMenu;
    private TextView tvGreeting;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plant);

        drawerLayout = findViewById(R.id.drawer_layout_add);
        navigationView = findViewById(R.id.nav_view_add);
        ivMenu = findViewById(R.id.iv_menu_add);
        tvGreeting = findViewById(R.id.tv_greeting_add);

        // MainActivity로부터 사용자 ID를 받아 환영 메시지 설정
        Intent intent = getIntent();
        String userId = intent.getStringExtra("USER_ID");
        if (userId != null && !userId.isEmpty()) {
            tvGreeting.setText(userId + "님 안녕하세요!");
        }

        // 메뉴 버튼 클릭 리스너
        ivMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));

        // 네비게이션 메뉴 아이템 클릭 리스너
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                logout();
            } else {
                Toast.makeText(AddPlantActivity.this, "준비 중인 기능입니다.", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });
    }

    private void logout() {
        // 자동 로그인 정보 삭제
        SharedPreferences sharedPreferences = getSharedPreferences("AutoLoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();

        // 로그인 화면으로 이동
        Intent intent = new Intent(AddPlantActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
