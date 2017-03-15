package org.literacyapp.synchronization;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by eli on 15/01/2017.
 */

public class P {
    public static final String Tag = "syncl";
    public static final String TAG = "syncl";
    public static final String testFileName = "test.jpg";
    public static final String SENDER = "sender";
    public static final String RECEIVER = "receiver";
    public static final int PORT = 8988;
    public static final int CONTROLLER_BASE_SESSION_INTERVAL_TIME_MINS = 5;
    public static final int CONTROLLER_DIFF_SESSION_INTERVAL_TIME_SECS = 5*60;

    public static final int CONTROLLER_RUN_TIME_MINS = 25;

    public static final String TEST_SUB_FOLDER1 = "/test_files/l1/";
    public static final String TEST_SUB_FOLDER2 = "/test_files/l1/l2/";
    public static final String TEST_FOLDER_NAME = "/test_files/";

    public static final String DEFAULT_OUTPUT_FOLDER_NAME = "wifi_direct_files";
    private static Status mStatus = Status.Idle;

    public enum Status {Idle,Discovering,FoundPeers, Connecting, Connected, SentOK, ReceivedOK,  }


    public static void setStatus(Status status) {
        mStatus = status;
    }

    public static Status getStatus() {
        return mStatus;
    }

    public static void setOutputFolder(Context ctx, String outputFolder) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("OutputFolder", outputFolder);
        editor.commit();
        Log.i(P.Tag, "OutputFolder: " + outputFolder + " set to persistent preferences");
    }

    public static String getOutputFolder(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String outputFolder = sp.getString("OutputFolder", null);
        if (outputFolder == null) {
            Log.i(P.Tag, "outputFolder from prefs is null, setting default");
            outputFolder = Environment.getExternalStorageDirectory() + "/" + P.DEFAULT_OUTPUT_FOLDER_NAME;
        }
        return outputFolder;
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
            if (currentDeviceIdsList.contains(deviceId)) {
                return true;
            }
            else return false;
        }

        public static void addDeviceId(Context ctx, String deviceId) {
            Set<String> currentDeviceIdsList = getDeviceIds(ctx);
            if (currentDeviceIdsList.contains(deviceId)) {
                Log.d(P.Tag, "Device: " + deviceId + " already in the list");
                return;
            }
            currentDeviceIdsList.add(deviceId);
            setDeviceIdStatus(ctx, deviceId, DeviceStatus.NA);

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
            SharedPreferences.Editor editor = sp.edit();
            editor.putStringSet("devices_ids", currentDeviceIdsList);
            editor.commit();
            Log.d(P.Tag, "Device: " + deviceId + " added to the list");
        }


        public static void setDeviceIdStatus(Context ctx, String deviceId, Enum deviceStatus) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(deviceId, deviceStatus.toString());
            editor.commit();
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
            boolean isAllFinished = true;
            Set<String> deviceIds = getDeviceIds(ctx);
            if (deviceIds == null) {
                Log.d(P.Tag, "Device List is empty, finishing");
                return false;
            }
            Iterator iter = deviceIds.iterator();
            while (iter.hasNext()) {
                String deviceId = (String)iter.next();
                String status = getDeviceStatus(ctx, deviceId);
                Log.d(P.Tag, "device: " + deviceId + " status: " + status);
                if (!status.equals(DeviceStatus.SentAndReceived.toString())) {
                    isAllFinished = false;
                    break;
                }
            }
            return isAllFinished;
        }
    }



}
