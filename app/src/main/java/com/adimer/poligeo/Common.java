package com.adimer.poligeo;

import android.content.Context;
import android.location.Location;
import android.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.Date;
import java.util.prefs.PreferenceChangeEvent;

public class Common {
    public static final String KET_REQUESTING_LOCATION_UPDATES = "LocationUpdateEnable";

    public static String getLocation(Location mLocation) {
        return mLocation==null?"Unknown Location":new StringBuilder()
                .append(mLocation.getLatitude())
                .append("/")
                .append(mLocation.getLatitude())
                .toString();
    }

    public static CharSequence getLocationTitle(MybackgroundService mybackgroundService) {
        return String.format("Location Update %1$s", DateFormat.getDateInstance().format(new Date()));
    }

    public static void setRequesetingLocationupdates(Context context, boolean value) {
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KET_REQUESTING_LOCATION_UPDATES,value)
                .apply();
    }

    public static boolean requestingLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KET_REQUESTING_LOCATION_UPDATES,false);
    }
}
