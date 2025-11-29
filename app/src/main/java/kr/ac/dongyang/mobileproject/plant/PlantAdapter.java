package kr.ac.dongyang.mobileproject.plant;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import kr.ac.dongyang.mobileproject.R;

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
            // Glide 라이브러리를 사용하여 이미지 로드 (app/build.gradle.kts 에 추가 필요)
            Glide.with(holder.itemView.getContext())
                    .load(plant.getImageUrl())
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setVisibility(View.GONE);
        }

        // 메모 유무에 따라 LinearLayout 가시성 조절 (첫 번째 메모만 표시)
        // TODO: 여러 메모를 보여주는 방식은 추후 기획에 따라 변경될 수 있습니다.
        if (plant.getMemo() != null && !plant.getMemo().isEmpty()) {
            holder.llMemoContainer.setVisibility(View.VISIBLE);
            holder.tvMemo.setText(plant.getMemo());
        } else {
            holder.llMemoContainer.setVisibility(View.GONE);
        }

        // 물 주기 D-day 설정 (isWatered 값에 따라 분기 처리)
        if(plant.isWatered()){
            holder.tvDDay.setText("물주기 D-" + plant.getWaterDDay());
        } else {
            holder.tvDDay.setText("물주기 D+" + plant.getWaterDDay());
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

        public PlantViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_plant_profile);
            tvName = itemView.findViewById(R.id.tv_plant_name);
            tvNickname = itemView.findViewById(R.id.tv_plant_nickname);
            llMemoContainer = itemView.findViewById(R.id.ll_memo_container);
            tvMemo = itemView.findViewById(R.id.tv_memo_content);
            tvDDay = itemView.findViewById(R.id.tv_water_dday);
        }
    }
}
