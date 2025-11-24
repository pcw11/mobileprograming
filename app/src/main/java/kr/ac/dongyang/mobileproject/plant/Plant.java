package kr.ac.dongyang.mobileproject.plant;
//package com.example.myplantapp; // 패키지명은 본인 프로젝트에 맞게 수정하세요

public class Plant {
    private String name;        // 식물 이름 (예: 아이비)
    private String nickname;    // 별명 (예: 초록이)
    private String memo;        // 메모 내용
    private int waterDDay;      // 물주기 남은 날짜
    private boolean hasImage;   // 이미지가 있는지 여부 (높이 변화 테스트용)

    // 생성자 (Constructor)
    public Plant(String name, String nickname, String memo, int waterDDay, boolean hasImage) {
        this.name = name;
        this.nickname = nickname;
        this.memo = memo;
        this.waterDDay = waterDDay;
        this.hasImage = hasImage;
    }

    // Getter 함수들
    public String getName() { return name; }
    public String getNickname() { return nickname; }
    public String getMemo() { return memo; }
    public int getWaterDDay() { return waterDDay; }
    public boolean isHasImage() { return hasImage; }
}