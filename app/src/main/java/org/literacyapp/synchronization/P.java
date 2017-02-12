package org.literacyapp.synchronization;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by eli on 15/01/2017.
 */

public class P {
    public static final String Tag = "syncl";
    public static final String TAG = "syncl";
    private static String hostAdress = null;
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

    public static String getHostAdress() {
        return hostAdress;
    }

    public static void setHostAdress(String hostAdress) {
        P.hostAdress = hostAdress;
    }


}
