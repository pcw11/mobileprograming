package kr.ac.dongyang.mobileproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WeatherAdapter extends RecyclerView.Adapter<WeatherAdapter.WeatherViewHolder> {

    private List<Weather> weatherList;

    public WeatherAdapter(List<Weather> weatherList) {
        this.weatherList = weatherList;
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
    }

    @Override
    public int getItemCount() {
        return weatherList.size();
    }

    static class WeatherViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView location;
        TextView temperature;

        public WeatherViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.iv_weather_icon);
            location = itemView.findViewById(R.id.tv_location);
            temperature = itemView.findViewById(R.id.tv_weather_temp);
        }
    }
}
