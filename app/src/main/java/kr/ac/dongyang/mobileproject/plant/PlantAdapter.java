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

        // 필수 데이터 설정
        holder.tvName.setText(plant.getName());
        holder.tvDDay.setText("물 주기까지 앞으로 " + plant.getWaterDDay() + "일 남았어요!");
        
        // 프로필 이미지 (임시로 기본 이미지 설정)
        // TODO: 나중에 실제 식물 이미지를 불러오는 로직 추가 필요 (Glide, Picasso 등)
        holder.ivProfile.setImageResource(R.drawable.plant_sample); 

        // 별명 데이터 확인 및 뷰 가시성 설정
        if (!TextUtils.isEmpty(plant.getNickname())) {
            holder.tvNickname.setVisibility(View.VISIBLE);
            holder.tvNickname.setText(plant.getNickname());
        } else {
            holder.tvNickname.setVisibility(View.GONE);
        }

        // 메모 데이터 확인 및 뷰 가시성 설정
        if (!TextUtils.isEmpty(plant.getMemo())) {
            holder.llMemoContainer.setVisibility(View.VISIBLE);
            holder.tvMemo.setText(plant.getMemo());
        } else {
            holder.llMemoContainer.setVisibility(View.GONE);
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
