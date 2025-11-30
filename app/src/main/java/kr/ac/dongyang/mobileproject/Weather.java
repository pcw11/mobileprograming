package kr.ac.dongyang.mobileproject;

public class Weather {
    private long locationId; // DB의 id (PK)
    private int icon;
    private String location;
    private String temperature;

    public Weather(long locationId, int icon, String location, String temperature) {
        this.locationId = locationId;
        this.icon = icon;
        this.location = location;
        this.temperature = temperature;
    }

    // Getters
    public long getLocationId() {
        return locationId;
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

    // Setters
    public void setLocationId(long locationId) {
        this.locationId = locationId;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }
}
