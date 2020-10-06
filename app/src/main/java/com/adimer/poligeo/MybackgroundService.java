package com.adimer.poligeo;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.greenrobot.eventbus.EventBus;

//import androidx.annotation.Nullable;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;


public class MybackgroundService extends Service {

    private Retrofit retrofit;
    private JsonPlaceHolderApi jsonPlaceHolderApi;

    private static final String CHANNEL_ID="my_channel";
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = "org.greenrobot.eventbus.EventBus"+
            ".started_from_notification";
    private final IBinder mBinder=new LocalBinder();
    private static final long UPDATE_INTERVAL_IN_MIL=5000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MUL=UPDATE_INTERVAL_IN_MIL/2;
    private static final int NOTI_ID=1223;
    private boolean mChangingConfiguration=false;
    private NotificationManager mNotificationManager;

    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Handler mServiceHandler;
    private Location mLocation;



    public String user="";
    public Boolean sonido=true;
    public MybackgroundService(){

    }
    private Socket mSocket;
    {
        try {
//            mSocket = IO.socket("https://deliveryoru.herokuapp.com");
            mSocket = IO.socket("https://policeoru.herokuapp.com/");
        } catch (URISyntaxException e) {}
    }
    @Override
    public void onCreate() {
        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(this);
        locationCallback=new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };
        mSocket.connect();



        createLocationRequest();
        getLastLocation();

        HandlerThread handlerThread=new HandlerThread("EDMTDev");
        handlerThread.start();
        mServiceHandler=new Handler(handlerThread.getLooper());
        mNotificationManager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel mChannel=new NotificationChannel(CHANNEL_ID,
                    getString(R.string.app_name),NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(mChannel);
        }

        retrofit= new Retrofit.Builder()
                .baseUrl( "http://200.110.50.35/api/" )
                .addConverterFactory( GsonConverterFactory.create() )
                .build();
        jsonPlaceHolderApi = retrofit.create(JsonPlaceHolderApi.class);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,false);
        if (startedFromNotification){
            removeLocationUpdates();

            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void removeLocationUpdates() {
        try{
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            Common.setRequesetingLocationupdates(this,false);
            stopSelf();
        }catch (SecurityException ex){
            Common.setRequesetingLocationupdates(this,true);
            Log.e("EDMT_DEV","Lost location permission. Could not remove updates. "+ ex);
        }
    }

    private void getLastLocation() {
        try {
            fusedLocationProviderClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult()!=null)
                                mLocation=task.getResult();
                            else
                                Log.e("EDMT_DEV","Failed to get location");
                        }
                    });
        }catch (SecurityException ex){
            Log.e("EDMT_DEV","Lost location permission"+ex);
        }
    }

    private void createLocationRequest() {
        locationRequest=new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MIL);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MUL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void onNewLocation(Location lastLocation) {
        mLocation=lastLocation;
        EventBus.getDefault().postSticky(new SendLocationToActivity(mLocation));
        //update notification content
        if (ServiceIsRunningInForeGround(this)){
            mNotificationManager.notify(NOTI_ID,getNotification());
        }

    }

    private Notification getNotification() {
        Intent intent=new Intent(this,MybackgroundService.class);
        String text=Common.getLocation(mLocation);
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION,true);
        PendingIntent serPendingIntent=PendingIntent.getService(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent activityPendingIntent=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),0);
        if (sonido){

        }
        NotificationCompat.Builder builder=new NotificationCompat.Builder(this)
                .addAction(R.drawable.ic_launch_black_24dp,"Launch",activityPendingIntent)
                .addAction(R.drawable.ic_cancel_black_24dp,"Remove",activityPendingIntent)
                .setContentText(text)
                .setContentTitle(Common.getLocationTitle(this))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(text)
                .setWhen(System.currentTimeMillis());
        mSocket.emit("chat message", text+"/"+user);

        String[] arrOfStr = text.split("/");
        create(arrOfStr[0],arrOfStr[1],user);

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            builder.setChannelId(CHANNEL_ID);
        }
        return builder.build();
    }

    private void create( String lat, String lng, String nombre) {
        com.adimer.poligeo.Location location = new com.adimer.poligeo.Location(lat, lng, nombre);
//        Map<String, String> fields = new HashMap<>();
//        fields.put("lat", "25");
//        fields.put("lng", "25");
//        fields.put("nombre", "New Title");

        Call<com.adimer.poligeo.Location> call = jsonPlaceHolderApi.createLocation(location);
        call.enqueue(new Callback<com.adimer.poligeo.Location>() {
            @Override
            public void onResponse(Call<com.adimer.poligeo.Location> call, Response<com.adimer.poligeo.Location> response) {
                Toast.makeText(getApplicationContext(), "Bien", Toast.LENGTH_LONG).show();
            }
            @Override
            public void onFailure(Call<com.adimer.poligeo.Location> call, Throwable t) {
                Toast.makeText(getApplicationContext(), t.toString(), Toast.LENGTH_LONG).show();
                Log.e( "LOG TAG", t.toString());

            }
        });
    }

    private boolean ServiceIsRunningInForeGround(Context context) {
        ActivityManager manager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service:manager.getRunningServices(Integer.MAX_VALUE))
            if (service.foreground)
                return true;
        return false;
    }

    public void requestLocationUpdates() {
        Common.setRequesetingLocationupdates(this,true);
        startService(new Intent(getApplicationContext(),MybackgroundService.class));
        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());
        }catch (SecurityException ex){
            Log.e("EDMT_DEV","Lost location permission. Could not request it "+ex);
        }
    }

    class LocalBinder extends Binder {
        MybackgroundService getService(){
            return MybackgroundService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        stopForeground(true);
        mChangingConfiguration=false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        stopForeground(true);
        mChangingConfiguration=false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (!mChangingConfiguration && Common.requestingLocationUpdates(this) )
            startForeground(NOTI_ID,getNotification());
        return true;
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacks(null);
        super.onDestroy();
    }
}
