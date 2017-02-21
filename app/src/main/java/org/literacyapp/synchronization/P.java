package org.literacyapp.synchronization;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by eli on 15/01/2017.
 */

public class P {
    public static final String Tag = "syncl";
    public static final String TAG = "syncl";
    public static final String testFileName = "test.jpg";
    public static final String SENDER = "sender";
    public static final String RECEIVER = "receiver";


    public static final String DEFAULT_OUTPUT_FOLDER_NAME = "wifi_direct_files";
    private static Status mStatus = Status.Idle;

    public enum Status {Idle,Discovering,FoundPeers, Connecting, Connected }

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


    public static void copyTestFileFromAssetsToLocalAppFolderIfNeeded(Context ctx) {
        String destinationFolderPath = ctx.getFilesDir().getAbsolutePath() + "/test_files/";
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


}
