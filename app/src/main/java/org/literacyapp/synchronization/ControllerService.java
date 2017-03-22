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
 * Responsible for instantiating the WiFiDirectService, and according to the status of
 * The peers/devices.
 */

public class ControllerService extends Service {

    private boolean isSender;
    private boolean isKillService = false;
    private long startTime;
    private long sessionIntervalTimeMillis;
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

    private void cleanState() {
        gdh = new GroupDeleteHelper(getApplicationContext(), false);
        gdh.deleteGroups();
        P.DevicesHelper.cleanDeviceIds(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        cleanState();

        Random rand = new Random();

        int intervalTimeDiff = rand.nextInt(P.CONTROLLER_DIFF_SESSION_INTERVAL_TIME_SECS);
        startTime = System.currentTimeMillis();
        // session interval will be between 5mins to 10mins
        int intervalTimeSecs = P.CONTROLLER_BASE_SESSION_INTERVAL_TIME_MINS *60 + intervalTimeDiff;
        Log.d(P.Tag, "ControllerService intervalTimeSecs: " + intervalTimeSecs);
        sessionIntervalTimeMillis = intervalTimeSecs * 1000;

        isSender = rand.nextBoolean();
        Log.d(P.Tag, "===ControllerService isSender: " + isSender);
        startWiFiDirectService();

        // will run every 10secs till timer is canceled (delayed 1sec)
        final Timer sessionTimer = new Timer();
        sessionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(P.Tag, "===Controller session timer called");
                if (P.getStatus() == P.Status.SentOK || P.getStatus() == P.Status.ReceivedOK) {
                      Log.d(P.Tag, "===Session ended successfully, calling reverseWiFiDirectServiceRolls");
                      reverseWiFiDirectServiceRolls();
                }
                if (isKillService) {
                    stopWiFiDirectService();
                    sessionTimer.cancel();
                }
                if ( (System.currentTimeMillis() - startTime) > sessionIntervalTimeMillis) {
                    Log.d(P.Tag, "===Controller session timed-out");
                    reverseWiFiDirectServiceRolls();
                }
            }
            // run every 10 secs (after 1 secs)
        }, 1000, 10000);


        // will run every 20secs till timer is canceled (delayed 3secs)
        final Timer controllerTimer = new Timer();
        controllerTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(P.Tag, "===Controller global timer called");

                if (isKillService == true) {
                    Log.d(P.Tag, "controllerTimer got killService (ControllerService destroyed), stopping timer");
                    controllerTimer.cancel();
                }

                long controllerRunTimeMS = P.CONTROLLER_RUN_TIME_MINS * 60000;
                if ((startTime - System.currentTimeMillis() ) > controllerRunTimeMS) {
                    Log.w(P.Tag, "===Controller timed-out");
                    isKillService = true;
                    stopWiFiDirectService();
                    controllerTimer.cancel();
                }

                // All devices/peers have the SentOK & ReceivedOK state.
                if (P.DevicesHelper.isAllDevicesFinished(getApplicationContext())) {
                    Log.w(P.Tag, "===Controller finished OK");
                    isKillService = true;
                    stopWiFiDirectService();
                    controllerTimer.cancel();
                }

                // WiFiDirect Service watchdog
                if (!P.isWiFiDirectServiceRunning(getApplicationContext())) {
                    if (!isKillService) {
                        Log.w(P.Tag, "===WifiDirectService is not running, starting.");
                        startWiFiDirectService();
                    }
                    else {
                        Log.d(P.Tag, "isKillService is true, watchdog not starting service");
                    }
                }
                else Log.d(P.Tag, "===WifiDirectService is running OK.");



            }
            // run every 10 secs (after 1 secs)
        }, 3000, 20000);

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
            i.putExtra("sender_receiver", P.SENDER);
        else
            i.putExtra("sender_receiver", P.RECEIVER);
        startService(i);
    }
}
