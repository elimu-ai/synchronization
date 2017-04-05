package org.literacyapp.synchronization;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by eli on 15/01/2017.
 */

public interface DeviceActionListener {
    void showDetails(WifiP2pDevice device);
    void cancelDisconnect();
    void connect(WifiP2pConfig config);
    void disconnect();
}
