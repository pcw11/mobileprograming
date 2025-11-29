package kr.ac.dongyang.mobileproject;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddPlantActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView ivMenu;
    private TextView tvGreeting;

    private EditText etPlantSpecies, etPlantNickname;
    private ImageView ivWaterEdit, ivMemoAdd;
    private TextView tvWaterSubtitle;
    private Button btnSave;
    private LinearLayout llMemoContainer;

    private int wateringCycle = 7; // 기본 물 주기 7일
    private String userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plant);

        // 기존 뷰 초기화
        drawerLayout = findViewById(R.id.drawer_layout_add);
        navigationView = findViewById(R.id.nav_view_add);
        ivMenu = findViewById(R.id.iv_menu_add);
        tvGreeting = findViewById(R.id.tv_greeting_add);

        // 새로 추가된 뷰 초기화
        etPlantSpecies = findViewById(R.id.et_plant_species);
        etPlantNickname = findViewById(R.id.et_plant_nickname);
        ivWaterEdit = findViewById(R.id.iv_water_edit);
        tvWaterSubtitle = findViewById(R.id.tv_water_subtitle);
        ivMemoAdd = findViewById(R.id.iv_memo_add);
        llMemoContainer = findViewById(R.id.ll_memo_container);
        btnSave = findViewById(R.id.btn_save);

        // MainActivity로부터 사용자 ID를 받아 환영 메시지 설정
        Intent intent = getIntent();
        userId = intent.getStringExtra("USER_ID");
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

        // 물 주기 수정 아이콘 클릭 리스너
        ivWaterEdit.setOnClickListener(v -> showWateringCycleDialog());

        // 메모 추가 버튼 클릭 리스너
        ivMemoAdd.setOnClickListener(v -> addMemoView());

        // 저장 버튼 클릭 리스너
        btnSave.setOnClickListener(v -> savePlantData());

        // 초기 물 주기 텍스트 설정
        tvWaterSubtitle.setText("물 주기는 " + wateringCycle + "일 입니다.");
        
        //초기 메모 입력창 추가
        addMemoView();
    }

    private void showWateringCycleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("물 주기 설정");

        final NumberPicker numberPicker = new NumberPicker(this);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(30);
        numberPicker.setValue(wateringCycle);
        builder.setView(numberPicker);

        builder.setPositiveButton("확인", (dialog, which) -> {
            wateringCycle = numberPicker.getValue();
            tvWaterSubtitle.setText("물 주기는 " + wateringCycle + "일 입니다.");
            Toast.makeText(AddPlantActivity.this, "물 주기가 " + wateringCycle + "일로 설정되었습니다.", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("취소", null);

        builder.create().show();
    }

    private void addMemoView() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View memoView = inflater.inflate(R.layout.memo_item, llMemoContainer, false);
        llMemoContainer.addView(memoView);
    }

    private void savePlantData() {
        String species = etPlantSpecies.getText().toString().trim();
        String nickname = etPlantNickname.getText().toString().trim();

        if (species.isEmpty()) {
            Toast.makeText(this, "식물 종을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> memos = new ArrayList<>();
        for (int i = 0; i < llMemoContainer.getChildCount(); i++) {
            View memoView = llMemoContainer.getChildAt(i);
            EditText etMemo = memoView.findViewById(R.id.et_memo_item);
            String memoText = etMemo.getText().toString().trim();
            if (!memoText.isEmpty()) {
                memos.add(memoText);
            }
        }

        new Thread(() -> {
            try (Connection conn = new DatabaseConnector().getConnection()) {
                String sql = "INSERT INTO plants (user_id, species, nickname, watering_cycle, last_watered_date) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, userId);
                    pstmt.setString(2, species);
                    pstmt.setString(3, nickname);
                    pstmt.setInt(4, wateringCycle);
                    pstmt.setDate(5, new Date(System.currentTimeMillis()));

                    int affectedRows = pstmt.executeUpdate();

                    if (affectedRows > 0) {
                        try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                long plantId = generatedKeys.getLong(1);

                                if (!memos.isEmpty()) {
                                    String memoSql = "INSERT INTO plant_memos (plant_id, content) VALUES (?, ?)";
                                    try (PreparedStatement memoPstmt = conn.prepareStatement(memoSql)) {
                                        for (String memo : memos) {
                                            memoPstmt.setLong(1, plantId);
                                            memoPstmt.setString(2, memo);
                                            memoPstmt.addBatch();
                                        }
                                        memoPstmt.executeBatch();
                                    }
                                }

                                new Handler(Looper.getMainLooper()).post(() -> {
                                    Toast.makeText(AddPlantActivity.this, "식물이 성공적으로 등록되었습니다.", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK); // MainActivity에 변경이 있음을 알림
                                    finish();
                                });
                            }
                        }
                    } else {
                        showToast("식물 등록에 실패했습니다.");
                    }
                }
            } catch (Exception e) {
                Log.e("DB_ERROR", "식물 등록 중 오류 발생", e);
                showToast("오류가 발생했습니다: " + e.getMessage());
            }
        }).start();
    }

    private void logout() {
        SharedPreferences sharedPreferences = getSharedPreferences("AutoLoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(AddPlantActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    
    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        });
    }
}
