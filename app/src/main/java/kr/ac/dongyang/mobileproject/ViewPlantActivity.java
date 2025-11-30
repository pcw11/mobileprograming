package kr.ac.dongyang.mobileproject;

import android.content.Context;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ViewPlantActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY_IMAGE = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String KEY_PHOTO_URI = "key_photo_uri";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView ivMenu;
    private EditText etPlantSpecies, etPlantNickname;
    private ImageView ivWaterEdit, ivMemoAdd, ivSearchIcon, ivMemoEdit, ivPhotoAdd;
    private TextView tvGreeting, tvWaterSubtitle;
    private Button btnSave;
    private RecyclerView rvMemos, rvPhotos;
    private LinearLayout llWaterDates;
    private MemoAdapter memoAdapter;
    private PhotoAdapter photoAdapter;
    private List<String> memoList = new ArrayList<>();
    private List<String> photoList = new ArrayList<>();

    private int wateringCycle = 7;
    private long plantId;
    private boolean isEditMode = false;
    private Date lastWateredDate;
    private FileUploadManager fileUploadManager;
    private Uri photoURI;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_plant);

        if (savedInstanceState != null) {
            photoURI = savedInstanceState.getParcelable(KEY_PHOTO_URI);
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        ivMenu = findViewById(R.id.iv_menu);
        etPlantSpecies = findViewById(R.id.et_plant_species);
        etPlantNickname = findViewById(R.id.et_plant_nickname);
        ivWaterEdit = findViewById(R.id.iv_water_edit);
        tvWaterSubtitle = findViewById(R.id.tv_water_subtitle);
        ivMemoAdd = findViewById(R.id.iv_memo_add);
        ivMemoEdit = findViewById(R.id.iv_memo_edit);
        rvMemos = findViewById(R.id.rv_memos);
        rvPhotos = findViewById(R.id.rv_photos);
        llWaterDates = findViewById(R.id.ll_water_dates);
        btnSave = findViewById(R.id.btn_save_plant);
        ivSearchIcon = findViewById(R.id.iv_search_icon);
        tvGreeting = findViewById(R.id.tv_greeting_add);
        ivPhotoAdd = findViewById(R.id.iv_photo_add);

        fileUploadManager = new FileUploadManager(this);

        Intent intent = getIntent();
        plantId = intent.getLongExtra("PLANT_ID", -1);

        if (plantId == -1) {
            Toast.makeText(this, "식물 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupRecyclerViews();
        loadPlantDetails();

        ivMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                logout();
            } else {
                Toast.makeText(ViewPlantActivity.this, "준비 중인 기능입니다.", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });

        ivWaterEdit.setOnClickListener(v -> showWateringCycleDialog());
        ivMemoAdd.setVisibility(View.GONE);
        ivMemoEdit.setOnClickListener(v -> toggleMemoEditMode());
        btnSave.setOnClickListener(v -> updatePlantData());
        ivSearchIcon.setOnClickListener(v -> {
            String species = etPlantSpecies.getText().toString();
            if (!species.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + species));
                startActivity(browserIntent);
            }
        });
        ivPhotoAdd.setOnClickListener(v -> showImageSourceDialog());
    }

    private void logout() {
        SharedPreferences sharedPreferences = getSharedPreferences("AutoLoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(ViewPlantActivity.this, LoginActivity.class);
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

    private void loadPlantDetails() {
        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(this).getConnection()) {
                // 식물 기본 정보 로드
                String plantSql = "SELECT p.species, p.nickname, p.watering_cycle, p.user_id, p.last_watered_date FROM plants p WHERE p.plant_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(plantSql)) {
                    pstmt.setLong(1, plantId);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        String species = rs.getString("species");
                        String nickname = rs.getString("nickname");
                        String userId = rs.getString("user_id");
                        wateringCycle = rs.getInt("watering_cycle");
                        lastWateredDate = rs.getDate("last_watered_date");

                        new Handler(Looper.getMainLooper()).post(() -> {
                            tvGreeting.setText(userId + "님 안녕하세요!");
                            etPlantSpecies.setText(species);
                            etPlantNickname.setText(nickname);
                            tvWaterSubtitle.setText("물 주기는 " + wateringCycle + "일 입니다.");
                            updateDateViews();
                        });
                    }
                }

                // 메모 로드
                String memoSql = "SELECT content FROM plant_memos WHERE plant_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(memoSql)) {
                    pstmt.setLong(1, plantId);
                    ResultSet rs = pstmt.executeQuery();
                    memoList.clear();
                    while (rs.next()) {
                        memoList.add(rs.getString("content"));
                    }
                    new Handler(Looper.getMainLooper()).post(() -> memoAdapter.notifyDataSetChanged());
                }

                // 사진 로드
                String photoSql = "SELECT image_url FROM plant_images WHERE plant_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(photoSql)) {
                    pstmt.setLong(1, plantId);
                    ResultSet rs = pstmt.executeQuery();
                    photoList.clear();
                    while (rs.next()) {
                        photoList.add(rs.getString("image_url"));
                    }
                    new Handler(Looper.getMainLooper()).post(() -> photoAdapter.notifyDataSetChanged());
                }

            } catch (Exception e) {
                Log.e("DB_ERROR", "식물 상세 정보 로딩 중 오류 발생", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(this, "상세 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
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
                Toast.makeText(this, "사진 파일을 생성하는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "kr.ac.dongyang.mobileproject.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new java.util.Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return image;
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
            } else if (requestCode == REQUEST_GALLERY_IMAGE && data != null) {
                selectedImageUri = data.getData();
            }

            if (selectedImageUri != null) {
                uploadImageToServer(selectedImageUri);
            }
        }
    }

    private void uploadImageToServer(Uri imageUri) {
        fileUploadManager.uploadImage(imageUri, new FileUploadManager.FileUploadCallback() {
            @Override
            public void onUploadSuccess(String filename) {
                String imageUrl = "http://" + BuildConfig.SERVER_IP + ":6006/download/" + filename;
                saveImageUrlToDatabase(imageUrl);
            }

            @Override
            public void onUploadFailure(String message) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(ViewPlantActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void saveImageUrlToDatabase(String imageUrl) {
        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(ViewPlantActivity.this).getConnection()) {
                String sql = "INSERT INTO plant_images (plant_id, image_url) VALUES (?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setLong(1, plantId);
                    pstmt.setString(2, imageUrl);
                    int affectedRows = pstmt.executeUpdate();

                    if (affectedRows > 0) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(ViewPlantActivity.this, "사진이 추가되었습니다.", Toast.LENGTH_SHORT).show();
                            photoList.add(imageUrl);
                            photoAdapter.notifyItemInserted(photoList.size() - 1);
                            setResult(RESULT_OK); // MainActivity에 변경사항 알림
                        });
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(ViewPlantActivity.this, "데이터베이스에 사진 저장 실패", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("DB_ERROR", "사진 URL 저장 중 오류 발생", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(ViewPlantActivity.this, "데이터베이스 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }


    // (기존의 다른 메서드들은 여기에 그대로 유지됩니다)
    // ... showWateringCycleDialog, updateDateViews, toggleMemoEditMode, updatePlantData, MemoAdapter 등 ...
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
            updateDateViews();
            Toast.makeText(this, "물 주기가 " + wateringCycle + "일로 설정되었습니다.", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("취소", null);

        builder.create().show();
    }

    private void updateDateViews() {
        llWaterDates.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DAY_OF_MONTH);

        Calendar nextWateringCal = null;
        if (lastWateredDate != null) {
            nextWateringCal = Calendar.getInstance();
            nextWateringCal.setTime(lastWateredDate);
            nextWateringCal.add(Calendar.DAY_OF_MONTH, wateringCycle);
        }

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

            if (nextWateringCal != null &&
                calendar.get(Calendar.YEAR) == nextWateringCal.get(Calendar.YEAR) &&
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
        isEditMode = !isEditMode;
        memoAdapter.setEditMode(isEditMode);
    }

    private void updatePlantData() {
        String species = etPlantSpecies.getText().toString().trim();
        String nickname = etPlantNickname.getText().toString().trim();

        if (species.isEmpty()) {
            Toast.makeText(this, "식물 종을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> memosToSave = memoAdapter.getMemos();

        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(this).getConnection()) {
                // 1. Update plant info
                String sql = "UPDATE plants SET species = ?, nickname = ?, watering_cycle = ? WHERE plant_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, species);
                    pstmt.setString(2, nickname);
                    pstmt.setInt(3, wateringCycle);
                    pstmt.setLong(4, plantId);
                    pstmt.executeUpdate();
                }

                // 2. Delete existing memos
                String deleteMemoSql = "DELETE FROM plant_memos WHERE plant_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteMemoSql)) {
                    pstmt.setLong(1, plantId);
                    pstmt.executeUpdate();
                }

                // 3. Insert new memos
                if (!memosToSave.isEmpty()) {
                    String insertMemoSql = "INSERT INTO plant_memos (plant_id, content) VALUES (?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertMemoSql)) {
                        for (String memo : memosToSave) {
                             if (!memo.trim().isEmpty()) {
                                pstmt.setLong(1, plantId);
                                pstmt.setString(2, memo);
                                pstmt.addBatch();
                            }
                        }
                        pstmt.executeBatch();
                    }
                }

                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(this, "식물 정보가 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });

            } catch (Exception e) {
                Log.e("DB_ERROR", "식물 정보 수정 중 오류 발생", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(this, "정보 수정에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    class PhotoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_PHOTO = 0;
        private static final int VIEW_TYPE_ADD = 1;

        private List<String> photos;

        public PhotoAdapter(List<String> photos) {
            this.photos = photos;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == photos.size()) {
                return VIEW_TYPE_ADD;
            } else {
                return VIEW_TYPE_PHOTO;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == VIEW_TYPE_PHOTO) {
                View view = inflater.inflate(R.layout.item_photo, parent, false);
                return new PhotoViewHolder(view);
            } else { // VIEW_TYPE_ADD
                View view = inflater.inflate(R.layout.add_button, parent, false);
                return new AddButtonViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == VIEW_TYPE_PHOTO) {
                PhotoViewHolder photoHolder = (PhotoViewHolder) holder;
                String imageUrl = photos.get(position);
                Glide.with(photoHolder.imageView.getContext())
                     .load(imageUrl)
                     .into(photoHolder.imageView);
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

            public PhotoViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.iv_photo);
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

    // Adapter for the memo RecyclerView
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
