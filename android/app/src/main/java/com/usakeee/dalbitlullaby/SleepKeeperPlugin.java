package com.usakeee.dalbitlullaby;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import com.getcapacitor.JSObject;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Called from the web page: start() when the lullaby begins, stop() when it ends. */
@CapacitorPlugin(name = "SleepKeeper")
public class SleepKeeperPlugin extends Plugin {
    private boolean askedForNotifications = false;

    @PluginMethod
    public void start(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 33
                && !askedForNotifications
                && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                && getActivity() != null) {
            askedForNotifications = true;
            ActivityCompat.requestPermissions(getActivity(), new String[] { Manifest.permission.POST_NOTIFICATIONS }, 7);
        }
        Double maxMs = call.getDouble("maxMs", 0.0);
        Intent intent = new Intent(getContext(), PlaybackService.class);
        intent.putExtra("title", call.getString("title", "달빛 자장가"));
        intent.putExtra("maxMs", maxMs == null ? 0L : maxMs.longValue());
        try {
            ContextCompat.startForegroundService(getContext(), intent);
            MainActivity.keepAwake = true;
            call.resolve();
        } catch (Exception e) {
            call.reject("재생 유지 서비스를 시작하지 못했어요", e);
        }
    }

    @PluginMethod
    public void batteryStatus(PluginCall call) {
        PowerManager pm = (PowerManager) getContext().getSystemService(android.content.Context.POWER_SERVICE);
        boolean free = Build.VERSION.SDK_INT < 23 || (pm != null && pm.isIgnoringBatteryOptimizations(getContext().getPackageName()));
        JSObject r = new JSObject();
        r.put("unrestricted", free);
        call.resolve(r);
    }

    @PluginMethod
    public void requestUnrestricted(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + getContext().getPackageName()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(i);
            } catch (Exception e) {
                Intent i = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(i);
            }
        }
        call.resolve();
    }

    @PluginMethod
    public void stop(PluginCall call) {
        MainActivity.keepAwake = false;
        getContext().stopService(new Intent(getContext(), PlaybackService.class));
        call.resolve();
    }
}
