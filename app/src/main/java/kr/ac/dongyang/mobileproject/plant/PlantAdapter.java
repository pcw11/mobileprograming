package kr.ac.dongyang.mobileproject.plant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

import kr.ac.dongyang.mobileproject.R;

public class PlantAdapter extends RecyclerView.Adapter<PlantAdapter.PlantViewHolder> {

    private ArrayList<Plant> plantList;

    // 생성자: 데이터를 받음
    public PlantAdapter(ArrayList<Plant> plantList) {
        this.plantList = plantList;
    }

    // 1. 뷰 홀더 생성: item_plant_card.xml을 가져옴
    @NonNull
    @Override
    public PlantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_plant_card, parent, false);
        return new PlantViewHolder(view);
    }

    // 2. 데이터 바인딩: 가져온 뷰에 실제 데이터를 넣음
    @Override
    public void onBindViewHolder(@NonNull PlantViewHolder holder, int position) {
        Plant plant = plantList.get(position);

        holder.tvName.setText(plant.getName());
        holder.tvNickname.setText(plant.getNickname());
        holder.tvMemo.setText(plant.getMemo());
        holder.tvDDay.setText("물 주기까지 앞으로 " + plant.getWaterDDay() + "일 남았어요!");

        // 이미지가 있는 식물과 없는 식물을 구분 (지그재그 레이아웃 효과 확인용)
        if (plant.isHasImage()) {
            holder.ivProfile.setVisibility(View.VISIBLE);
        } else {
            holder.ivProfile.setVisibility(View.GONE);
        }
    }

    // 데이터 개수 반환
    @Override
    public int getItemCount() {
        return plantList.size();
    }

    // 내부 클래스: 뷰의 아이디들을 찾아서 잡고 있는 홀더
    public static class PlantViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvName, tvNickname, tvMemo, tvDDay;

        public PlantViewHolder(@NonNull View itemView) {
            super(itemView);
            // item_plant_card.xml에 있는 ID들과 연결
            ivProfile = itemView.findViewById(R.id.iv_plant_profile);
            tvName = itemView.findViewById(R.id.tv_plant_name);
            tvNickname = itemView.findViewById(R.id.tv_plant_nickname);
            tvMemo = itemView.findViewById(R.id.tv_memo_content);
            tvDDay = itemView.findViewById(R.id.tv_water_dday);
        }
    }
}