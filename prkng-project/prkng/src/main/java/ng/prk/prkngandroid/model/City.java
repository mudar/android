package ng.prk.prkngandroid.model;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.List;

import ng.prk.prkngandroid.Const;
import ng.prk.prkngandroid.util.MapUtils;

public class City implements Comparable<City> {
    private int id;
    private String name;
    @SerializedName(Const.ApiArgs.DISPLAY_NAME)
    private String areaName;
    @SerializedName(Const.ApiArgs.GEO_LAT)
    private double latitude;
    @SerializedName(Const.ApiArgs.GEO_LNG)
    private double longitude;
    @SerializedName(Const.ApiArgs.URBAN_AREA_RADIUS)
    private int areaRadius; // km
    private String country;
    @SerializedName(Const.ApiArgs.COUNTRY_CODE)
    private String countryCode;
    private String region;
    @SerializedName(Const.ApiArgs.REGION_CODE)
    private String regionCode;
    private double distanceTo;
    private List<String> carshare;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAreaName() {
        return areaName;
    }

    public LatLng getLatLng() {
        return new LatLng(latitude, longitude);
    }

    public double getAreaRadius() {
        return areaRadius * MapUtils.KILOMETER_IN_METERS;
    }

    public String getCountry() {
        return country;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getRegion() {
        return region;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public double getDistanceTo() {
        return distanceTo;
    }

    public List<String> getCarshare() {
        return carshare;
    }

    public void setDistanceTo(double distanceTo) {
        this.distanceTo = distanceTo;
    }

    public boolean containsInRadius(LatLng point) {
        return Double.compare(point.distanceTo(getLatLng()), getAreaRadius()) <= 0;
    }

    @Override
    public String toString() {
        return "City{" +
                "distanceTo=" + distanceTo +
                ", id=" + id +
                ", name='" + name + '\'' +
                ", areaName='" + areaName + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", areaRadius=" + areaRadius +
                '}';
    }

    @Override
    public int compareTo(@NonNull City another) {
        return Double.compare(this.distanceTo, another.distanceTo);
    }
}
