package kr.ac.dongyang.mobileproject.weather;

import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import kr.ac.dongyang.mobileproject.BuildConfig;

public class GeoCoder {

    public static void getCoordinates(String address) {
        new Thread(() -> {
            String apiKey = BuildConfig.GEO_API_KEY; // ""를 제거하고 BuildConfig 변수를 직접 사용합니다.
            String searchType = "road";
            String epsg = "epsg:4326";

            StringBuilder sb = new StringBuilder("https://api.vworld.kr/req/address");
            try {
                sb.append("?service=address");
                sb.append("&request=getCoord");
                sb.append("&format=json");
                sb.append("&crs=" + epsg);
                sb.append("&key=" + apiKey);
                sb.append("&type=" + searchType);
                sb.append("&address=" + URLEncoder.encode(address, StandardCharsets.UTF_8));

                URL url = new URL(sb.toString());
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
                Log.d("GeoCoder", "Response: " + result);

                if (responseCode != 200) {
                    Log.e("GeoCoder", "API Call Failed: " + responseCode);
                    return;
                }

                JSONObject jsob = new JSONObject(result);
                JSONObject jsrs = jsob.getJSONObject("response");
                String status = jsrs.getString("status");

                if (!"OK".equals(status)) {
                    Log.e("GeoCoder", "API Status Error: " + status);
                    return;
                }

                JSONObject jsResult = jsrs.getJSONObject("result");
                JSONObject jspoint = jsResult.getJSONObject("point");

                String x = jspoint.getString("x"); // Longitude
                String y = jspoint.getString("y"); // Latitude

                Log.i("GeoCoder", "Address: " + address + ", Coordinates: [" + x + ", " + y + "]");

            } catch (Exception e) {
                Log.e("GeoCoder", "Exception occurred", e);
            }
        }).start();
    }
}
