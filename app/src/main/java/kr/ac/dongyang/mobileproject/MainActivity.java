package kr.ac.dongyang.mobileproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kr.ac.dongyang.mobileproject.plant.Plant;
import kr.ac.dongyang.mobileproject.plant.PlantAdapter;

public class MainActivity extends AppCompatActivity {

    public static final int ADD_PLANT_REQUEST = 1;
    public static final int VIEW_PLANT_REQUEST = 2;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView ivMenu;
    private RecyclerView recyclerView;
    private PlantAdapter adapter;
    private ArrayList<Plant> plantList;
    private FloatingActionButton fabAdd;
    private ViewPager2 weatherViewPager;
    private WeatherAdapter weatherAdapter;
    private LinearLayout indicatorLayout;
    private ImageView[] indicators;
    private TextView tvGreeting, tvPlantCount;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 초기화
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        ivMenu = findViewById(R.id.iv_menu);
        tvGreeting = findViewById(R.id.tv_greeting);
        recyclerView = findViewById(R.id.rv_plant_list);
        fabAdd = findViewById(R.id.fab_add);
        plantList = new ArrayList<>();
        weatherViewPager = findViewById(R.id.vp_weather);
        indicatorLayout = findViewById(R.id.ll_indicator);
        tvPlantCount = findViewById(R.id.tv_plant_count);

        // 환영 메시지 설정
        Intent intent = getIntent();
        currentUserId = intent.getStringExtra("USER_ID");
        if (currentUserId != null && !currentUserId.isEmpty()) {
            tvGreeting.setText(currentUserId + "님 안녕하세요!");
        } else {
            tvGreeting.setText("안녕하세요!");
        }

        // 메뉴 버튼 클릭 리스너
        ivMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));

        // 네비게이션 메뉴 아이템 클릭 리스너
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                logout();
            } else {
                Toast.makeText(MainActivity.this, "준비 중인 기능입니다.", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });

        // 3. 어댑터 생성 및 연결
        adapter = new PlantAdapter(plantList);
        recyclerView.setAdapter(adapter);

        // 4. 레이아웃 매니저 설정
        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recyclerView.setLayoutManager(layoutManager);

        // 5. FAB 버튼 클릭 이벤트 - 식물 추가 화면으로 이동
        fabAdd.setOnClickListener(v -> {
            Intent addPlantIntent = new Intent(MainActivity.this, AddPlantActivity.class);
            addPlantIntent.putExtra("USER_ID", currentUserId); // 현재 사용자 ID 전달
            startActivityForResult(addPlantIntent, ADD_PLANT_REQUEST);
        });

        // 6. 날씨 뷰페이저 설정
        setupWeatherViewPager();
        
        // 7. DB에서 식물 목록 불러오기
        loadPlantData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == ADD_PLANT_REQUEST || requestCode == VIEW_PLANT_REQUEST) && resultCode == RESULT_OK) {
            // 식물 추가/수정/삭제 후 돌아왔을 때 목록 새로고침
            loadPlantData();
        }
    }

    private void loadPlantData() {
        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(this).getConnection()) {
                String sql = "SELECT p.plant_id, p.nickname, p.species, p.main_image_url, p.watering_cycle, p.last_watered_date, GROUP_CONCAT(pm.content SEPARATOR '\n') as memos " +
                             "FROM plants p LEFT JOIN plant_memos pm ON p.plant_id = pm.plant_id " +
                             "WHERE p.user_id = ? GROUP BY p.plant_id";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, currentUserId);
                    ResultSet rs = pstmt.executeQuery();

                    plantList.clear();
                    while (rs.next()) {
                        long plantId = rs.getLong("plant_id");
                        String nickname = rs.getString("nickname");
                        String species = rs.getString("species");
                        String imageUrl = rs.getString("main_image_url");
                        int wateringCycle = rs.getInt("watering_cycle");
                        String lastWateredDate = rs.getString("last_watered_date");
                        String memosConcat = rs.getString("memos");
                        List<String> memos = new ArrayList<>();
                        if(memosConcat != null) {
                            memos.addAll(Arrays.asList(memosConcat.split("\n")));
                        }

                        plantList.add(new Plant(plantId, species, nickname, imageUrl, memos, lastWateredDate, wateringCycle));
                    }

                    new Handler(Looper.getMainLooper()).post(() -> {
                        adapter.notifyDataSetChanged();
                        tvPlantCount.setText("총 " + plantList.size() + "개의 식물이 등록되어 있어요.");
                        if(plantList.isEmpty()) {
                            Toast.makeText(this, "등록된 식물이 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("DB_ERROR", "식물 목록 로딩 중 오류 발생", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(this, "식물 목록을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void logout() {
        // 자동 로그인 정보 삭제
        SharedPreferences sharedPreferences = getSharedPreferences("AutoLoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();

        // 로그인 화면으로 이동
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setupWeatherViewPager() {
        List<Weather> weatherList = new ArrayList<>();
        weatherList.add(new Weather(R.drawable.sunny, "서울특별시 구로구 고척동", "맑음 19°C🌡"));
        weatherList.add(new Weather(R.drawable.cloud, "경기도 부천시 역곡동", "구름 많음 18°C☁️"));
        weatherList.add(new Weather(R.drawable.rain, "인천광역시 미추홀구", "비 17°C🌧️"));

        weatherAdapter = new WeatherAdapter(weatherList);
        weatherViewPager.setAdapter(weatherAdapter);

        setupIndicators(weatherAdapter.getItemCount());

        weatherViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
            }
        });
    }

    private void setupIndicators(int count) {
        indicators = new ImageView[count];
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 0);

        indicatorLayout.removeAllViews();

        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(this);
            indicators[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.tab_indicator_default));
            indicators[i].setLayoutParams(params);
            indicatorLayout.addView(indicators[i]);
        }
        updateIndicators(0); // 초기 상태 설정
    }

    private void updateIndicators(int position) {
        for (int i = 0; i < indicators.length; i++) {
            indicators[i].setImageDrawable(ContextCompat.getDrawable(this,
                    i == position ? R.drawable.tab_indicator_selected : R.drawable.tab_indicator_default));
        }
    }
}
