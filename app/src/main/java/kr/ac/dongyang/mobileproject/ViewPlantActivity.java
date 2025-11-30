package kr.ac.dongyang.mobileproject;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ViewPlantActivity extends AppCompatActivity {

    private EditText etPlantSpecies, etPlantNickname;
    private ImageView ivWaterEdit, ivMemoAdd, ivSearchIcon, ivMemoEdit;
    private TextView tvGreeting, tvWaterSubtitle;
    private Button btnSave;
    private RecyclerView rvMemos;
    private MemoAdapter memoAdapter;
    private List<String> memoList = new ArrayList<>();

    private int wateringCycle = 7;
    private long plantId;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_plant);

        etPlantSpecies = findViewById(R.id.et_plant_species);
        etPlantNickname = findViewById(R.id.et_plant_nickname);
        ivWaterEdit = findViewById(R.id.iv_water_edit);
        tvWaterSubtitle = findViewById(R.id.tv_water_subtitle);
        ivMemoAdd = findViewById(R.id.iv_memo_add);
        ivMemoEdit = findViewById(R.id.iv_memo_edit);
        rvMemos = findViewById(R.id.rv_memos);
        btnSave = findViewById(R.id.btn_save_plant);
        ivSearchIcon = findViewById(R.id.iv_search_icon);
        tvGreeting = findViewById(R.id.tv_greeting_add);

        Intent intent = getIntent();
        plantId = intent.getLongExtra("PLANT_ID", -1);

        if (plantId == -1) {
            Toast.makeText(this, "식물 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupRecyclerView();
        loadPlantDetails();

        ivWaterEdit.setOnClickListener(v -> showWateringCycleDialog());
        ivMemoAdd.setOnClickListener(v -> {
            memoList.add("");
            memoAdapter.notifyItemInserted(memoList.size() - 1);
        });
        ivMemoEdit.setOnClickListener(v -> toggleMemoEditMode());
        btnSave.setOnClickListener(v -> updatePlantData());
        ivSearchIcon.setOnClickListener(v -> {
            String species = etPlantSpecies.getText().toString();
            if (!species.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + species));
                startActivity(browserIntent);
            }
        });
    }

    private void setupRecyclerView() {
        rvMemos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        memoAdapter = new MemoAdapter(memoList);
        rvMemos.setAdapter(memoAdapter);
    }

    private void loadPlantDetails() {
        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(this).getConnection()) {
                String sql = "SELECT p.species, p.nickname, p.watering_cycle, p.user_id, GROUP_CONCAT(pm.content SEPARATOR '\n') as memos " +
                             "FROM plants p LEFT JOIN plant_memos pm ON p.plant_id = pm.plant_id " +
                             "WHERE p.plant_id = ? GROUP BY p.plant_id";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setLong(1, plantId);
                    ResultSet rs = pstmt.executeQuery();

                    if (rs.next()) {
                        String species = rs.getString("species");
                        String nickname = rs.getString("nickname");
                        String userId = rs.getString("user_id");
                        wateringCycle = rs.getInt("watering_cycle");
                        String memosConcat = rs.getString("memos");

                        new Handler(Looper.getMainLooper()).post(() -> {
                            tvGreeting.setText(userId + "님 안녕하세요!");
                            etPlantSpecies.setText(species);
                            etPlantNickname.setText(nickname);
                            tvWaterSubtitle.setText("물 주기는 " + wateringCycle + "일 입니다.");

                            memoList.clear();
                            if (memosConcat != null && !memosConcat.isEmpty()) {
                                memoList.addAll(Arrays.asList(memosConcat.split("\n")));
                            }
                            if (memoList.isEmpty()) {
                                memoList.add(""); // Add an empty memo if there are none
                            }
                            memoAdapter.notifyDataSetChanged();
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("DB_ERROR", "식물 상세 정보 로딩 중 오류 발생", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(this, "상세 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
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
            Toast.makeText(this, "물 주기가 " + wateringCycle + "일로 설정되었습니다.", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("취소", null);

        builder.create().show();
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

    // Adapter for the memo RecyclerView
    class MemoAdapter extends RecyclerView.Adapter<MemoAdapter.MemoViewHolder> {

        private List<String> memos;
        private boolean isEditMode = false;

        public MemoAdapter(List<String> memos) {
            this.memos = memos;
        }

        @NonNull
        @Override
        public MemoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_memo, parent, false);
            return new MemoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MemoViewHolder holder, int position) {
            String memoText = memos.get(position);
            holder.etMemoContent.setText(memoText);
            holder.ivDeleteMemo.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

            holder.ivDeleteMemo.setOnClickListener(v -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    memos.remove(currentPosition);
                    notifyItemRemoved(currentPosition);
                }
            });
        }

        @Override
        public int getItemCount() {
            return memos.size();
        }

        public void setEditMode(boolean editMode) {
            this.isEditMode = editMode;
            notifyDataSetChanged(); // Redraw all items to show/hide delete button
        }

        public List<String> getMemos() {
             List<String> currentMemos = new ArrayList<>();
             for (int i = 0; i < getItemCount(); i++) {
                MemoViewHolder holder = (MemoViewHolder) rvMemos.findViewHolderForAdapterPosition(i);
                if (holder != null) {
                    String memoText = holder.etMemoContent.getText().toString();
                     if (!memoText.trim().isEmpty()) {
                        currentMemos.add(memoText);
                    }
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
    }
}
