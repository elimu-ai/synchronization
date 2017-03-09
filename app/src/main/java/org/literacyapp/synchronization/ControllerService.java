package org.literacyapp.synchronization;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by eli on 09/03/2017.
 */

public class ControllerService extends Service {

    private boolean isSender;
    private boolean isKillService = false;
    private long startTime;
    private long intervalTimeMillis;
    private GroupDeleteHelper gdh;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(P.Tag, "ControllerService destroy called");
        isKillService = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        gdh = new GroupDeleteHelper(getApplicationContext(), false);
        gdh.deleteGroups();
        Random rand = new Random();

        int intervalTimeDiff =rand.nextInt(P.CONTROLLER_DIFF_INTERVAL_TIME_SECS);
        startTime = System.currentTimeMillis();
        // will be between 5mins to 10mins
        int intervalTimeSecs = P.CONTROLLER_BASE_INTERVAL_TIME_MINS*60 + intervalTimeDiff;
        Log.d(P.Tag, "ControllerService intervalTimeSecs: " + intervalTimeSecs);
        intervalTimeMillis = intervalTimeSecs * 1000;


        isSender = rand.nextBoolean();
        Log.d(P.Tag, "ControllerService isSender: " + isSender);
        startWiFiDirectService();

        final Timer watchDogTimer = new Timer();
        watchDogTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (P.getStatus() == P.Status.SentOK || P.getStatus() == P.Status.ReceivedOK) {
                      reverseWiFiDirectServiceRolls();
                }
                if (isKillService) {
                    stopWiFiDirectService();
                    watchDogTimer.cancel();
                }
                if ( (System.currentTimeMillis() - startTime) > intervalTimeMillis) {
                    Log.d(P.Tag, "Controller session timed-out");
                    reverseWiFiDirectServiceRolls();
                }
            }
            // run every 10 secs (after 1 secs)
        }, 1000, 10000);

        return START_STICKY;
    }

    private void reverseWiFiDirectServiceRolls() {
        gdh.deleteGroups();
        startTime = System.currentTimeMillis();
        stopWiFiDirectService();
        try { Thread.sleep(3000); } catch (InterruptedException e) { }
        if (isSender) isSender = false;else isSender = true;
        startWiFiDirectService();
    }

    private void stopWiFiDirectService() {
        Intent i = new Intent(getApplicationContext(), WiFiDirectService.class);
        stopService(i);
    }

    private void startWiFiDirectService() {
        Intent i = new Intent(getApplicationContext(), WiFiDirectService.class);
        if (isSender)
            i.putExtra("sender_receiver", "sender");
        else
            i.putExtra("sender_receiver", "receiver");
        startService(i);
    }
}
