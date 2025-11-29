package kr.ac.dongyang.mobileproject.plant;

import java.util.List;

public class Plant {
    private String name;        // 식물 이름 (예: 아이비)
    private String nickname;    // 별명 (예: 초록이)
    private String imageUrl;    // 이미지 URL
    private List<String> memos; // 메모 목록
    private int waterDDay;      // 물주기 남은 날짜
    private boolean isWatered;   // 물을 주었는지 여부

    // 생성자 (Constructor)
    public Plant(String name, String nickname, String imageUrl, List<String> memos, int waterDDay, boolean isWatered) {
        this.name = name;
        this.nickname = nickname;
        this.imageUrl = imageUrl;
        this.memos = memos;
        this.waterDDay = waterDDay;
        this.isWatered = isWatered;
    }

    // Getter 함수들
    public String getName() { return name; }
    public String getNickname() { return nickname; }
    public String getImageUrl() { return imageUrl; }
    public List<String> getMemos() { return memos; }
    public int getWaterDDay() { return waterDDay; }
    public boolean isWatered() { return isWatered; }

    // 첫번째 메모만 반환하는 편의 메소드
    public String getMemo() {
        if (memos != null && !memos.isEmpty()) {
            return memos.get(0);
        }
        return null;
    }
}
