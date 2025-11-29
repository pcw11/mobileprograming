package kr.ac.dongyang.mobileproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import kr.ac.dongyang.mobileproject.plant.Plant;
import kr.ac.dongyang.mobileproject.plant.PlantAdapter;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PlantAdapter adapter;
    private ArrayList<Plant> plantList;
    private FloatingActionButton fabAdd;
    private ViewPager2 weatherViewPager;
    private WeatherAdapter weatherAdapter;
    private LinearLayout indicatorLayout;
    private ImageView[] indicators;
    private TextView tvGreeting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 초기화
        tvGreeting = findViewById(R.id.tv_greeting);
        recyclerView = findViewById(R.id.rv_plant_list);
        fabAdd = findViewById(R.id.fab_add);
        plantList = new ArrayList<>();
        weatherViewPager = findViewById(R.id.vp_weather);
        indicatorLayout = findViewById(R.id.ll_indicator);

        // 환영 메시지 설정
        Intent intent = getIntent();
        String userId = intent.getStringExtra("USER_ID");
        if (userId != null && !userId.isEmpty()) {
            tvGreeting.setText(userId + "님 안녕하세요!");
        } else {
            tvGreeting.setText("안녕하세요!"); // ID가 없는 경우 기본 메시지
        }

        // 2. 더미 데이터(기본 식물들) 추가
        plantList.add(new Plant("아이비", "초록이", "그늘을 좋아해요", 4, true));
        plantList.add(new Plant("선인장", "가시돌이", "물 자주 주지 말것", 17, false)); // 이미지 없음
        plantList.add(new Plant("스투키", "공기청정기", "침실에 두면 좋음", 7, true));
        plantList.add(new Plant("몬스테라", "왕잎", "잎이 갈라질 때까지", 2, true));

        // 3. 어댑터 생성 및 연결
        adapter = new PlantAdapter(plantList);
        recyclerView.setAdapter(adapter);

        // 4. [중요] 레이아웃 매니저 설정 (지그재그 배치)
        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recyclerView.setLayoutManager(layoutManager);

        // 5. FAB 버튼 클릭 이벤트 (식물 추가)
        fabAdd.setOnClickListener(v -> addNewPlant());

        // 6. 날씨 뷰페이저 설정
        setupWeatherViewPager();
    }

    private void addNewPlant() {
        Plant newPlant = new Plant("새로운 식물", "뉴비", "새로 들어왔어요!", 5, true);
        plantList.add(newPlant);
        adapter.notifyItemInserted(plantList.size() - 1);
        recyclerView.smoothScrollToPosition(plantList.size() - 1);
        Toast.makeText(this, "새 식물이 추가되었습니다!", Toast.LENGTH_SHORT).show();
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
        params.setMargins(16, 0, 16, 0);

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
