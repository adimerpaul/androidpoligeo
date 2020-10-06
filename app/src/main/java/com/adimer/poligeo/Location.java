package com.adimer.poligeo;

public class Location {
    private String lat;
    private String lng;
    private String nombre;

    public Location(String lat, String lng, String nombre) {
        this.lat = lat;
        this.lng = lng;
        this.nombre = nombre;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
