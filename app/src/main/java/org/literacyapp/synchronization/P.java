package org.literacyapp.synchronization;

/**
 * Created by eli on 15/01/2017.
 */

public class P {
    public static final String Tag = "syncl";
    private static Status mStatus = Status.Idle;

    public enum Status {Idle,Discovering,FoundPeers, Connecting, Connected }

    public static void setStatus(Status status) {
        mStatus = status;
    }

    public static Status getStatus() {
        return mStatus;
    }


}
