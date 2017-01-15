package org.literacyapp.synchronization;

import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class Main2Activity extends AppCompatActivity implements WifiP2pManager.ChannelListener, DeviceActionListener {

    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = true;
    private boolean retryChannel = false;
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel channel;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        new DiscoverAsyncTask().execute();

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

    }

    @Override
    public void disconnect() {

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
}
