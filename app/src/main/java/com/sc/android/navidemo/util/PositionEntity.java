package com.sc.android.navidemo.util;

/**
 * Created by Administrator on 2016/1/22.
 * ClassName:PositionEntity
 * Function: 封装的关于位置的实体
 * Date: 2015年4月3日 上午9:50:28 <
 * @author yiyi.qi
 * @see
 * @since JDK 1.6
 */
public class PositionEntity {
    public double latitude; //纬度

    public double longitude; //经度

    public String address;

    public String district;

    public String city;

    public PositionEntity() {
    }

    public PositionEntity(double latitude, double longitude, String address, String district, String city) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.district = district;
        this.city = city;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
