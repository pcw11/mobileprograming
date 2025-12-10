# 🌿 동물의 숲 (동양미래 식물의 숲)

**동물의 숲**은 나의 반려 식물을 등록하고 관리하며, 식물 성장에 중요한 날씨 정보를 실시간으로 확인하고 알림을 받을 수 있는 안드로이드 애플리케이션입니다.

## 📱 프로젝트 소개

이 프로젝트는 사용자가 키우는 식물의 물주기 주기를 관리하고, 성장 과정을 기록하며, 외부 날씨 API를 활용해 식물 관리에 필요한 정보를 제공합니다. MySQL 데이터베이스와 직접 연동하여 데이터를 관리하며, Flask 서버를 통해 이미지를 업로드하는 하이브리드 통신 방식을 채택하고 있습니다.

## ✨ 주요 기능 (Key Features)

### 1. 🌼 식물 관리 (Plant Management)
* **식물 등록 및 조회**: 식물의 종류, 별명, 물주기 주기, 입양 날짜 등을 등록하고 `StaggeredGridLayout`을 통해 카드 형태로 모아볼 수 있습니다.
* **물주기 알림 시스템**:
    * 등록된 `last_watered_date`와 `watering_cycle`을 계산하여 물주기가 필요한 날짜를 자동으로 판단합니다.
    * 물주기가 필요한 당일, 메인 화면의 알림 아이콘이 활성화되며 사용자에게 리스트를 제공합니다.
    * `AlarmManager`를 활용하여 매일 설정된 시간에 푸시 알림을 발송합니다.
* **성장 기록**: 식물별로 사진과 메모를 남겨 성장 과정을 타임라인처럼 관리할 수 있습니다. (DB 테이블 `plant_memos`, `plant_images` 연동)

### 2. ⛅ 날씨 정보 및 스마트 알림 (Smart Weather Alerts)
* **실시간 날씨 모니터링**:
    * OpenWeatherMap API를 사용하여 사용자가 저장한 지역의 실시간 날씨(온도, 상태)를 조회합니다.
    * `ViewPager2`를 사용하여 여러 지역의 날씨 정보를 스와이프하여 확인할 수 있습니다.
* **사용자 맞춤형 기상 알림**:
    * `WorkManager`를 이용해 백그라운드에서 주기적으로 날씨를 체크합니다.
    * 사용자가 설정한 조건(비/눈 예보, 특정 최저/최고 온도 도달) 충족 시 알림을 발송하여 식물을 실내로 들이거나 보호할 수 있도록 돕습니다.

### 3. 👤 사용자 시스템 (User System)
* **회원가입 및 로그인**: MySQL 기반의 회원 인증 시스템을 갖추고 있으며, 비밀번호는 해싱 처리되어 안전하게 저장됩니다.
* **계정 관리**: 이메일 변경(인증 포함), 비밀번호 변경, 데이터 초기화 기능을 제공합니다.
* **자동 로그인**: `SharedPreferences`를 활용하여 앱 재실행 시 로그인 상태를 유지합니다.

### 4. 📸 이미지 처리
* **사진 업로드**: `Retrofit2`를 사용하여 Flask 기반의 웹 서버로 식물 이미지를 업로드합니다.
* **이미지 로딩**: `Glide` 라이브러리를 사용하여 이미지를 효율적으로 로딩하고 캐싱합니다.

## 🛠 기술 스택 (Tech Stack)

* **Language**: Java 11
* **SDK**: Min SDK 28 (Android 9.0) / Target SDK 35
* **UI Components**: `RecyclerView`, `ViewPager2`, `DrawerLayout`, `CardView`, `CoordinatorLayout`
* **Networking**:
    * **Retrofit2 & OkHttp3**: 날씨 API 조회 및 이미지 업로드 서버 통신
    * **JDBC (MySQL Connector)**: 원격 MySQL 데이터베이스 직접 연결 및 쿼리 수행
* **Async & Background**:
    * **WorkManager**: 주기적인 날씨 체크 작업 예약
    * **Thread & Handler**: DB 작업 비동기 처리 및 UI 업데이트
* **Image Loader**: Glide 4.12.0

## ⚙️ 환경 설정 (Configuration)

이 프로젝트는 보안을 위해 API 키와 서버 정보를 `local.properties` 파일에서 관리합니다. 앱을 빌드하기 위해서는 프로젝트 루트의 `local.properties` 파일에 다음 항목들을 설정해야 합니다.

```properties
# local.properties

# Android SDK 경로
sdk.dir=...

# Database Connection (MySQL)
db.host=YOUR_DB_HOST
db.name=YOUR_DB_NAME
db.user=YOUR_DB_USER
db.password=YOUR_DB_PASSWORD

# Image Server (Flask)
server.ip=YOUR_SERVER_IP

# External APIs
geo.apikey=YOUR_GEOCODING_API_KEY
weather.apikey=YOUR_OPENWEATHERMAP_API_KEY
