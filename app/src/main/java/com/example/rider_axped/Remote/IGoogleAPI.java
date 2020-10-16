package com.example.rider_axped.Remote;

import com.google.firebase.database.snapshot.StringNode;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface IGoogleAPI {
    // Enable billing for project to use this api
    @GET("maps/api/directions/json")
    Observable<String> getDirection(
            @Query("mode") String mode,
            @Query("transit_routing_preference") String transit_routing,
            @Query("origin") String from,
            @Query("destination") String to,
            @Query("key") String key
    );


}
