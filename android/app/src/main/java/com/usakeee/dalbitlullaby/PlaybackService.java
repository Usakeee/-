package com.usakeee.dalbitlullaby;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
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
    private AudioManager audio;
    private AudioFocusRequest focusRequest;
    private boolean holdingFocus = false;

    /** An alarm or a ringing phone takes the audio focus: tell the page so the lullaby stops for good. */
    private final AudioManager.OnAudioFocusChangeListener focusListener = (change) -> {
        if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            holdingFocus = false;
            int mode = audio != null ? audio.getMode() : AudioManager.MODE_NORMAL;
            boolean call = mode == AudioManager.MODE_RINGTONE || mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION;
            SleepKeeperPlugin.emitInterrupted(call ? "call" : "alarm");
        }
        // AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK (a message notification) is ignored: the lullaby just keeps going.
    };

    private void takeFocus() {
        if (holdingFocus) return;
        audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audio == null) return;
        int r;
        if (Build.VERSION.SDK_INT >= 26) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setOnAudioFocusChangeListener(focusListener)
                    .build();
            r = audio.requestAudioFocus(focusRequest);
        } else {
            r = audio.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        holdingFocus = r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void dropFocus() {
        if (audio == null) return;
        if (Build.VERSION.SDK_INT >= 26 && focusRequest != null) audio.abandonAudioFocusRequest(focusRequest);
        else audio.abandonAudioFocus(focusListener);
        holdingFocus = false;
    }

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
        takeFocus();
        return START_NOT_STICKY;
    }

    private void releaseLock() {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        wakeLock = null;
    }

    @Override
    public void onDestroy() {
        releaseLock();
        dropFocus();
        MainActivity.keepAwake = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
