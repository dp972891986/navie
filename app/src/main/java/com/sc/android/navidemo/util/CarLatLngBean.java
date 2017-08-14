package com.sc.android.navidemo.util;

/**
 * Created by Administrator on 2016/3/24.
 */
public class CarLatLngBean {
    private double Latitude;
    private double Longitude;

    @Override
    public String toString() {
        return "CarLatLngBean{" +
                "Latitude=" + Latitude +
                ", Longitude=" + Longitude +
                '}';
    }

    public double getLatitude() {
        return Latitude;
    }

    public void setLatitude(double latitude) {
        Latitude = latitude;
    }

    public double getLongitude() {
        return Longitude;
    }

    public void setLongitude(double longitude) {
        Longitude = longitude;
    }
}
