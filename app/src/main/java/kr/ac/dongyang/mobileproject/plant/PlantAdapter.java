package kr.ac.dongyang.mobileproject.plant;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import kr.ac.dongyang.mobileproject.DatabaseConnector;
import kr.ac.dongyang.mobileproject.MainActivity;
import kr.ac.dongyang.mobileproject.R;
import kr.ac.dongyang.mobileproject.ViewPlantActivity;

public class PlantAdapter extends RecyclerView.Adapter<PlantAdapter.PlantViewHolder> {

    private ArrayList<Plant> plantList;

    public PlantAdapter(ArrayList<Plant> plantList) {
        this.plantList = plantList;
    }

    @NonNull
    @Override
    public PlantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_plant_card, parent, false);
        return new PlantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlantViewHolder holder, int position) {
        Plant plant = plantList.get(position);

        // 식물 이름 설정
        holder.tvName.setText(plant.getName());

        // 별명 유무에 따라 TextView 가시성 조절
        if (plant.getNickname() != null && !plant.getNickname().isEmpty()) {
            holder.tvNickname.setVisibility(View.VISIBLE);
            holder.tvNickname.setText(plant.getNickname());
        } else {
            holder.tvNickname.setVisibility(View.GONE);
        }

        // 사진 URL 유무에 따라 ImageView 가시성 조절
        if (plant.getImageUrl() != null && !plant.getImageUrl().isEmpty()) {
            holder.ivProfile.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(plant.getImageUrl())
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setVisibility(View.GONE);
        }

        // 메모 유무에 따라 LinearLayout 가시성 조절
        if (plant.getMemo() != null && !plant.getMemo().isEmpty()) {
            holder.llMemoContainer.setVisibility(View.VISIBLE);
            holder.tvMemo.setText(plant.getMemo());
        } else {
            holder.llMemoContainer.setVisibility(View.GONE);
        }

        // 물주기 D-Day 계산 및 표시
        calculateDDay(holder, plant);

        // 아이템 클릭 리스너
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, ViewPlantActivity.class);
            intent.putExtra("PLANT_ID", plant.getPlantId());
            ((MainActivity) context).startActivityForResult(intent, MainActivity.VIEW_PLANT_REQUEST);
        });
    }

    private void calculateDDay(PlantViewHolder holder, Plant plant) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate lastWateredDate = LocalDate.parse(plant.getLastWateredDate(), formatter);
        LocalDate today = LocalDate.now();

        LocalDate nextWateringDate = lastWateredDate.plusDays(plant.getWaterCycle());
        long dDay = ChronoUnit.DAYS.between(today, nextWateringDate);

        holder.cbWatered.setOnCheckedChangeListener(null); // 리스너 초기화
        holder.cbWatered.setChecked(false);

        if (dDay > 0) { // 물 줄 날짜가 남았을 때
            holder.tvDDay.setText("D-" + dDay);
            holder.tvDDay.setTextColor(Color.BLACK);
            holder.tvDDay.setTypeface(null, Typeface.NORMAL);
            holder.cbWatered.setVisibility(View.GONE);
        } else if (dDay == 0) { // 오늘이 물 주는 날일 때
            holder.tvDDay.setText("D-Day");
            holder.tvDDay.setTextColor(Color.BLUE);
            holder.tvDDay.setTypeface(null, Typeface.BOLD);
            holder.cbWatered.setVisibility(View.VISIBLE);
        } else { // 물 줄 날짜가 지났을 때
            holder.tvDDay.setText("물 주기 " + Math.abs(dDay) + "일 경과!");
            holder.tvDDay.setTextColor(Color.RED);
            holder.tvDDay.setTypeface(null, Typeface.BOLD);
            holder.cbWatered.setVisibility(View.VISIBLE);
        }

        // 체크박스 리스너 설정
        if (dDay <= 0) {
            holder.cbWatered.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    String newLastWateredDate = LocalDate.now().format(formatter);
                    DatabaseConnector dbConnector = new DatabaseConnector(holder.itemView.getContext());
                    dbConnector.updateLastWateredDate(plant.getPlantId(), newLastWateredDate, success -> {
                        if (success) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                plant.setLastWateredDate(newLastWateredDate);
                                notifyItemChanged(holder.getAdapterPosition());
                            });
                        } else {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    Toast.makeText(holder.itemView.getContext(), "물 주기 정보 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show());
                        }
                    });
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return plantList.size();
    }

    public static class PlantViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvName, tvNickname, tvMemo, tvDDay;
        LinearLayout llMemoContainer;
        CheckBox cbWatered;

        public PlantViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_plant_profile);
            tvName = itemView.findViewById(R.id.tv_plant_name);
            tvNickname = itemView.findViewById(R.id.tv_plant_nickname);
            llMemoContainer = itemView.findViewById(R.id.ll_memo_container);
            tvMemo = itemView.findViewById(R.id.tv_memo_content);
            tvDDay = itemView.findViewById(R.id.tv_water_dday);
            cbWatered = itemView.findViewById(R.id.cb_watered);
        }
    }
}
