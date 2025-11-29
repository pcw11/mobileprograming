package kr.ac.dongyang.mobileproject.plant;

import java.util.List;

public class Plant {
    private long plantId;       // 식물 고유 ID
    private String name;        // 식물 이름 (예: 아이비)
    private String nickname;    // 별명 (예: 초록이)
    private String imageUrl;    // 이미지 URL
    private List<String> memos; // 메모 목록
    private String lastWateredDate; // 마지막으로 물 준 날짜
    private int waterCycle;      // 물주기 주기 (n일)

    // 생성자 (Constructor)
    public Plant(long plantId, String name, String nickname, String imageUrl, List<String> memos, String lastWateredDate, int waterCycle) {
        this.plantId = plantId;
        this.name = name;
        this.nickname = nickname;
        this.imageUrl = imageUrl;
        this.memos = memos;
        this.lastWateredDate = lastWateredDate;
        this.waterCycle = waterCycle;
    }

    // Getter 함수들
    public long getPlantId() { return plantId; }
    public String getName() { return name; }
    public String getNickname() { return nickname; }
    public String getImageUrl() { return imageUrl; }
    public List<String> getMemos() { return memos; }
    public String getLastWateredDate() { return lastWateredDate; }
    public int getWaterCycle() { return waterCycle; }

    // Setter
    public void setLastWateredDate(String lastWateredDate) {
        this.lastWateredDate = lastWateredDate;
    }

    // 첫번째 메모만 반환하는 편의 메소드
    public String getMemo() {
        if (memos != null && !memos.isEmpty()) {
            return memos.get(0);
        }
        return null;
    }
}
