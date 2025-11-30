package kr.ac.dongyang.mobileproject.weather;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import kr.ac.dongyang.mobileproject.BuildConfig;

public class AddressSearcher {

    // 주소, 경도, 위도를 담을 데이터 클래스
    public static class AddressItem {
        public final String title;
        public final String x; // 경도
        public final String y; // 위도

        public AddressItem(String title, String x, String y) {
            this.title = title;
            this.x = x;
            this.y = y;
        }

        // ArrayAdapter가 리스트에 주소 이름을 표시하도록 하기 위함
        @NonNull
        @Override
        public String toString() {
            return title;
        }
    }

    public interface OnAddressSearchListener {
        void onResult(List<AddressItem> addressList);
        void onError(String message);
    }

    public static void searchAddress(String query, OnAddressSearchListener listener) {
        new Thread(() -> {
            List<AddressItem> addressList = new ArrayList<>();
            try {
                String apiKey = BuildConfig.GEO_API_KEY;
                String urlString = new StringBuilder("https://api.vworld.kr/req/search")
                        .append("?service=search")
                        .append("&request=search")
                        .append("&version=2.0")
                        .append("&crs=EPSG:4326")
                        .append("&size=10")
                        .append("&page=1")
                        .append("&query=").append(URLEncoder.encode(query, StandardCharsets.UTF_8))
                        .append("&type=district")      // 행정구역 검색으로 수정
                        .append("&category=L4")       // 행정동 레벨로 수정
                        .append("&format=json")
                        .append("&key=").append(apiKey)
                        .toString();

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                BufferedReader reader;
                if (responseCode == 200) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                }

                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                reader.close();
                conn.disconnect();

                String result = responseBuilder.toString();
                Log.d("AddressSearcher", "Response: " + result);

                if (responseCode != 200) {
                    new Handler(Looper.getMainLooper()).post(() -> listener.onError("API Call Failed: " + responseCode));
                    return;
                }

                JSONObject jsonResponse = new JSONObject(result);
                JSONObject responseObj = jsonResponse.getJSONObject("response");
                String status = responseObj.getString("status");

                if ("OK".equals(status)) {
                    JSONObject resultObj = responseObj.getJSONObject("result");
                    JSONArray items = resultObj.getJSONArray("items");

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        String title = item.getString("title");
                        JSONObject point = item.getJSONObject("point");
                        String x = point.getString("x");
                        String y = point.getString("y");
                        addressList.add(new AddressItem(title, x, y));
                    }
                    new Handler(Looper.getMainLooper()).post(() -> listener.onResult(addressList));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> listener.onError("API Status Error: " + status));
                }

            } catch (Exception e) {
                Log.e("AddressSearcher", "Exception occurred", e);
                new Handler(Looper.getMainLooper()).post(() -> listener.onError(e.getMessage()));
            }
        }).start();
    }
}
