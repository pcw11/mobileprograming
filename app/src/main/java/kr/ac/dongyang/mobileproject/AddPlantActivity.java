package kr.ac.dongyang.mobileproject;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class AddPlantActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY_IMAGE = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String KEY_PHOTO_URI = "key_photo_uri";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView ivMenu;
    private TextView tvGreeting;
    private EditText etPlantSpecies, etPlantNickname;
    private ImageView ivWaterEdit, ivMemoAdd, ivMemoEdit, ivPhotoAdd, ivPhotoEdit;
    private TextView tvWaterSubtitle;
    private Button btnSave;
    private RecyclerView rvMemos, rvPhotos;
    private LinearLayout llWaterDates;
    private MemoAdapter memoAdapter;
    private PhotoAdapter photoAdapter;
    private List<String> memoList = new ArrayList<>();
    private List<String> photoList = new ArrayList<>(); // 로컬 Uri 또는 서버 URL 저장

    private int wateringCycle = 7; // 기본 물 주기 7일
    private String userId;
    private boolean isMemoEditMode = false;
    private boolean isPhotoEditMode = false;
    private Date lastWateredDate;
    private FileUploadManager fileUploadManager;
    private Uri photoURI;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plant);

        if (savedInstanceState != null) {
            photoURI = savedInstanceState.getParcelable(KEY_PHOTO_URI);
        }

        // 뷰 초기화
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        ivMenu = findViewById(R.id.iv_menu);
        tvGreeting = findViewById(R.id.tv_greeting_add);
        etPlantSpecies = findViewById(R.id.et_plant_species);
        etPlantNickname = findViewById(R.id.et_plant_nickname);
        ivWaterEdit = findViewById(R.id.iv_water_edit);
        tvWaterSubtitle = findViewById(R.id.tv_water_subtitle);
        ivPhotoAdd = findViewById(R.id.iv_photo_add);
        ivPhotoEdit = findViewById(R.id.iv_photo_edit);
        ivMemoAdd = findViewById(R.id.iv_memo_add);
        ivMemoEdit = findViewById(R.id.iv_memo_edit);
        rvMemos = findViewById(R.id.rv_memos);
        rvPhotos = findViewById(R.id.rv_photos);
        llWaterDates = findViewById(R.id.ll_water_dates);
        btnSave = findViewById(R.id.btn_save);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        ViewGroup.LayoutParams params = navigationView.getLayoutParams();
        params.width = (int) (width * 0.66);
        navigationView.setLayoutParams(params);

        fileUploadManager = new FileUploadManager(this);

        // MainActivity로부터 사용자 ID를 받아 환영 메시지 설정
        Intent intent = getIntent();
        userId = intent.getStringExtra("USER_ID");
        tvGreeting.setText("식물 추가");

        lastWateredDate = new Date(System.currentTimeMillis()); // 오늘을 마지막 물 준 날짜로 초기화

        setupRecyclerViews();
        updateDateViews();

        ivMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));
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
                Toast.makeText(AddPlantActivity.this, "준비 중인 기능입니다.", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });

        // 물 주기 수정 아이콘 클릭 리스너
        ivWaterEdit.setOnClickListener(v -> showWateringCycleDialog());

        // 사진 추가/수정 버튼 리스너
        ivPhotoAdd.setOnClickListener(v -> showImageSourceDialog());
        ivPhotoEdit.setOnClickListener(v -> togglePhotoEditMode());
        // 메모 추가/수정 버튼 리스너
        ivMemoAdd.setOnClickListener(v -> {
            int insertPosition = memoList.size();
            memoList.add("");
            memoAdapter.notifyItemInserted(insertPosition);
            rvMemos.scrollToPosition(insertPosition);
        });
        ivMemoEdit.setOnClickListener(v -> toggleMemoEditMode());

        // 저장 버튼 클릭 리스너
        btnSave.setOnClickListener(v -> savePlantData());

        // 초기 물 주기 텍스트 설정
        tvWaterSubtitle.setText("물 주기는 " + wateringCycle + "일 입니다.");
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

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (photoURI != null) {
            outState.putParcelable(KEY_PHOTO_URI, photoURI);
        }
    }

    private void setupRecyclerViews() {
        rvMemos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        memoAdapter = new MemoAdapter(memoList);
        rvMemos.setAdapter(memoAdapter);

        rvPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        photoAdapter = new PhotoAdapter(photoList);
        rvPhotos.setAdapter(photoAdapter);
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
            updateDateViews(); // 날짜 뷰 업데이트
            Toast.makeText(AddPlantActivity.this, "물 주기가 " + wateringCycle + "일로 설정되었습니다.", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("취소", null);

        builder.create().show();
    }

    private void updateDateViews() {
        llWaterDates.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DAY_OF_MONTH);

        Calendar nextWateringCal = Calendar.getInstance();
        nextWateringCal.setTime(lastWateredDate);
        nextWateringCal.add(Calendar.DAY_OF_MONTH, wateringCycle);

        calendar.add(Calendar.DAY_OF_MONTH, -3);

        for (int i = 0; i < 8; i++) {
            View dateView = inflater.inflate(R.layout.item_date, llWaterDates, false);
            TextView tvDayOfWeek = dateView.findViewById(R.id.tv_day_of_week);
            TextView tvDate = dateView.findViewById(R.id.tv_date);
            ImageView ivTodayDot = dateView.findViewById(R.id.iv_today_dot);
            ImageView ivWaterBg = dateView.findViewById(R.id.iv_water_bg);

            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

            tvDate.setText(String.valueOf(day));

            String[] daysOfWeek = {"", "일", "월", "화", "수", "목", "금", "토"};
            tvDayOfWeek.setText(daysOfWeek[dayOfWeek]);

            if (day == today) {
                ivTodayDot.setVisibility(View.VISIBLE);
            } else {
                ivTodayDot.setVisibility(View.GONE);
            }

            if (calendar.get(Calendar.YEAR) == nextWateringCal.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == nextWateringCal.get(Calendar.DAY_OF_YEAR)) {
                ivWaterBg.setVisibility(View.VISIBLE);
            } else {
                ivWaterBg.setVisibility(View.GONE);
            }

            int color = ContextCompat.getColor(this, R.color.black);
            if (dayOfWeek == Calendar.SATURDAY) {
                color = ContextCompat.getColor(this, R.color.blue);
            } else if (dayOfWeek == Calendar.SUNDAY) {
                color = ContextCompat.getColor(this, R.color.red);
            }
            tvDate.setTextColor(color);
            tvDayOfWeek.setTextColor(color);

            llWaterDates.addView(dateView);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void toggleMemoEditMode() {
        isMemoEditMode = !isMemoEditMode;
        memoAdapter.setEditMode(isMemoEditMode);
    }

    private void togglePhotoEditMode() {
        isPhotoEditMode = !isPhotoEditMode;
        photoAdapter.setEditMode(isPhotoEditMode);
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("사진 추가");
        builder.setItems(new CharSequence[]{"카메라로 촬영", "갤러리에서 선택"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    checkCameraPermission();
                    break;
                case 1:
                    openGallery();
                    break;
            }
        });
        builder.show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("IMAGE_CAPTURE", "Error creating image file", ex);
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this, "kr.ac.dongyang.mobileproject.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new java.util.Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "카메라 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri selectedImageUri = null;
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                selectedImageUri = photoURI;
            } else if (requestCode == REQUEST_GALLERY_IMAGE && data != null && data.getData() != null) {
                selectedImageUri = data.getData();
            }

            if (selectedImageUri != null) {
                // AddPlantActivity에서는 즉시 업로드하지 않고, Uri를 리스트에 추가
                photoList.add(0, selectedImageUri.toString());
                photoAdapter.notifyItemInserted(0);
            }
        }
    }

    private void savePlantData() {
        String species = etPlantSpecies.getText().toString().trim();
        String nickname = etPlantNickname.getText().toString().trim();

        if (species.isEmpty()) {
            Toast.makeText(this, "식물 종을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> memos = memoAdapter.getMemos();

        // 1. 이미지들을 서버에 업로드하고 URL 목록을 받음
        uploadImagesAndSavePlant(species, nickname, memos);
    }

    private void uploadImagesAndSavePlant(String species, String nickname, List<String> memos) {
        List<String> imageUrls = new ArrayList<>();
        if (photoList.isEmpty()) {
            // 이미지가 없으면 바로 식물 정보 저장
            insertPlantData(species, nickname, memos, imageUrls);
            return;
        }

        // 여러 이미지를 순차적으로 업로드
        for (int i = 0; i < photoList.size(); i++) {
            Uri imageUri = Uri.parse(photoList.get(i));
            int finalI = i;
            fileUploadManager.uploadImage(imageUri, new FileUploadManager.FileUploadCallback() {
                @Override
                public void onUploadSuccess(String filename) {
                    String imageUrl = "http://" + BuildConfig.SERVER_IP + ":6006/download/" + filename;
                    imageUrls.add(imageUrl);
                    // 모든 이미지 업로드가 완료되면 DB에 저장
                    if (imageUrls.size() == photoList.size()) {
                        insertPlantData(species, nickname, memos, imageUrls);
                    }
                }

                @Override
                public void onUploadFailure(String message) {
                    showToast("이미지 업로드 실패: " + message);
                    // 실패하더라도 진행을 멈추지 않고, 업로드된 것들만 저장할 수 있음
                    // 혹은 여기서 전체 프로세스를 중단할 수도 있음
                    if (imageUrls.size() == photoList.size() -1) { //실패한 1개를 제외하고 모든게 업로드되었을때
                        insertPlantData(species, nickname, memos, imageUrls);
                    }
                }
            });
        }
    }

    private void insertPlantData(String species, String nickname, List<String> memos, List<String> imageUrls) {
        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(this).getConnection()) {
                String mainImageUrl = imageUrls.isEmpty() ? null : imageUrls.get(0);

                String sql = "INSERT INTO plants (user_id, species, nickname, watering_cycle, last_watered_date, main_image_url) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, userId);
                    pstmt.setString(2, species);
                    pstmt.setString(3, nickname);
                    pstmt.setInt(4, wateringCycle);
                    pstmt.setDate(5, new Date(System.currentTimeMillis()));
                    pstmt.setString(6, mainImageUrl);

                    int affectedRows = pstmt.executeUpdate();

                    if (affectedRows > 0) {
                        try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                long plantId = generatedKeys.getLong(1);

                                // 메모 저장
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

                                // 이미지 URL 저장
                                if (!imageUrls.isEmpty()) {
                                    String imageSql = "INSERT INTO plant_images (plant_id, image_url) VALUES (?, ?)";
                                    try (PreparedStatement imagePstmt = conn.prepareStatement(imageSql)) {
                                        for (String imageUrl : imageUrls) {
                                            imagePstmt.setLong(1, plantId);
                                            imagePstmt.setString(2, imageUrl);
                                            imagePstmt.addBatch();
                                        }
                                        imagePstmt.executeBatch();
                                    }
                                }

                                showToast("식물이 성공적으로 등록되었습니다.");
                                setResult(RESULT_OK);
                                finish();
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

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        });
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
                    pstmt.setString(1, userId);
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
                    pstmt.setString(2, userId);
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

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(view);

        EditText etCurrentPassword = view.findViewById(R.id.et_current_password);
        EditText etNewPassword = view.findViewById(R.id.et_new_password);
        EditText etConfirmNewPassword = view.findViewById(R.id.et_confirm_new_password);

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

    private void updatePassword(String currentPassword, String newPassword) {
        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(this).getConnection()) {
                // 1. 현재 비밀번호 확인
                String checkSql = "SELECT password FROM users WHERE user_id = ?";
                try (PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
                    checkPstmt.setString(1, userId);
                    ResultSet rs = checkPstmt.executeQuery();

                    if (rs.next()) {
                        String dbPassword = rs.getString("password");
                        if (dbPassword.equals(currentPassword)) {
                            // 2. 비밀번호 업데이트
                            String updateSql = "UPDATE users SET password = ? WHERE user_id = ?";
                            try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                                updatePstmt.setString(1, newPassword);
                                updatePstmt.setString(2, userId);
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
                    pstmt.setString(1, userId);
                    int affectedRows = pstmt.executeUpdate();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (affectedRows > 0) {
                            Toast.makeText(this, "모든 식물 데이터가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                            finish(); // 목록 새로고침 대신 액티비티 종료
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
                    editor.putBoolean("isNotificationEnabled", switchNotification.isChecked());
                    editor.putInt("notificationHour", timePicker.getHour());
                    editor.putInt("notificationMinute", timePicker.getMinute());

                    // 날씨 알림 저장
                    editor.putBoolean("isWeatherNotificationEnabled", switchWeatherNotification.isChecked());
                    editor.putBoolean("isLowTempEnabled", switchLowTemp.isChecked());
                    editor.putInt("lowTempThreshold", Integer.parseInt(etLowTemp.getText().toString()));
                    editor.putBoolean("isHighTempEnabled", switchHighTemp.isChecked());
                    editor.putInt("highTempThreshold", Integer.parseInt(etHighTemp.getText().toString()));
                    editor.putBoolean("isRainAlertEnabled", cbRain.isChecked());
                    editor.putBoolean("isSnowAlertEnabled", cbSnow.isChecked());

                    editor.apply();

                    // TODO: WorkManager를 사용하여 백그라운드 날씨 확인 작업 스케줄링/취소 로직 추가

                    Toast.makeText(this, "알림 설정이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("취소", null);

        builder.create().show();
    }


    class PhotoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_PHOTO = 0;
        private static final int VIEW_TYPE_ADD = 1;

        private List<String> photos; // 이 리스트는 Uri 문자열을 담고 있음
        private boolean isEditMode = false;

        public PhotoAdapter(List<String> photos) {
            this.photos = photos;
        }

        public void setEditMode(boolean editMode) {
            this.isEditMode = editMode;
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return (position == photos.size()) ? VIEW_TYPE_ADD : VIEW_TYPE_PHOTO;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == VIEW_TYPE_PHOTO) {
                View view = inflater.inflate(R.layout.item_photo, parent, false);
                return new PhotoViewHolder(view);
            } else {
                View view = inflater.inflate(R.layout.add_button, parent, false);
                return new AddButtonViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == VIEW_TYPE_PHOTO) {
                PhotoViewHolder photoHolder = (PhotoViewHolder) holder;
                Uri imageUri = Uri.parse(photos.get(position));
                Glide.with(photoHolder.imageView.getContext())
                     .load(imageUri)
                     .into(photoHolder.imageView);

                photoHolder.ivDeletePhoto.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
                photoHolder.ivDeletePhoto.setOnClickListener(v -> {
                    int currentPosition = photoHolder.getAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        photos.remove(currentPosition);
                        notifyItemRemoved(currentPosition);
                    }
                });
            } else {
                AddButtonViewHolder addButtonHolder = (AddButtonViewHolder) holder;
                addButtonHolder.addButton.setOnClickListener(v -> showImageSourceDialog());
            }
        }

        @Override
        public int getItemCount() {
            return photos.size() + 1;
        }

        class PhotoViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            ImageView ivDeletePhoto;

            public PhotoViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.iv_photo);
                ivDeletePhoto = itemView.findViewById(R.id.iv_delete_photo);
            }
        }

        class AddButtonViewHolder extends RecyclerView.ViewHolder {
            ImageButton addButton;
            public AddButtonViewHolder(@NonNull View itemView) {
                super(itemView);
                addButton = itemView.findViewById(R.id.circular_add_button);
            }
        }
    }

    class MemoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_MEMO = 0;
        private static final int VIEW_TYPE_ADD = 1;

        private List<String> memos;
        private boolean isEditMode = false;

        public MemoAdapter(List<String> memos) {
            this.memos = memos;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == memos.size()) {
                return VIEW_TYPE_ADD;
            } else {
                return VIEW_TYPE_MEMO;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == VIEW_TYPE_MEMO) {
                View view = inflater.inflate(R.layout.item_memo, parent, false);
                return new MemoViewHolder(view);
            } else { // VIEW_TYPE_ADD
                View view = inflater.inflate(R.layout.add_button, parent, false);
                return new AddButtonViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == VIEW_TYPE_MEMO) {
                MemoViewHolder memoHolder = (MemoViewHolder) holder;
                String memoText = memos.get(position);
                memoHolder.etMemoContent.setText(memoText);
                memoHolder.ivDeleteMemo.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

                memoHolder.ivDeleteMemo.setOnClickListener(v -> {
                    int currentPosition = memoHolder.getAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        memos.remove(currentPosition);
                        notifyItemRemoved(currentPosition);
                    }
                });
            } else {
                AddButtonViewHolder addButtonHolder = (AddButtonViewHolder) holder;
                addButtonHolder.addButton.setOnClickListener(v -> {
                    int insertPosition = memos.size();
                    memos.add("");
                    notifyItemInserted(insertPosition);
                    rvMemos.scrollToPosition(insertPosition);
                });
            }
        }

        @Override
        public int getItemCount() {
            return memos.size() + 1;
        }

        public void setEditMode(boolean editMode) {
            this.isEditMode = editMode;
            notifyDataSetChanged();
        }

        public List<String> getMemos() {
            List<String> currentMemos = new ArrayList<>();
            for (int i = 0; i < memos.size(); i++) {
                MemoViewHolder holder = (MemoViewHolder) rvMemos.findViewHolderForAdapterPosition(i);
                if (holder != null) {
                    memos.set(i, holder.etMemoContent.getText().toString());
                }
            }
            for (String memo : memos) {
                if (memo != null && !memo.trim().isEmpty()) {
                    currentMemos.add(memo);
                }
            }
            return currentMemos;
        }

        class MemoViewHolder extends RecyclerView.ViewHolder {
            EditText etMemoContent;
            ImageView ivDeleteMemo;

            public MemoViewHolder(@NonNull View itemView) {
                super(itemView);
                etMemoContent = itemView.findViewById(R.id.et_memo_content);
                ivDeleteMemo = itemView.findViewById(R.id.iv_delete_memo);
            }
        }

        class AddButtonViewHolder extends RecyclerView.ViewHolder {
            ImageButton addButton;

            public AddButtonViewHolder(@NonNull View itemView) {
                super(itemView);
                addButton = itemView.findViewById(R.id.circular_add_button);
            }
        }
    }
}
