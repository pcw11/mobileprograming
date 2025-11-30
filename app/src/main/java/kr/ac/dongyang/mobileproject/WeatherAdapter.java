package kr.ac.dongyang.mobileproject;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet; // 추가
import java.sql.Statement;
import java.util.List;
import java.util.Locale;

import kr.ac.dongyang.mobileproject.weather.AddressSearcher;
import kr.ac.dongyang.mobileproject.weather.WeatherApiService;
import kr.ac.dongyang.mobileproject.weather.WeatherResponse;
import kr.ac.dongyang.mobileproject.weather.WeatherService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherAdapter extends RecyclerView.Adapter<WeatherAdapter.WeatherViewHolder> {

    private List<Weather> weatherList;
    private Context context;
    private String userId; // 현재 사용자 ID

    // MainActivity의 날씨 목록을 새로고침하기 위한 리스너
    public interface WeatherDataReloadListener {
        void reloadWeatherData();
    }
    private WeatherDataReloadListener reloadListener;

    public WeatherAdapter(List<Weather> weatherList, Context context, String userId, WeatherDataReloadListener listener) {
        this.weatherList = weatherList;
        this.context = context;
        this.userId = userId;
        this.reloadListener = listener;
    }

    @NonNull
    @Override
    public WeatherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weather, parent, false);
        return new WeatherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeatherViewHolder holder, int position) {
        Weather weather = weatherList.get(position);
        holder.icon.setImageResource(weather.getIcon());
        holder.location.setText(weather.getLocation());
        holder.temperature.setText(weather.getTemperature());

        holder.ivEditLocation.setOnClickListener(v -> {
            showSearchDialog(weather.getLocationId(), position);
        });
    }

    private void showSearchDialog(long locationId, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_search_address, null);
        builder.setView(view);

        EditText etAddress = view.findViewById(R.id.et_address);
        Button btnSearch = view.findViewById(R.id.btn_search);
        ListView lvResults = view.findViewById(R.id.lv_results);

        AlertDialog dialog = builder.create();

        btnSearch.setOnClickListener(v -> {
            String query = etAddress.getText().toString();
            if (!query.isEmpty()) {
                AddressSearcher.searchAddress(query, new AddressSearcher.OnAddressSearchListener() {
                    @Override
                    public void onResult(List<AddressSearcher.AddressItem> addressList) {
                        if (addressList.isEmpty()) {
                            Toast.makeText(context, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ArrayAdapter<AddressSearcher.AddressItem> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, addressList);
                        lvResults.setAdapter(adapter);
                        lvResults.setOnItemClickListener((parent, view1, pos, id) -> {
                            AddressSearcher.AddressItem selectedAddress = addressList.get(pos);
                            
                            if (locationId == -1) { // "지역 추가" 카드인 경우
                                addNewLocation(selectedAddress.title, selectedAddress.y, selectedAddress.x);
                            } else { // 기존 지역 카드인 경우
                                updateLocation(locationId, selectedAddress.title, selectedAddress.y, selectedAddress.x, position);
                            }

                            dialog.dismiss();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(context, "주소 검색 오류: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }

    private void addNewLocation(String regionName, String y, String x) {
        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(context).getConnection()) {
                String sql = "INSERT INTO saved_locations (user_id, region_name, nx, ny) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, userId);
                    pstmt.setString(2, regionName);
                    pstmt.setDouble(3, Double.parseDouble(x));
                    pstmt.setDouble(4, Double.parseDouble(y));
                    pstmt.executeUpdate();
                    // 메인 액티비티의 날씨 목록 전체를 새로고침하도록 요청
                    ((MainActivity) context).runOnUiThread(() -> reloadListener.reloadWeatherData());
                    Log.i("DB_INSERT", "새 지역 추가 성공: " + regionName);
                }
            } catch (Exception e) {
                Log.e("DB_INSERT", "새 지역 추가 중 오류", e);
            }
        }).start();
    }

    private void updateLocation(long locationId, String newRegionName, String y, String x, int position) {
        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(context).getConnection()) {
                String sql = "UPDATE saved_locations SET region_name = ?, nx = ?, ny = ?, reload_at = CURRENT_TIMESTAMP WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, newRegionName);
                    pstmt.setDouble(2, Double.parseDouble(x));
                    pstmt.setDouble(3, Double.parseDouble(y));
                    pstmt.setLong(4, locationId);
                    pstmt.executeUpdate();
                    
                    // 즉시 UI 업데이트 및 날씨 정보 갱신
                    ((MainActivity) context).runOnUiThread(() -> {
                        Weather weatherItem = weatherList.get(position);
                        weatherItem.setLocation(newRegionName);
                        fetchAndUpdateWeather(position, newRegionName, y, x);
                    });
                    Log.i("DB_UPDATE", "지역 정보 업데이트 성공: " + newRegionName);
                }
            } catch (Exception e) {
                Log.e("DB_UPDATE", "지역 정보 업데이트 중 오류", e);
            }
        }).start();
    }

    private void fetchAndUpdateWeather(int position, String regionName, String latitude, String longitude) {
        WeatherApiService weatherService = WeatherService.getWeatherApiService();
        String apiKey = BuildConfig.WEATHER_API_KEY;

        double lat = Double.parseDouble(latitude);
        double lon = Double.parseDouble(longitude);

        weatherService.getWeatherData(lat, lon, apiKey, "metric", "kr").enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weatherData = response.body();
                    String description = weatherData.getWeather().get(0).getDescription();
                    double temp = weatherData.getMain().getTemp();

                    Weather weatherItem = weatherList.get(position);
                    weatherItem.setTemperature(String.format(Locale.getDefault(), "%.1f°C %s", temp, description));
                    weatherItem.setIcon(getWeatherIcon(description));

                    saveWeatherDataToDatabase(regionName, temp, description);

                    notifyItemChanged(position);
                } else {
                     // Handle error
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                 // Handle failure
            }
        });
    }

    private void saveWeatherDataToDatabase(String regionName, double temperature, String weatherStatus) {
        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(context).getConnection()) {
                long locationId = -1;
                String selectSql = "SELECT id FROM saved_locations WHERE user_id = ? AND region_name = ?";
                try (PreparedStatement selectPstmt = conn.prepareStatement(selectSql)) {
                    selectPstmt.setString(1, userId);
                    selectPstmt.setString(2, regionName);
                    try (ResultSet rs = selectPstmt.executeQuery()) {
                        if (rs.next()) {
                            locationId = rs.getLong("id");
                        }
                    }
                }

                if (locationId == -1) return;

                String insertSql = "INSERT INTO weather_data (location_id, temperature, weather_status) VALUES (?, ?, ?) " +
                                 "ON DUPLICATE KEY UPDATE temperature = VALUES(temperature), weather_status = VALUES(weather_status), updated_at = CURRENT_TIMESTAMP";
                try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                    insertPstmt.setLong(1, locationId);
                    insertPstmt.setDouble(2, temperature);
                    insertPstmt.setString(3, weatherStatus);
                    insertPstmt.executeUpdate();
                }
            } catch (Exception e) {
                Log.e("DB_SAVE_WEATHER", "날씨 정보 저장 중 오류", e);
            }
        }).start();
    }

    private int getWeatherIcon(String description) {
        if (description.contains("맑음")) return R.drawable.sunny;
        if (description.contains("구름") || description.contains("흐림")) return R.drawable.cloud;
        if (description.contains("비")) return R.drawable.rain;
        return R.drawable.sunny; // Default
    }

    @Override
    public int getItemCount() {
        return weatherList.size();
    }

    static class WeatherViewHolder extends RecyclerView.ViewHolder {
        ImageView icon, ivEditLocation;
        TextView location, temperature;

        public WeatherViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.iv_weather_icon);
            location = itemView.findViewById(R.id.tv_location);
            temperature = itemView.findViewById(R.id.tv_weather_temp);
            ivEditLocation = itemView.findViewById(R.id.iv_edit_location);
        }
    }
}
