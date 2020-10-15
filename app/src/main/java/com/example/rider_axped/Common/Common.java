package com.example.rider_axped.Common;

import com.example.rider_axped.Model.RiderModel;

public class Common {
    public static final String RIDER_INFO_REFENCE = "Riders";
    public static RiderModel currentRider;

    public static CharSequence buildWelcomeMessage() {   // string not working so CharSequence
        if(Common.currentRider != null)    // user == rider
        {
            return new StringBuilder("Welcome ")
                    .append(Common.currentRider.getFirstName())
                    .append(" ")
                    .append(Common.currentRider.getLastName());
        }
        else return "";
    }
}
