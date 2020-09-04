package com.adimer.poligeo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class MybackgroundService extends Service {
    private static final String CHANNEL_ID="my_channel";
    private final IBinder mBinder=new LocalBinder();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
