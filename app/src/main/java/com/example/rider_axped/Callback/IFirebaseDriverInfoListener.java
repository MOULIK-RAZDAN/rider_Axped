package com.example.rider_axped.Callback;


import com.example.rider_axped.Common.DriverGeoModel;

public interface IFirebaseDriverInfoListener {
    void onDriverInfoLoadSuccess(DriverGeoModel driverGeoModel);
}
