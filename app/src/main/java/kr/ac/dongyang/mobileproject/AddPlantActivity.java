package kr.ac.dongyang.mobileproject;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class AddPlantActivity extends AppCompatActivity {

    private TextView tvGreeting;
    private EditText etPlantSpecies, etPlantNickname;
    private ImageView ivWaterEdit, ivMemoAdd, ivMemoEdit;
    private TextView tvWaterSubtitle;
    private Button btnSave;
    private RecyclerView rvMemos;
    private LinearLayout llWaterDates;
    private MemoAdapter memoAdapter;
    private List<String> memoList = new ArrayList<>();

    private int wateringCycle = 7; // 기본 물 주기 7일
    private String userId;
    private boolean isEditMode = false;
    private Date lastWateredDate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plant);

        // 뷰 초기화
        tvGreeting = findViewById(R.id.tv_greeting_add);
        etPlantSpecies = findViewById(R.id.et_plant_species);
        etPlantNickname = findViewById(R.id.et_plant_nickname);
        ivWaterEdit = findViewById(R.id.iv_water_edit);
        tvWaterSubtitle = findViewById(R.id.tv_water_subtitle);
        ivMemoAdd = findViewById(R.id.iv_memo_add);
        ivMemoEdit = findViewById(R.id.iv_memo_edit);
        rvMemos = findViewById(R.id.rv_memos);
        llWaterDates = findViewById(R.id.ll_water_dates);
        btnSave = findViewById(R.id.btn_save);

        // MainActivity로부터 사용자 ID를 받아 환영 메시지 설정
        Intent intent = getIntent();
        userId = intent.getStringExtra("USER_ID");
        tvGreeting.setText("식물 추가");

        lastWateredDate = new Date(System.currentTimeMillis()); // 오늘을 마지막 물 준 날짜로 초기화

        setupRecyclerViews();
        updateDateViews();

        // 물 주기 수정 아이콘 클릭 리스너
        ivWaterEdit.setOnClickListener(v -> showWateringCycleDialog());

        // 메모 추가 버튼은 이제 RecyclerView 내부에 있음
        ivMemoAdd.setVisibility(View.GONE);
        // 메모 수정 버튼 클릭 리스너
        ivMemoEdit.setOnClickListener(v -> toggleMemoEditMode());

        // 저장 버튼 클릭 리스너
        btnSave.setOnClickListener(v -> savePlantData());

        // 초기 물 주기 텍스트 설정
        tvWaterSubtitle.setText("물 주기는 " + wateringCycle + "일 입니다.");
    }

    private void setupRecyclerViews() {
        rvMemos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        memoAdapter = new MemoAdapter(memoList);
        rvMemos.setAdapter(memoAdapter);
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

        Calendar nextWateringCal = null;
        if (lastWateredDate != null) {
            nextWateringCal = Calendar.getInstance();
            nextWateringCal.setTime(lastWateredDate);
            nextWateringCal.add(Calendar.DAY_OF_MONTH, wateringCycle);
        }

        calendar.add(Calendar.DAY_OF_MONTH, -3); // 오늘을 기준으로 3일 전부터 표시

        for (int i = 0; i < 8; i++) { // 8일간의 날짜를 표시
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

    private void savePlantData() {
        String species = etPlantSpecies.getText().toString().trim();
        String nickname = etPlantNickname.getText().toString().trim();

        if (species.isEmpty()) {
            Toast.makeText(this, "식물 종을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> memos = memoAdapter.getMemos();

        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(this).getConnection()) {
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

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        });
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
