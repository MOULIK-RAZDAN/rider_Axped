package com.example.rider_axped.Common;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

//import com.example.rider_axped.Model.AnimationModel;
import com.example.rider_axped.Model.DriverGeoModel;
import com.example.rider_axped.Model.RiderModel;
import com.example.rider_axped.R;
import com.google.android.gms.maps.model.Marker;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Common {
    public static final String RIDER_INFO_REFERENCE = "Riders";
    public static final String TOKEN_REFERENCE ="Token" ;
    public static final String NOTI_TITLE = "title";
    public static final String NOTI_CONTENT ="body" ;
    public static final String DRIVER_LOCATION_REFERENCES = "LenderLocation";
    public static final String DRIVER_INFO_REFERENCE = "DriverInfo";

    public static RiderModel currentRider;
    public static Set<DriverGeoModel> driversFound= new HashSet<DriverGeoModel>();
    public static HashMap<String, Marker> markerList= new HashMap<>();
  //  public static HashMap<String, AnimationModel> driverLocationSubscribe = new HashMap<String, AnimationModel>();

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

    public static void setWelcomeMessage(TextView txt_welcome) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if(hour >= 1 && hour <= 12)
            txt_welcome.setText(new StringBuilder("Good Morning."));
        else if(hour >= 13 && hour <= 17)
            txt_welcome.setText(new StringBuilder("Good Afternoon."));
        else txt_welcome.setText(new StringBuilder("Good Evening."));
    }

    public static String buildName(String firstName, String lastName) {
        return  new StringBuilder(firstName).append(" ").append(lastName).toString();
    }

    public static void showNotification(Context context, int id, String title, String body, Intent intent) {
        PendingIntent pendingIntent=null;
        if(intent!=null)
            pendingIntent=PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        String NOTIFICATION_CHANNEL_ID="CyRent";
        NotificationManager notificationManager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
        {
            NotificationChannel notificationChannel=new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "CyRent",NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("CyRent");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0,1000,500,1000});
            notificationChannel.enableVibration(true);

            notificationManager.createNotificationChannel(notificationChannel);

        }
        NotificationCompat.Builder builder=new NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID);
        builder.setContentText(title)
                .setContentText(body)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.drawable.ic_baseline_electric_bike_24)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.drawable.ic_baseline_electric_bike_24));
        if(pendingIntent !=null)
        {
            builder.setContentIntent(pendingIntent);
        }
        Notification notification= builder.build();
        notificationManager.notify(id,notification);


    }

   /* public static String buildName(String firstName, String lastName) {
        return  new StringBuilder(firstName).append(" ").append(lastName).toString();
    }
*/
  /*  //DECODE POLY
    public static List<LatLng> decodePoly(String encoded) {
        List poly = new ArrayList();
        int index=0,len=encoded.length();
        int lat=0,lng=0;
        while(index < len)
        {
            int b,shift=0,result=0;
            do{
                b=encoded.charAt(index++)-63;
                result |= (b & 0x1f) << shift;
                shift+=5;

            }while(b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1):(result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do{
                b = encoded.charAt(index++)-63;
                result |= (b & 0x1f) << shift;
                shift +=5;
            }while(b >= 0x20);
            int dlng = ((result & 1)!=0 ? ~(result >> 1): (result >> 1));
            lng +=dlng;

            LatLng p = new LatLng((((double)lat / 1E5)),
                    (((double)lng/1E5)));
            poly.add(p);
        }
        return poly;
    }


    public static float getBearing(LatLng begin, LatLng end) {

        double lat = Math.abs(begin.latitude - end.latitude);
        double lng = Math.abs(begin.longitude - end.longitude);

        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
        return -1;
    }*/
}
