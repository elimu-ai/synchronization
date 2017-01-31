package org.literacyapp.synchronization;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class WiFiDirectService extends Service implements WifiP2pManager.ChannelListener, DeviceActionListener, WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {

    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = true;
    private boolean retryChannel = false;
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;
    private boolean autoDiscover = true;
    private boolean isConnected = false;

    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private WifiP2pInfo info;

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }


    public WiFiDirectService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(P.Tag, "WiFiDirectService start sticky");

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                new DiscoverAsyncTask().execute();
                if (isConnected)
                    timer.cancel();
            }
        // run every 10 secs (after 1 secs)
        }, 1000, 10000);





        return START_STICKY;
    }

    private class DiscoverAsyncTask extends AsyncTask<Void, Void, Void> {

        private boolean discover() {
            Log.i(P.Tag, "discover called...");
            if (!isWifiP2pEnabled) {
                Log.w(P.Tag, "isWifiP2pEnabled false");
                return true;
            }

            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    Log.i(P.Tag, "discoverPeers onSuccess");

                }

                @Override
                public void onFailure(int reasonCode) {
                    Log.i(P.Tag, "discoverPeers onFailure");

                }
            });
            return true;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(P.Tag, "doInBackground");
            discover();
            return null;
        }






    }

    public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

        private WifiP2pManager manager;
        private WifiP2pManager.Channel channel;


        /**
         * @param manager WifiP2pManager system service
         * @param channel Wifi p2p channel
         * @param wiFiDirectService service associated with the receiver
         */
        public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                           WiFiDirectService wiFiDirectService) {
            super();
            this.manager = manager;
            this.channel = channel;

        }

        /*
         * (non-Javadoc)
         * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
         * android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(P.Tag, "WiFiDirectBroadcastReceiver onReceive called");
            String action = intent.getAction();



            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // UI update to indicate wifi p2p status.
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                Log.d(P.Tag, "P2P state changed: " + state);

                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    // Wifi Direct mode is enabled
                    setIsWifiP2pEnabled(true);
                    Log.d(P.Tag, "autoDiscover: " + autoDiscover);
                    if (!isConnected) {
                        new DiscoverAsyncTask().execute();
                    }

                } else {
                    setIsWifiP2pEnabled(false);
                    Log.i(P.Tag, "onReceive calls resetData because WiFi is not enabled");
                    resetData();

                }

            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

                Log.d(P.Tag, "====P2P peers changed");

                // request available peers from the wifi p2p manager. This is an
                // asynchronous call and the calling activity is notified with a
                // callback on PeerListListener.onPeersAvailable()
                if (manager != null) {
                    manager.requestPeers(channel, WiFiDirectService.this);
                }



            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

                Log.d(P.Tag , "P2P connection changed");

                if (manager == null) {
                    Log.e(P.Tag, "manager is null, exiting");
                    return;
                }

                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected()) {
                    Log.d(P.Tag , "==P2P connected==");
                }


            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                Log.d(P.Tag , "P2P WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");

            }
        }
    }


    public void resetData() {
        Log.w(P.Tag, "Not implemented");
    }

    @Override
    public void onChannelDisconnected() {

    }

    @Override
    public void showDetails(WifiP2pDevice device) {

    }

    @Override
    public void cancelDisconnect() {

    }

    @Override
    public void connect(WifiP2pConfig config) {
        Log.i(P.Tag, "Connect(WifiP2pConfig config) called.");
        Toast.makeText(this,"Connecting..." ,Toast.LENGTH_LONG).show();
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.i(P.Tag, "===connect onSuccess()");
                isConnected = true;
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                //  Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry.",Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    public void disconnect() {

    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        Log.i(P.Tag, "onConnectionInfoAvailable");

        if (info != null && info.groupOwnerAddress != null) {
            this.info = info;
            Log.i(P.Tag, "info.groupOwnerAddress.getHostAddress(): " + info.groupOwnerAddress.getHostAddress());
        }
        else {
            Log.w(P.Tag, "onConnectionInfoAvailable info is null");
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        Log.d(P.Tag, "==Peers Available");
        List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
        peers.addAll(wifiP2pDeviceList.getDeviceList());
        for (WifiP2pDevice device: peers) {
            Log.d(P.Tag, "deviceName: " + device.deviceName);
            Log.d(P.Tag, "deviceAddress: " + device.deviceAddress);
            Log.d(P.Tag, "device.status: " + device.status);
            if (device.status != WifiP2pDevice.CONNECTED) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                connect(config);
            }
        }
    }
}
