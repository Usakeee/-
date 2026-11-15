package com.usakeee.dalbitlullaby;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Called from the web page: start() when the lullaby begins, stop() when it ends. */
@CapacitorPlugin(name = "SleepKeeper")
public class SleepKeeperPlugin extends Plugin {

    @PluginMethod
    public void start(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                && getActivity() != null) {
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
    public void stop(PluginCall call) {
        MainActivity.keepAwake = false;
        getContext().stopService(new Intent(getContext(), PlaybackService.class));
        call.resolve();
    }
}
