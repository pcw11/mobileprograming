package kr.ac.dongyang.mobileproject;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.viewpager2.widget.ViewPager2;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import kr.ac.dongyang.mobileproject.plant.Plant;
import kr.ac.dongyang.mobileproject.plant.PlantAdapter;
import kr.ac.dongyang.mobileproject.weather.WeatherApiService;
import kr.ac.dongyang.mobileproject.weather.WeatherResponse;
import kr.ac.dongyang.mobileproject.weather.WeatherService;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class MainActivity extends AppCompatActivity implements WeatherAdapter.WeatherDataReloadListener {

    public static final int ADD_PLANT_REQUEST = 1;
    public static final int VIEW_PLANT_REQUEST = 2;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;
    private static final int PICK_IMAGE_REQUEST = 3;
    private static final String WEATHER_CHECK_WORK_TAG = "weatherCheckWork";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView ivMenu, ivNotification;
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
    private Button btnUploadTest; // 추가된 버튼
    private List<Weather> weatherList; // 날씨 목록
    private long backKeyPressedTime = 0;
    private Toast toast;


    // Retrofit
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Retrofit 초기화
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://" + BuildConfig.SERVER_IP + ":6006/") // Flask 서버 주소
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(new OkHttpClient.Builder().build())
                .build();
        apiService = retrofit.create(ApiService.class);

        // 1. 초기화
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        ViewGroup.LayoutParams params = navigationView.getLayoutParams();
        params.width = (int) (width * 0.66);
        navigationView.setLayoutParams(params);

        ivMenu = findViewById(R.id.iv_menu);
        ivNotification = findViewById(R.id.iv_notification);
        tvGreeting = findViewById(R.id.tv_greeting);
        recyclerView = findViewById(R.id.rv_plant_list);
        fabAdd = findViewById(R.id.fab_add);
        plantList = new ArrayList<>();
        weatherList = new ArrayList<>();
        weatherViewPager = findViewById(R.id.vp_weather);
        indicatorLayout = findViewById(R.id.ll_indicator);
        tvPlantCount = findViewById(R.id.tv_plant_count);
        btnUploadTest = findViewById(R.id.btn_upload_test); // 버튼 초기화

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
            } else if (id == R.id.nav_account) {
                showAccountSettingsDialog();
            } else if (id == R.id.nav_notifications) {
                showNotificationSettingsDialog();
            } else if (id == R.id.nav_reset_data) {
                showResetDataConfirmDialog();
            } else {
                Toast.makeText(MainActivity.this, "준비 중인 기능입니다.", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });

        // 3. 어댑터 생성 및 연결
        adapter = new PlantAdapter(plantList, VIEW_PLANT_REQUEST);
        recyclerView.setAdapter(adapter);

        // 4. 레이아웃 매니저 설정
        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recyclerView.setLayoutManager(layoutManager);

        // 5. FAB 버튼 클릭 이벤트 - 식물 추가 화면으로 이동
        fabAdd.setOnClickListener(v -> {
            Intent addPlantIntent = new Intent(MainActivity.this, AddPlantActivity.class);
            addPlantIntent.putExtra("USER_ID", currentUserId);
            startActivityForResult(addPlantIntent, ADD_PLANT_REQUEST);
        });

        // 업로드 테스트 버튼 클릭 리스너
        btnUploadTest.setOnClickListener(v -> openGallery());

        // 6. 날씨 뷰페이저 설정
        setupWeatherViewPager();

        // 8. 알림 권한 요청
        requestNotificationPermission();
        requestStoragePermission();

        // Back button handler
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
                        backKeyPressedTime = System.currentTimeMillis();
                        toast = Toast.makeText(MainActivity.this, "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT);
                        toast.show();
                    } else {
                        finish();
                        if (toast != null) {
                            toast.cancel();
                        }
                    }
                }
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        new Handler(Looper.getMainLooper()).postDelayed(this::loadPlantData, 1000); // 1초 지연
        loadSavedWeatherData(); // 날씨 데이터 로드
    }

    @Override
    public void reloadWeatherData() {
        loadSavedWeatherData();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void uploadImage(Uri imageUri) {
        File tempFile = createTempFileFromUri(imageUri);
        if (tempFile == null) {
            Toast.makeText(this, "이미지 파일을 준비하는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(imageUri)), tempFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", tempFile.getName(), requestFile);

        Call<String> call = apiService.uploadImage(body);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                tempFile.delete(); // 임시 파일 삭제
                if (response.isSuccessful()) {
                    String imageUrl = response.body();
                    Toast.makeText(MainActivity.this, "Upload successful: " + imageUrl, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Upload failed: " + response.code() + " " + response.message(), Toast.LENGTH_SHORT).show();
                    try {
                        Log.e("UPLOAD_ERROR", "Error Body: " + response.errorBody().string());
                    } catch(Exception e) {
                        Log.e("UPLOAD_ERROR", "Error reading error body", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                tempFile.delete(); // 임시 파일 삭제
                Log.e("UPLOAD_FAILURE", "Upload error: " + t.getMessage(), t);
                Toast.makeText(MainActivity.this, "Upload error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private File createTempFileFromUri(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) return null;

        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        String fileName = cursor.getString(nameIndex);
        cursor.close();

        File tempFile = null;
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            tempFile = new File(getCacheDir(), fileName);
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            Log.e("FileCreation", "Error creating temp file", e);
            return null;
        }
        return tempFile;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "알림 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "알림 권한이 거부되었습니다. 설정에서 권한을 허용해주세요.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 102) { // 저장소 권한
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "저장소 접근 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "저장소 접근 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == ADD_PLANT_REQUEST || requestCode == VIEW_PLANT_REQUEST) && resultCode == RESULT_OK) {
            // onResume에서 처리하므로 여기서는 제거
        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadImage(imageUri);
        }
    }

    private void loadPlantData() {
        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(this).getConnection()) {
                String sql = "SELECT p.plant_id, p.nickname, p.species, p.main_image_url, p.watering_cycle, p.last_watered_date, " +
                        "(SELECT GROUP_CONCAT(pm.content SEPARATOR '\n') FROM plant_memos pm WHERE pm.plant_id = p.plant_id) as memos, " +
                        "IFNULL(p.main_image_url, (SELECT pi.image_url FROM plant_images pi WHERE pi.plant_id = p.plant_id ORDER BY pi.image_id DESC LIMIT 1)) as final_image_url " +
                        "FROM plants p WHERE p.user_id = ?";

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, currentUserId);
                    ResultSet rs = pstmt.executeQuery();

                    plantList.clear();
                    while (rs.next()) {
                        long plantId = rs.getLong("plant_id");
                        String nickname = rs.getString("nickname");
                        String species = rs.getString("species");
                        String imageUrl = rs.getString("final_image_url");
                        int waterCycle = rs.getInt("watering_cycle");
                        String lastWateredDate = rs.getString("last_watered_date");
                        String memosConcat = rs.getString("memos");
                        List<String> memos = new ArrayList<>();
                        if (memosConcat != null) {
                            memos.addAll(Arrays.asList(memosConcat.split("\\n")));
                        }

                        // main_image_url이 null이었는데, 대체 이미지를 찾은 경우 DB 업데이트
                        String originalMainUrl = rs.getString("main_image_url");
                        if (originalMainUrl == null && imageUrl != null) {
                            updateMainImageUrl(plantId, imageUrl);
                        }

                        plantList.add(new Plant(plantId, species, nickname, imageUrl, memos, lastWateredDate, waterCycle));
                    }

                    new Handler(Looper.getMainLooper()).post(() -> {
                        adapter.notifyDataSetChanged();
                        tvPlantCount.setText("총 " + plantList.size() + "개의 식물이 등록되어 있어요.");
                        if (plantList.isEmpty()) {
                            Toast.makeText(this, "등록된 식물이 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                        checkWateringNeeded();
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

    private void updateMainImageUrl(long plantId, String imageUrl) {
        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(MainActivity.this).getConnection()) {
                String sql = "UPDATE plants SET main_image_url = ? WHERE plant_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, imageUrl);
                    pstmt.setLong(2, plantId);
                    pstmt.executeUpdate();
                }
            } catch (Exception e) {
                Log.e("DB_UPDATE_ERROR", "Main image URL 업데이트 중 오류 발생", e);
            }
        }).start();
    }


    private void checkWateringNeeded() {
        List<String> plantsToWater = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());

        for (Plant plant : plantList) {
            try {
                Date lastWatered = sdf.parse(plant.getLastWateredDate());
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(lastWatered);
                calendar.add(Calendar.DAY_OF_YEAR, plant.getWaterCycle());
                String nextWateringDay = sdf.format(calendar.getTime());

                if (today.equals(nextWateringDay)) {
                    plantsToWater.add(plant.getNickname());
                }
            } catch (Exception e) {
                Log.e("DATE_PARSE_ERROR", "날짜 파싱 오류", e);
            }
        }

        if (!plantsToWater.isEmpty()) {
            ivNotification.setImageResource(R.drawable.bellpoint); // bellpoint 아이콘으로 교체 필요
            ivNotification.setOnClickListener(v -> showWateringListDialog(plantsToWater));
        } else {
            ivNotification.setImageResource(R.drawable.bell);
            ivNotification.setOnClickListener(null);
        }
    }

    private void showWateringListDialog(List<String> plantsToWater) {
        StringBuilder message = new StringBuilder("오늘 물을 줘야 할 식물:\n\n");
        for (String plantName : plantsToWater) {
            message.append("- ").append(plantName).append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("물주기 알림")
                .setMessage(message.toString())
                .setPositiveButton("확인", null)
                .show();
    }

    private void logout() {
        SharedPreferences sharedPreferences = getSharedPreferences("AutoLoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setupWeatherViewPager() {
        weatherAdapter = new WeatherAdapter(weatherList, this, currentUserId, this);
        weatherViewPager.setAdapter(weatherAdapter);

        weatherViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
            }
        });
    }

    private void loadSavedWeatherData() {
        new Thread(() -> {
            final List<Weather> newWeatherList = new ArrayList<>();
            try (Connection conn = new DatabaseConnector(this).getConnection()) {
                String sql = "SELECT sl.id, sl.region_name, sl.nx, sl.ny, wd.temperature, wd.weather_status, wd.updated_at " +
                             "FROM saved_locations sl LEFT JOIN weather_data wd ON sl.id = wd.location_id " +
                             "WHERE sl.user_id = ? ORDER BY sl.id ASC";

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, currentUserId);
                    ResultSet rs = pstmt.executeQuery();
                    while (rs.next()) {
                        long locationId = rs.getLong("id");
                        String regionName = rs.getString("region_name");
                        double nx = rs.getDouble("nx");
                        double ny = rs.getDouble("ny");
                        String weatherStatus = rs.getString("weather_status");
                        double temperature = rs.getDouble("temperature");
                        Timestamp updatedAt = rs.getTimestamp("updated_at");

                        boolean needsUpdate = (updatedAt == null || (System.currentTimeMillis() - updatedAt.getTime()) > 30 * 60 * 1000);

                        if (needsUpdate) {
                            newWeatherList.add(new Weather(locationId, R.drawable.cloud, regionName, "갱신 중..."));
                            updateWeatherForLocation(locationId, regionName, ny, nx);
                        } else {
                            String temperatureText = String.format(Locale.getDefault(), "%.1f°C %s", temperature, weatherStatus);
                            int weatherIcon = getWeatherIcon(weatherStatus);
                            newWeatherList.add(new Weather(locationId, weatherIcon, regionName, temperatureText));
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("DB_WEATHER_LOAD", "저장된 날씨 정보 로딩 중 오류", e);
            }

            runOnUiThread(() -> {
                weatherList.clear();
                weatherList.addAll(newWeatherList);

                // 저장된 지역이 3개 미만일 때 "지역 추가" 카드 추가
                if (weatherList.size() < 3) {
                    weatherList.add(new Weather(-1, R.drawable.edit, "지역 추가", ""));
                }

                weatherAdapter.notifyDataSetChanged();
                setupIndicators(weatherList.size());
                updateIndicators(weatherViewPager.getCurrentItem()); // 현재 페이지로 인디케이터 업데이트
            });
        }).start();
    }

    private void updateWeatherForLocation(final long locationId, final String regionName, final double lat, final double lon) {
        WeatherApiService weatherService = WeatherService.getWeatherApiService();
        String apiKey = BuildConfig.WEATHER_API_KEY;

        weatherService.getWeatherData(lat, lon, apiKey, "metric", "kr").enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse data = response.body();
                    String description = data.getWeather().get(0).getDescription();
                    double temp = data.getMain().getTemp();

                    saveWeatherDataToDatabase(locationId, temp, description);

                    runOnUiThread(() -> {
                        for (Weather item : weatherList) {
                            if (item.getLocationId() == locationId) {
                                item.setTemperature(String.format(Locale.getDefault(), "%.1f°C %s", temp, description));
                                item.setIcon(getWeatherIcon(description));
                                weatherAdapter.notifyDataSetChanged();
                                break;
                            }
                        }
                    });
                } else {
                    Log.e("WEATHER_UPDATE", "날씨 정보 갱신 실패 for " + regionName);
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Log.e("WEATHER_UPDATE", "날씨 API 호출 실패 for " + regionName, t);
            }
        });
    }

    private void saveWeatherDataToDatabase(long locationId, double temperature, String weatherStatus) {
        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(MainActivity.this).getConnection()) {
                String sql = "INSERT INTO weather_data (location_id, temperature, weather_status) VALUES (?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE temperature = VALUES(temperature), weather_status = VALUES(weather_status), updated_at = CURRENT_TIMESTAMP";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setLong(1, locationId);
                    pstmt.setDouble(2, temperature);
                    pstmt.setString(3, weatherStatus);
                    pstmt.executeUpdate();
                    Log.i("DB_WEATHER_UPDATE", "날씨 정보 DB 갱신 성공, location_id: " + locationId);
                }
            } catch (Exception e) {
                Log.e("DB_WEATHER_UPDATE", "날씨 정보 DB 갱신 오류", e);
            }
        }).start();
    }

    private int getWeatherIcon(String description) {
        if (description == null) return R.drawable.cloud;
        if (description.contains("맑음")) {
            return R.drawable.sunny;
        } else if (description.contains("구름") || description.contains("흐림")) {
            return R.drawable.cloud;
        } else if (description.contains("비")) {
            return R.drawable.rain;
        } else {
            return R.drawable.sunny; // Default icon
        }
    }

    private void setupIndicators(int count) {
        if (count == 0) return;
        indicators = new ImageView[count];
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 12, 0);

        indicatorLayout.removeAllViews();

        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(this);
            indicators[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.tab_indicator_default));
            indicators[i].setLayoutParams(params);
            indicatorLayout.addView(indicators[i]);
        }
    }

    private void updateIndicators(int position) {
        if (indicators == null || indicators.length == 0) return;
        for (int i = 0; i < indicators.length; i++) {
            indicators[i].setImageDrawable(ContextCompat.getDrawable(this,
                    i == position ? R.drawable.tab_indicator_selected : R.drawable.tab_indicator_default));
        }
    }

    private void showAccountSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_account_settings, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        TextView tvChangePassword = view.findViewById(R.id.tv_change_password);
        TextView tvChangeEmail = view.findViewById(R.id.tv_change_email);

        tvChangePassword.setOnClickListener(v -> {
            showChangePasswordDialog();
            dialog.dismiss();
        });

        tvChangeEmail.setOnClickListener(v -> {
            showChangeEmailDialog();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showChangeEmailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_change_email, null);
        builder.setView(view);

        EditText etCurrentEmail = view.findViewById(R.id.et_current_email);
        EditText etNewEmail = view.findViewById(R.id.et_new_email);
        Button btnVerifyEmail = view.findViewById(R.id.btn_verify_email);

        // 현재 이메일 가져오기
        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(this).getConnection()) {
                String sql = "SELECT email FROM users WHERE user_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, currentUserId);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        String currentEmail = rs.getString("email");
                        new Handler(Looper.getMainLooper()).post(() -> etCurrentEmail.setText(currentEmail));
                    }
                }
            } catch (Exception e) {
                Log.e("DB_ERROR", "이메일 로딩 중 오류 발생", e);
            }
        }).start();

        final boolean[] isEmailVerified = {false};

        btnVerifyEmail.setOnClickListener(v -> {
            String newEmail = etNewEmail.getText().toString().trim();
            if (Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                isEmailVerified[0] = true;
                Toast.makeText(this, "이메일이 확인되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                isEmailVerified[0] = false;
                Toast.makeText(this, "유효하지 않은 이메일 형식입니다.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setTitle("이메일 변경")
                .setPositiveButton("저장", (dialog, which) -> {
                    if (!isEmailVerified[0]) {
                        Toast.makeText(this, "이메일 인증을 먼저 진행해주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String newEmail = etNewEmail.getText().toString().trim();
                    updateEmail(newEmail);
                })
                .setNegativeButton("취소", null);

        builder.create().show();
    }

    private void updateEmail(String newEmail) {
        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(this).getConnection()) {
                String sql = "UPDATE users SET email = ? WHERE user_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, newEmail);
                    pstmt.setString(2, currentUserId);
                    int affectedRows = pstmt.executeUpdate();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (affectedRows > 0) {
                            Toast.makeText(this, "이메일이 성공적으로 변경되었습니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "이메일 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("DB_ERROR", "이메일 업데이트 중 오류 발생", e);
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(this, "데이터베이스 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(view);

        EditText etCurrentPassword = view.findViewById(R.id.et_current_password);
        EditText etNewPassword = view.findViewById(R.id.et_new_password);
        EditText etConfirmNewPassword = view.findViewById(R.id.et_confirm_new_password);

        etCurrentPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etCurrentPassword.getRight() - etCurrentPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    togglePasswordVisibility(etCurrentPassword);
                    return true;
                }
            }
            return false;
        });

        etNewPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etNewPassword.getRight() - etNewPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    togglePasswordVisibility(etNewPassword);
                    return true;
                }
            }
            return false;
        });

        etConfirmNewPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (etConfirmNewPassword.getRight() - etConfirmNewPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    togglePasswordVisibility(etConfirmNewPassword);
                    return true;
                }
            }
            return false;
        });


        builder.setTitle("비밀번호 변경")
                .setPositiveButton("저장", (dialog, which) -> {
                    String currentPassword = etCurrentPassword.getText().toString();
                    String newPassword = etNewPassword.getText().toString();
                    String confirmNewPassword = etConfirmNewPassword.getText().toString();

                    if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
                        Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newPassword.equals(confirmNewPassword)) {
                        Toast.makeText(this, "새 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!isValidPassword(newPassword)) {
                        Toast.makeText(this, "비밀번호는 8글자 이상, 숫자, 기호를 포함해야 합니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updatePassword(currentPassword, newPassword);
                })
                .setNegativeButton("취소", null);

        builder.create().show();
    }

    private void togglePasswordVisibility(EditText editText) {
        Typeface typeface = editText.getTypeface();
        if (editText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_on, 0);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_off, 0);
        }
        editText.setTypeface(typeface);
        editText.setSelection(editText.getText().length());
    }

    private void updatePassword(String currentPassword, String newPassword) {
        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(this).getConnection()) {
                // 1. 현재 비밀번호 확인
                String checkSql = "SELECT password FROM users WHERE user_id = ?";
                try (PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
                    checkPstmt.setString(1, currentUserId);
                    ResultSet rs = checkPstmt.executeQuery();

                    if (rs.next()) {
                        String dbPassword = rs.getString("password");
                        if (dbPassword.equals(currentPassword)) {
                            // 2. 비밀번호 업데이트
                            String updateSql = "UPDATE users SET password = ? WHERE user_id = ?";
                            try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                                updatePstmt.setString(1, newPassword);
                                updatePstmt.setString(2, currentUserId);
                                int affectedRows = updatePstmt.executeUpdate();
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (affectedRows > 0) {
                                        Toast.makeText(this, "비밀번호가 성공적으로 변경되었습니다.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(this, "비밀번호 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(this, "현재 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(this, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show());
                    }
                }
            } catch (Exception e) {
                Log.e("DB_ERROR", "비밀번호 업데이트 중 오류 발생", e);
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(this, "데이터베이스 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 8) {
            return false;
        }
        Pattern hasNumber = Pattern.compile("[0-9]");
        Pattern hasSymbol = Pattern.compile("[^a-zA-Z0-9]");
        return hasNumber.matcher(password).find() && hasSymbol.matcher(password).find();
    }

    private void showResetDataConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("데이터 초기화")
                .setMessage("저장된 모든 식물 데이터가 삭제됩니다. 진행하시겠습니까?")
                .setPositiveButton("확인", (dialog, which) -> resetPlantData())
                .setNegativeButton("취소", null)
                .show();
    }

    private void resetPlantData() {
        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(this).getConnection()) {
                // CASCADE 설정으로 plants 테이블 데이터만 삭제해도 plant_memos, plant_images 데이터도 삭제됨
                String sql = "DELETE FROM plants WHERE user_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, currentUserId);
                    int affectedRows = pstmt.executeUpdate();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (affectedRows > 0) {
                            Toast.makeText(this, "모든 식물 데이터가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                            loadPlantData(); // 목록 새로고침
                        } else {
                            Toast.makeText(this, "삭제할 데이터가 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("DB_ERROR", "데이터 초기화 중 오류 발생", e);
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(this, "데이터 초기화에 실패했습니다.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    private void showNotificationSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_notification_settings, null);
        builder.setView(view);

        // 기존 물주기 알림 UI
        SwitchMaterial switchNotification = view.findViewById(R.id.switch_notification);
        TimePicker timePicker = view.findViewById(R.id.time_picker_notification);

        // 새로운 날씨 알림 UI
        SwitchMaterial switchWeatherNotification = view.findViewById(R.id.switch_weather_notification);
        LinearLayout layoutLowTemp = view.findViewById(R.id.layout_low_temp);
        SwitchMaterial switchLowTemp = view.findViewById(R.id.switch_low_temp);
        EditText etLowTemp = view.findViewById(R.id.et_low_temp);
        LinearLayout layoutHighTemp = view.findViewById(R.id.layout_high_temp);
        SwitchMaterial switchHighTemp = view.findViewById(R.id.switch_high_temp);
        EditText etHighTemp = view.findViewById(R.id.et_high_temp);
        CheckBox cbRain = view.findViewById(R.id.cb_rain);
        CheckBox cbSnow = view.findViewById(R.id.cb_snow);

        // SharedPreferences에서 설정 값 불러오기
        SharedPreferences prefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE);
        // 물주기 알림
        switchNotification.setChecked(prefs.getBoolean("isNotificationEnabled", true));
        timePicker.setHour(prefs.getInt("notificationHour", 9));
        timePicker.setMinute(prefs.getInt("notificationMinute", 0));
        // 날씨 알림
        boolean isWeatherEnabled = prefs.getBoolean("isWeatherNotificationEnabled", false);
        switchWeatherNotification.setChecked(isWeatherEnabled);
        switchLowTemp.setChecked(prefs.getBoolean("isLowTempEnabled", false));
        etLowTemp.setText(String.valueOf(prefs.getInt("lowTempThreshold", 10)));
        switchHighTemp.setChecked(prefs.getBoolean("isHighTempEnabled", false));
        etHighTemp.setText(String.valueOf(prefs.getInt("highTempThreshold", 30)));
        cbRain.setChecked(prefs.getBoolean("isRainAlertEnabled", false));
        cbSnow.setChecked(prefs.getBoolean("isSnowAlertEnabled", false));

        // 날씨 알림 메인 스위치에 따라 하위 설정 활성화/비활성화
        layoutLowTemp.setVisibility(isWeatherEnabled ? View.VISIBLE : View.GONE);
        layoutHighTemp.setVisibility(isWeatherEnabled ? View.VISIBLE : View.GONE);
        cbRain.setVisibility(isWeatherEnabled ? View.VISIBLE : View.GONE);
        cbSnow.setVisibility(isWeatherEnabled ? View.VISIBLE : View.GONE);

        switchWeatherNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int visibility = isChecked ? View.VISIBLE : View.GONE;
            layoutLowTemp.setVisibility(visibility);
            layoutHighTemp.setVisibility(visibility);
            cbRain.setVisibility(visibility);
            cbSnow.setVisibility(visibility);
        });

        builder.setTitle("알림 설정")
                .setPositiveButton("저장", (dialog, which) -> {
                    SharedPreferences.Editor editor = prefs.edit();

                    // 물주기 알림 저장
                    boolean isWateringNotificationEnabled = switchNotification.isChecked();
                    int hour = timePicker.getHour();
                    int minute = timePicker.getMinute();

                    editor.putBoolean("isNotificationEnabled", isWateringNotificationEnabled);
                    editor.putInt("notificationHour", hour);
                    editor.putInt("notificationMinute", minute);

                    if (isWateringNotificationEnabled) {
                        scheduleWateringAlarm(hour, minute);
                    } else {
                        cancelWateringAlarm();
                    }

                    // 날씨 알림 저장
                    boolean isWeatherNotificationEnabled = switchWeatherNotification.isChecked();
                    editor.putBoolean("isWeatherNotificationEnabled", isWeatherNotificationEnabled);
                    editor.putBoolean("isLowTempEnabled", switchLowTemp.isChecked());
                    editor.putInt("lowTempThreshold", Integer.parseInt(etLowTemp.getText().toString()));
                    editor.putBoolean("isHighTempEnabled", switchHighTemp.isChecked());
                    editor.putInt("highTempThreshold", Integer.parseInt(etHighTemp.getText().toString()));
                    editor.putBoolean("isRainAlertEnabled", cbRain.isChecked());
                    editor.putBoolean("isSnowAlertEnabled", cbSnow.isChecked());

                    if (isWeatherNotificationEnabled) {
                        scheduleWeatherCheck();
                    } else {
                        cancelWeatherCheck();
                    }

                    editor.apply();
                    Toast.makeText(this, "알림 설정이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("취소", null);

        builder.create().show();
    }

    private void scheduleWeatherCheck() {
        PeriodicWorkRequest weatherWorkRequest =
                new PeriodicWorkRequest.Builder(WeatherCheckWorker.class, 1, TimeUnit.HOURS)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                WEATHER_CHECK_WORK_TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                weatherWorkRequest
        );
        Toast.makeText(this, "날씨 확인 작업이 예약되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private void cancelWeatherCheck() {
        WorkManager.getInstance(this).cancelUniqueWork(WEATHER_CHECK_WORK_TAG);
        Toast.makeText(this, "날씨 확인 작업이 취소되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private void scheduleWateringAlarm(int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // 만약 설정한 시간이 이미 지났다면, 다음 날로 설정
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );

        Toast.makeText(this, "알림이 " + hour + "시 " + minute + "분에 설정되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private void cancelWateringAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
        Toast.makeText(this, "알림이 해제되었습니다.", Toast.LENGTH_SHORT).show();
    }


    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
            }
        }
    }
}
