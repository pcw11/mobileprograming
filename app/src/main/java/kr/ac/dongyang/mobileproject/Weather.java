package kr.ac.dongyang.mobileproject;

public class Weather {
    int icon;
    String location;
    String temperature;

    public Weather(int icon, String location, String temperature) {
        this.icon = icon;
        this.location = location;
        this.temperature = temperature;
    }

    public int getIcon() {
        return icon;
    }

    public String getLocation() {
        return location;
    }

    public String getTemperature() {
        return temperature;
    }
}
