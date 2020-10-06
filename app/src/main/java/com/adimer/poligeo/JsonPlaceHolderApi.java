package com.adimer.poligeo;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface JsonPlaceHolderApi {
    @POST("location")
    Call<Location> createLocation(@Body Location location);
}
