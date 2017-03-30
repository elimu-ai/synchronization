package org.literacyapp.synchronization;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by eli on 15/01/2017.
 */

public class P extends PreferenceActivity {
    public static final String Tag = "syncl";
    public static final String TAG = "syncl";
    public static final String testFileName = "test.jpg";
    public static final String SENDER = "Sender";
    public static final String RECEIVER = "Receiver";
    public static transient String SENDER_RECEIVER_TYPE = null;
    public static final int PORT = 8988;
    public static final int CONTROLLER_BASE_SESSION_INTERVAL_TIME_MINS = 2;
    public static final int CONTROLLER_DIFF_SESSION_INTERVAL_TIME_SECS = 5*60;

    //public static final int CONTROLLER_RUN_TIME_MINS = 25;

    public static final String TEST_SUB_FOLDER1 = "/test_files/l1/";
    public static final String TEST_SUB_FOLDER2 = "/test_files/l1/l2/";
    public static final String TEST_FOLDER_NAME = "/test_files/";

    public static final String DEFAULT_OUTPUT_FOLDER_NAME = "wifi_direct_files";
    private static Status mStatus = Status.Idle;
    private static final String CONTROLLER_ACTION = "org.literacyapp.synchronization.ControllerService";

    public enum Status {Idle,Discovering,FoundPeers, Connecting, Connected, SentOK, ReceivedOK,  }


    public static void setStatus(Status status) {
        mStatus = status;
    }

    public static Status getStatus() {
        return mStatus;
    }


    public static class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs);
        }


    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }



    // getSyncFolderPath
    public static String getSyncFolderPath(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String outputFolder = sp.getString("sync_folder_path", ctx.getString(R.string.sync_folder_path_default));
        if (outputFolder == null) {
            Log.i(P.Tag, "outputFolder from prefs is null, setting default");
            outputFolder = Environment.getExternalStorageDirectory() + "/" + P.DEFAULT_OUTPUT_FOLDER_NAME;
        }
        return outputFolder;
    }

    public static boolean isSyncTestFolder(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean b = sp.getBoolean("sync_test_folder", true);
        return b;
    }


    public static String getMyFolderPath(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String f = sp.getString("my_folder_path", ctx.getString(R.string.my_folder_path_default));
        return f;
    }

    public static String getSyncStartTime(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String syncStartTime = sp.getString("sync_start_time", ctx.getString(R.string.start_time_default));
        return syncStartTime;
    }

    public static String getSyncMaxDurationMin(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String syncMaxDuration = sp.getString("sync_max_duration", ctx.getString(R.string.sync_max_duration_min_default));
        return syncMaxDuration;
    }


    public static int getSyncAlarmCycleHours(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String syncAlarmCycleHoursStr = sp.getString("sync_alarm_cycle_hours", ctx.getString(R.string.sync_alarm_cycle_hours_default));
        int syncAlarmCycleHours = Integer.parseInt(syncAlarmCycleHoursStr);
        return syncAlarmCycleHours;
    }

    public static String getFileNameFromFilePath(String filePath) {
        int index = filePath.lastIndexOf("/");
        return filePath.substring(index + 1 , filePath.length());
    }



    public static boolean createFolder(String folderPath) {
        File f = new File(folderPath);
        return f.mkdirs();
    }

    public static String getLocalTestFilePath(Context ctx) {
        String destinationFolderPath = ctx.getFilesDir().getAbsolutePath() + "/test_files/";
        String model = android.os.Build.MODEL.toLowerCase().replaceAll(" ", "_").trim();
        String destinationFilePath = destinationFolderPath + model + "_" + testFileName;
        return destinationFilePath;
    }

    public static String getTestFolderPath(Context ctx) {
        String testFolderPath = ctx.getFilesDir().getAbsolutePath() + TEST_FOLDER_NAME;
        return testFolderPath;
    }

    public static String getLocalTestFileName(Context ctx) {
        String model = android.os.Build.MODEL.toLowerCase().replaceAll(" ", "_").trim();
        String destinationFilePath =  model + "_" + testFileName;
        return destinationFilePath;
    }


    public static void copyTestFileFromAssetsToLocalAppFolderIfNeeded(Context ctx) {
        String destinationFolderPath = ctx.getFilesDir().getAbsolutePath() + TEST_FOLDER_NAME;
        File destFile = null;
        try {
            String localTestFilePath = getLocalTestFilePath(ctx);
            Log.d(P.Tag, "localTestFilePath: " + localTestFilePath);
            destFile = new File(localTestFilePath);
            if (!destFile.exists()) {
                File destFolder = new File(destinationFolderPath);
                boolean isFolderCreated = false;
                if (!destFolder.exists()) {
                    isFolderCreated = P.createFolder(destinationFolderPath);
                    Log.i(P.TAG, destinationFolderPath + " created? " + isFolderCreated);
                }
                if (isFolderCreated || destFolder.exists()) {
                    Log.i(P.TAG, "Adding test file at " + destFile.getAbsolutePath());
                    InputStream is = ctx.getAssets().open(testFileName);
                    BufferedOutputStream o = null;
                    try {
                        byte[] buff = new byte[10000];
                        int read = -1;
                        o = new BufferedOutputStream(new FileOutputStream(destFile), 10000);
                        while ((read = is.read(buff)) > -1) {
                            o.write(buff, 0, read);
                        }
                        Log.i(P.TAG, "Copy " + destFile.getAbsolutePath() + " from assets to app folder finished successfully");
                    }
                    catch (Exception e) {
                        Log.w(P.TAG, "Failed copying: " + destFile.getAbsolutePath());
                    }
                    finally {
                        is.close();
                        if (o != null) o.close();

                    }
                }
                else {
                    Log.w(P.TAG, "Test folder was not created.");
                }

            }
            else {
                Log.d(P.TAG, "Test file already exists, not copying test file.");

            }

        } catch (FileNotFoundException e) {
            Log.e(P.TAG, "FileNotFoundException: " + e.getMessage(), e);
        } catch (IOException e) {
            Log.e(P.TAG,"IOException: " +  e.getMessage());
        }
    }



    public static void createTestFolderIfNeeded(Context ctx) {
        String destinationFolderPath1 = ctx.getFilesDir().getAbsolutePath() + TEST_SUB_FOLDER1;
        String destinationFolderPath2 = ctx.getFilesDir().getAbsolutePath() + TEST_SUB_FOLDER2;
        File testFile = null;
        try {
            String localTestFilePath = destinationFolderPath2 + getLocalTestFileName(ctx);
            Log.d(P.Tag, "localTestFilePath: " + localTestFilePath);
            testFile = new File(localTestFilePath);
            if (!testFile.exists()) {
                File destFolder = new File(destinationFolderPath1);
                boolean isFolder1Created = false;
                boolean isFolder2Created = false;
                if (!destFolder.exists()) {
                    isFolder1Created = P.createFolder(destinationFolderPath1);
                    Log.i(P.TAG, destinationFolderPath1 + " created? " + isFolder1Created);
                    if (isFolder1Created) {
                        isFolder2Created = P.createFolder(destinationFolderPath2);
                        Log.i(P.TAG, destinationFolderPath2 + " created? " + isFolder2Created);
                    }

                }
                if (isFolder2Created ) {
                    Log.i(P.TAG, "Adding test file at " + testFile.getAbsolutePath());
                    InputStream is = ctx.getAssets().open(testFileName);
                    BufferedOutputStream o = null;
                    try {
                        byte[] buff = new byte[10000];
                        int read = -1;
                        o = new BufferedOutputStream(new FileOutputStream(testFile), 10000);
                        while ((read = is.read(buff)) > -1) {
                            o.write(buff, 0, read);
                        }
                        Log.i(P.TAG, "Copy " + testFile.getAbsolutePath() + " from assets to app folder finished successfully");
                    }
                    catch (Exception e) {
                        Log.w(P.TAG, "Failed copying: " + testFile.getAbsolutePath());
                    }
                    finally {
                        is.close();
                        if (o != null) o.close();

                    }
                }
                else {
                    Log.w(P.TAG, "Test folder was not created.");
                }

            }
            else {
                Log.d(P.TAG, "Test Folder already exists.");

            }

        } catch (FileNotFoundException e) {
            Log.e(P.TAG, "FileNotFoundException: " + e.getMessage(), e);
        } catch (IOException e) {
            Log.e(P.TAG,"IOException: " +  e.getMessage());
        }
    }


    public static String getVersionName(Context ctx) {
        String versionName = "";
        try {
            versionName = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(P.Tag, "No version code found, returning -1");
        }
        return versionName;
    }

    public static boolean isWiFiDirectServiceRunning(Context ctx) {
        return P.isMyServiceRunning(WiFiDirectService.class, ctx);
    }

    public static boolean isMyServiceRunning(Class<?> serviceClass, Context ctx) {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }



    public static class DevicesHelper {

        public enum DeviceStatus {NA,Sent,Received, SentAndReceived  }

        public static Set<String> getDeviceIds(Context ctx) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
            return sp.getStringSet("devices_ids", null);
        }

        public static void cleanDeviceIds(Context ctx) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
            SharedPreferences.Editor editor = sp.edit();
            editor.putStringSet("devices_ids", null);
            editor.commit();
        }

        public static boolean isDeviceIdInList(Context ctx, String deviceId) {
            Set<String> currentDeviceIdsList = getDeviceIds(ctx);
            if (currentDeviceIdsList == null) return false;
            if (currentDeviceIdsList.contains(deviceId)) {
                return true;
            }
            else return false;
        }

        public static void addDeviceId(Context ctx, String deviceId) {
            Set<String> currentDeviceIdsList = getDeviceIds(ctx);
            if (currentDeviceIdsList == null) {
                Log.i(P.Tag, "===currentDeviceIdsList is null, creating new empty one");
                currentDeviceIdsList = new HashSet<String>();

            }
            if (currentDeviceIdsList.contains(deviceId)) {
                Log.d(P.Tag, "===Device: " + deviceId + " already in the list");
                return;
            }
            currentDeviceIdsList.add(deviceId);
            setDeviceIdStatus(ctx, deviceId, DeviceStatus.NA);

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
            SharedPreferences.Editor editor = sp.edit();
            editor.putStringSet("devices_ids", currentDeviceIdsList);
            editor.commit();
            Log.d(P.Tag, "===Device: " + deviceId + " added to the list");
        }


        public static void setDeviceIdStatus(Context ctx, String deviceId, Enum deviceStatus) {
            Log.d(P.Tag, "===setDeviceIdStatus " + deviceId + " " + deviceStatus);
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(deviceId, deviceStatus.toString());
            editor.commit();
        }

        public static void setDeviceIdStatusSmart(Context ctx, String deviceId, Enum deviceStatus) {
            String currentStatus = getDeviceStatus(ctx, deviceId);
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
            SharedPreferences.Editor editor = sp.edit();
            if (currentStatus.equals(DeviceStatus.Received.toString()) && deviceStatus == DeviceStatus.Sent)  {
                editor.putString(deviceId, DeviceStatus.SentAndReceived.toString());
                editor.commit();
                Log.d(P.Tag, "===Device: " + deviceId + " set DeviceStatus.SentAndReceived");

            }
            else if  (currentStatus.equals(DeviceStatus.Sent.toString()) && deviceStatus == DeviceStatus.Received)  {
                editor.putString(deviceId, DeviceStatus.SentAndReceived.toString());
                editor.commit();
                Log.d(P.Tag, "===Device: " + deviceId + " set DeviceStatus.SentAndReceived");
            }
            else {
                editor.putString(deviceId, deviceStatus.toString());
                editor.commit();
                Log.d(P.Tag, "===Device: " + deviceId + " set: " + deviceStatus.toString());
            }


        }

        public static String getDeviceStatus(Context ctx, String deviceId) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
            return sp.getString(deviceId, DeviceStatus.NA.toString());
        }

        /**
         *
         * @return true if All devices/peers have the SentOK & ReceivedOK state.
         */
        public static boolean isAllDevicesFinished(Context ctx) {
            Log.d(P.Tag, "===isAllDevicesFinished?");
            boolean isAllFinished = true;
            Set<String> deviceIds = getDeviceIds(ctx);
            if (deviceIds == null) {
                Log.d(P.Tag, "===Device List is empty.");
                return false;
            }
            Iterator iter = deviceIds.iterator();
            while (iter.hasNext()) {
                String deviceId = (String)iter.next();
                String status = getDeviceStatus(ctx, deviceId);
                Log.d(P.Tag, "=== checking device: " + deviceId + " status: " + status);
                if (!status.equals(DeviceStatus.SentAndReceived.toString())) {
                    isAllFinished = false;
                    break;
                }
            }
            return isAllFinished;
        }


        public static String getDevicesStatusAsString(Context ctx) {
            //Log.d(P.Tag, "===getDevicesStatusAsString");
            String devicesStatusStr = "";
            String line = "";
            Set<String> deviceIds = getDeviceIds(ctx);
            if (deviceIds == null) {
                //Log.d(P.Tag, "===Device List is empty.");
                return "";
            }
            Iterator iter = deviceIds.iterator();
            while (iter.hasNext()) {
                String deviceId = (String)iter.next();
                String status = getDeviceStatus(ctx, deviceId);
                Log.d(P.Tag, "=== device: " + deviceId + " status: " + status);
                line = deviceId + " " + status + "\n";
                devicesStatusStr += line;
            }
            return devicesStatusStr;
        }

    }


    private static long getFirstTimeForAlarmInMS(Context ctx) {
        long timeForAlarmInMS = -1;
        String timeForAlarmStr = getSyncStartTime(ctx);
        Calendar cal = Calendar.getInstance();
        String dateStr = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        String dateWithTime = dateStr + " " + timeForAlarmStr;

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm");
        try {
            Date date = dateFormat.parse(dateWithTime);
            Date now = new Date();
            if (date.after(now)) {
                Log.d(P.Tag, "alarm start dateWithTime: " + dateWithTime);
                timeForAlarmInMS = date.getTime();
            }
            else {
                // add one day
                cal.add(Calendar.DATE, 1);
                dateStr = new SimpleDateFormat("dd-MM-yyyy").format(cal.getTime());
                dateWithTime = dateStr + " " + timeForAlarmStr;
                Log.d(P.Tag, "alarm start dateWithTime (one day added): " + dateWithTime);
                date = dateFormat.parse(dateWithTime);
                timeForAlarmInMS = date.getTime();
            }
        } catch (ParseException e) {
            Log.e(P.Tag, e.getMessage(), e);
        }
        return timeForAlarmInMS;
    }

    public static void startControllerServiceAlarmIfNotActive(Context ctx) {
        Log.d(P.Tag, "startControllerServiceAlarmIfNotActive");
        PendingIntent mAlarmSender = PendingIntent.getService(ctx, 0, new Intent(CONTROLLER_ACTION), 0);
        long firstTime = getFirstTimeForAlarmInMS(ctx);
        if (firstTime == -1) {
            Log.e(P.Tag, "No valid start time, not setting alarm");
            return;
        }
        AlarmManager am = (AlarmManager) ctx.getSystemService(ctx.ALARM_SERVICE);
        int alarmTimeHours = getSyncAlarmCycleHours(ctx);
        long alarmTimeMS = alarmTimeHours * 60 * 60 * 1000;
        Log.d(P.Tag, "Starting Repeating (ControllerService), Service alarm time (Hours): " + alarmTimeHours);
        am.setRepeating(AlarmManager.RTC_WAKEUP, firstTime, alarmTimeMS, mAlarmSender);
    }


    public static void stopControllerServiceAlarm(Context ctx) {
        PendingIntent mAlarmSender = PendingIntent.getService(ctx, 0, new Intent(CONTROLLER_ACTION), 0);
        AlarmManager am = (AlarmManager) ctx.getSystemService(ctx.ALARM_SERVICE);
        am.cancel(mAlarmSender);
        Log.d(P.Tag, "stopControllerServiceAlarm done");
    }

    /**
     *
     * @param ctx
     * @return 1 = on; 0 = off; -1 = undefined;
     */
    public static int isControllerServiceAlarmOn(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getInt("ControllerServiceAlarmOn", -1);
    }

    /**
     *
     * @param ctx
     * @param isOn 1 = on; 0 = off; -1 = undefined;
     */
    public static void setControllerServiceAlarm(Context ctx, int isOn) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("ControllerServiceAlarmOn", isOn);
        editor.commit();
    }



}
