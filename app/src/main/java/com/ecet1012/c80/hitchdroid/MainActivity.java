package com.ecet1012.c80.hitchdroid;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.os.SystemClock;


import com.ecet1012.c80.hitchdroid.Workers.AccelerometerWorker;
import com.ecet1012.c80.hitchdroid.Workers.ProximityWorker;
import com.ecet1012.c80.hitchdroid.services.ActiveNotification;
import com.ecet1012.c80.hitchdroid.services.TaskManager;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    public static final String TAG_WAKE = "HitchDroid_WakeLock";


    public static final String STATE_SLEEP = "HitchDroid_Sleep";                //Need to conserve as much battery as possible, really really don't want be to be here. < 50% battery.
    public static final String STATE_LOW_BATTERY = "HitchDroid_LowBattery";     //if battery is not 100% we start cutting back on how often we do things.
    public static final String STATE_NOMINAL = "HitchDroid_Nominal";            //everything is nice and peachy here.
    public static final String STATE_INTERACTING = "HitchDroid_Interacting";    //screen on and interacting with a user.

    public static String activeState;

    public static long lastActionTime = 0;


    private DevicePolicyManager devicePolicyManager;


    public static PowerState powerState;
    private static CrashHandler crashHandler;

    public static Context mainContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        mainContext = this.getApplicationContext();
        Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 0); //as soon as system will allow
        setContentView(R.layout.activity_main_screen);

        powerState = new PowerState();

        crashHandler = new CrashHandler();
        //Thread.setDefaultUncaughtExceptionHandler(crashHandler);


        if (MainActivity.lastActionTime == 0)
            MainActivity.lastActionTime = SystemClock.elapsedRealtime();

        if (devicePolicyManager == null) {
            devicePolicyManager = (DevicePolicyManager) this.getSystemService(Activity.DEVICE_POLICY_SERVICE);
            IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
            filter.addCategory(Intent.CATEGORY_HOME);
            filter.addCategory(Intent.CATEGORY_DEFAULT);
            devicePolicyManager.addPersistentPreferredActivity(DeviceOwnerReceiver.getComponentName(this), filter, DeviceOwnerReceiver.getComponentName(this));
            String[] packages = {this.getPackageName()};
            devicePolicyManager.setLockTaskPackages(DeviceOwnerReceiver.getComponentName(this), packages);
        }


        startService(new Intent(this, ActiveNotification.class));
        sendBroadcast(new Intent(TaskManager.INIT_INTENT));


        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );


        if (devicePolicyManager.isLockTaskPermitted(getPackageName()))
            startLockTask();
    }


    @Override
    public void onAttachedToWindow() {
        addWindowFlags(
                LayoutParams.FLAG_FULLSCREEN
                        | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        //| LayoutParams.FLAG_KEEP_SCREEN_ON
                        | LayoutParams.FLAG_DISMISS_KEYGUARD
                //| LayoutParams.FLAG_TURN_SCREEN_ON
        );

        super.onAttachedToWindow();
/**        getWindow().getDecorView().setSystemUiVisibility(
 View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
 | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
 | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
 );
 */Log.d(TAG, "onAttachedToWindow");
    }


    @Override
    public void onResume()
    {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        Log.d(TAG, "onResume");
        activeState = STATE_INTERACTING;
        sendBroadcast(new Intent(activeState));
    }

    @Override
    protected void onPause() {
        super.onPause();
        /**if (activityManager != null)
         activityManager.moveTaskToFront( this.getTaskId(), 0);*/
        Log.d(TAG, "onPause");

        activeState = STATE_NOMINAL;
        sendBroadcast(new Intent(activeState));
    }


    @Override
    public void onDestroy()
    {
        Log.d(TAG, "DEATH");
        super.onDestroy();
        //sendBroadcast(new Intent("HitchDroid_dead"));
    }


    public void addWindowFlags(int flags) {
        getWindow().addFlags(flags);
    }

    public void delWindowFlags(int flags) {
        getWindow().clearFlags(flags);
    }


}


