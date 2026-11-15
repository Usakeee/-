package com.usakeee.dalbitlullaby;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

/**
 * Foreground "media playback" service. It shows the ongoing notification and holds a CPU wake lock so
 * Android does not freeze the app (and its sound and sleep timer) once the screen turns off.
 */
public class PlaybackService extends Service {
    private static final String CHANNEL = "sleep_playback";
    private static final long SAFETY_MS = 10 * 60 * 1000L;
    private PowerManager.WakeLock wakeLock;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String title = intent != null ? intent.getStringExtra("title") : null;
        long maxMs = intent != null ? intent.getLongExtra("maxMs", 0L) : 0L;

        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26 && nm != null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "자장가 재생", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_moon)
                .setContentTitle("달빛 자장가")
                .setContentText(title == null || title.isEmpty() ? "편안한 밤 되세요" : title)
                .setContentIntent(pi)
                .setOngoing(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
        int type = Build.VERSION.SDK_INT >= 29 ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK : 0;
        ServiceCompat.startForeground(this, 1, n, type);

        releaseLock();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DalbitLullaby:playback");
            // With a sleep timer, never hold the lock much longer than the timer itself.
            wakeLock.acquire(maxMs > 0 ? maxMs + SAFETY_MS : 10 * 60 * 60 * 1000L);
        }
        return START_NOT_STICKY;
    }

    private void releaseLock() {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        wakeLock = null;
    }

    @Override
    public void onDestroy() {
        releaseLock();
        MainActivity.keepAwake = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
