package kr.ac.dongyang.mobileproject;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;

import kr.ac.dongyang.mobileproject.plant.Plant;
import kr.ac.dongyang.mobileproject.plant.PlantAdapter;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PlantAdapter adapter;
    private ArrayList<Plant> plantList;
    private FloatingActionButton fabAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 초기화
        recyclerView = findViewById(R.id.rv_plant_list);
        fabAdd = findViewById(R.id.fab_add);
        plantList = new ArrayList<>();

        // 2. 더미 데이터(기본 식물들) 추가
        plantList.add(new Plant("아이비", "초록이", "그늘을 좋아해요", 4, true));
        plantList.add(new Plant("선인장", "가시돌이", "물 자주 주지 말것", 17, false)); // 이미지 없음
        plantList.add(new Plant("스투키", "공기청정기", "침실에 두면 좋음", 7, true));
        plantList.add(new Plant("몬스테라", "왕잎", "잎이 갈라질 때까지", 2, true));

        // 3. 어댑터 생성 및 연결
        adapter = new PlantAdapter(plantList);
        recyclerView.setAdapter(adapter);

        // 4. [중요] 레이아웃 매니저 설정 (지그재그 배치)
        // SPAN_COUNT = 2 (두 줄), VERTICAL (세로 스크롤)
        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);

        // 아이템 이동 시 빈 공간 방지
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recyclerView.setLayoutManager(layoutManager);

        // 5. FAB 버튼 클릭 이벤트 (식물 추가)
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewPlant();
            }
        });
    }

    // 식물을 추가하는 함수
    private void addNewPlant() {
        // 실제 앱에서는 여기서 입력창(Dialog)을 띄워야 하지만,
        // 지금은 테스트를 위해 임의의 식물을 바로 추가합니다.

        Plant newPlant = new Plant("새로운 식물", "뉴비", "새로 들어왔어요!", 5, true);

        // 리스트에 데이터 추가
        plantList.add(newPlant);

        // 어댑터에게 "데이터 추가됐으니 화면 갱신해!"라고 알림
        // (전체 갱신인 notifyDataSetChanged()보다 효율적입니다)
        adapter.notifyItemInserted(plantList.size() - 1);

        // 스크롤을 맨 아래(새로 추가된 곳)로 이동
        recyclerView.smoothScrollToPosition(plantList.size() - 1);

        Toast.makeText(this, "새 식물이 추가되었습니다!", Toast.LENGTH_SHORT).show();
    }
}