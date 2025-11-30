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

    public WeatherAdapter(List<Weather> weatherList, Context context) {
        this.weatherList = weatherList;
        this.context = context;
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
            showSearchDialog(position);
        });
    }

    private void showSearchDialog(int position) {
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
                            
                            // 1. Update location text immediately
                            weatherList.get(position).setLocation(selectedAddress.title);

                            // 2. Fetch new weather data using coordinates
                            fetchAndUpdateWeather(position, selectedAddress.y, selectedAddress.x); // y가 위도(lat), x가 경도(lon)

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

    private void fetchAndUpdateWeather(int position, String latitude, String longitude) {
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

                    notifyItemChanged(position);
                    Toast.makeText(context, "날씨 정보가 업데이트되었습니다.", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(context, "날씨 정보를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Toast.makeText(context, "날씨 API 호출 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getWeatherIcon(String description) {
        if (description.contains("맑음")) {
            return R.drawable.sunny;
        } else if (description.contains("구름") || description.contains("흐림")) {
            return R.drawable.cloud;
        } else if (description.contains("비")) {
            return R.drawable.rain;
        } else {
            return R.drawable.sunny; // Default icon
        }
    }

    @Override
    public int getItemCount() {
        return weatherList.size();
    }

    static class WeatherViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView location;
        TextView temperature;
        ImageView ivEditLocation;

        public WeatherViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.iv_weather_icon);
            location = itemView.findViewById(R.id.tv_location);
            temperature = itemView.findViewById(R.id.tv_weather_temp);
            ivEditLocation = itemView.findViewById(R.id.iv_edit_location);
        }
    }
}
