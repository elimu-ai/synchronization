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
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class WiFiDirectService extends Service implements WifiP2pManager.ChannelListener, DeviceActionListener, WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {

    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = true;
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;
    private FileServerAsyncTask fileServerAsyncTask;
    private int connectingCounter = 0;
    private boolean isKillService = false;
    private static int fileIndex = 0;
    private String META_DELIMITER = ":::";
    private String folderFromSend = null;
    private File folder2send = null;
    private static final String FINISH_STR = "###finish###";
    private String senderReceiverType = null;
    private List<WifiP2pDevice> peers = new ArrayList<>();
    private String currentDevice = null;



    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }


    public WiFiDirectService() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(P.Tag, "WiFiDirectService onDestroy called");
        stop();

    }

    private void stop() {
        isKillService = true;
        P.setStatus(P.Status.Idle);
        if (fileServerAsyncTask != null)
            fileServerAsyncTask.close();
        removeGroup();
        if (receiver != null) {
            try {
                unregisterReceiver(receiver);
            }
            catch (Exception e) {
                Log.w(P.Tag, "unregisterReceiver failed", e);
            }
        }

    }

    private void restart() {
        stop();
        try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
        start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(P.Tag, "WiFiDirectService start sticky");

        if (intent != null && intent.getExtras() != null) {
            senderReceiverType = intent.getExtras().getString("sender_receiver");

        }

        start();

        return START_STICKY;
    }

    private void start() {

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        if (senderReceiverType != null) {
            Log.i(P.Tag, "senderReceiverType: " + senderReceiverType);
            P.SENDER_RECEIVER_TYPE = senderReceiverType;
        }
        else
            Log.w(P.Tag, "senderReceiverType is null");

        isKillService = false;
        manager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);

        final Timer discoverTimer = new Timer();
        discoverTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isKillService) {
                    Log.d(P.Tag, "stopping discoverTimer (serviceKill)");
                    discoverTimer.cancel();
                }
                // if status is FoundPeers or more stop discovery.
                if  (P.getStatus().ordinal() <= 1) {
                    new DiscoverAsyncTask().execute();
                 }
                else {
                    Log.d(P.Tag, "stopping discoverTimer status is FoundPeers or more");
                    discoverTimer.cancel();
                }
            }
            // run every 10 secs (after 1 secs)
        }, 1000, 10000);


        final Timer connectingWdTimer = new Timer();
        connectingWdTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isKillService) {
                    Log.d(P.Tag, "stopping connectingWdTimer(isKillService)");
                    connectingWdTimer.cancel();
                }
                // if status is FoundPeers or more stop discovery.
                if  (P.getStatus() == P.Status.Connecting) {
                    connectingCounter++;
                    Log.d(P.Tag, "connectingCounter: " + connectingCounter);
                }
                if (connectingCounter == 20) {
                    connectingCounter = 0;
                    Log.d(P.Tag, "connectingCounter starting DiscoverAsyncTask");
                    new DiscoverAsyncTask().execute();
                }
                if (P.getStatus() == P.Status.Connected) {
                    Log.d(P.Tag, "connectingCounter: connectingWdTimer.cancel()");
                    connectingWdTimer.cancel();
                }
            }
            // run every 10 secs (after 1 secs)
        }, 2000, 2000);
    }



    private class DiscoverAsyncTask extends AsyncTask<Void, Void, Void> {

        private boolean discover() {
            Log.i(P.Tag, "discover called...");
            P.setStatus(P.Status.Discovering);
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
            Log.d(P.Tag, "DiscoverAsyncTask doInBackground");
            if (!isKillService)
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

                    if (P.getStatus().ordinal() <= 1) {
                        new DiscoverAsyncTask().execute();
                    }

                } else {
                    setIsWifiP2pEnabled(false);
                    Log.i(P.Tag, "onReceive calls resetData because WiFi is not enabled");
                    resetData();

                }

            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

                Log.d(P.Tag, "P2P peers changed");
                P.setStatus(P.Status.FoundPeers);

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
                    Log.i(P.TAG, "==P2P connected==");
                    //Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
                    P.setStatus(P.Status.Connected);

                    manager.requestConnectionInfo(channel, WiFiDirectService.this);

                    /*
                    if (senderReceiverType.equals(P.RECEIVER)) {
                        Log.i(P.TAG, "FileServerAsyncTask started listening...");
                        fileServerAsyncTask = new FileServerAsyncTask();
                        fileServerAsyncTask.execute();
                    }
                    else {
                        Log.i(P.Tag, "senderReceiverType is sender, not starting receiver");
                    }
                    */

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
        //Toast.makeText(this,"Connecting..." ,Toast.LENGTH_SHORT).show();
        P.setStatus(P.Status.Connecting);
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.i(P.Tag, "connect onSuccess()");
                P.setStatus(P.Status.Connected);
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Log.i(P.Tag, "connect onFailure(): " + reason);

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
            Log.i(P.Tag, "info.groupOwnerAddress.getHostAddress(): " + info.groupOwnerAddress.getHostAddress());
            if (senderReceiverType != null) {

                if (senderReceiverType.equals(P.SENDER)) {
                    Log.i(P.Tag, "Forced as sender");
                    //sendTestFile(info.groupOwnerAddress.getHostAddress());
                    sendTestFolder(info.groupOwnerAddress.getHostAddress());
                }
                else {
                    if (info.groupFormed && info.isGroupOwner) {
                        Log.i(P.TAG, "Forced as receiver, FileServerAsyncTask started listening...");
                        fileServerAsyncTask = new FileServerAsyncTask();
                        fileServerAsyncTask.execute();
                    }
                }
            }
            else {
                Log.i(P.Tag, "info.isGroupOwner: " + info.isGroupOwner);
                if (info.groupFormed && info.isGroupOwner) {
                    Log.i(P.TAG, "FileServerAsyncTask started listening...");
                    fileServerAsyncTask = new FileServerAsyncTask();
                    fileServerAsyncTask.execute();
                } else if (info.groupFormed) {
                    sendTestFile(info.groupOwnerAddress.getHostAddress());
                } else {
                    Log.w(P.TAG, "Group not formed yet...");
                }
            }
        }
        else {
            Log.w(P.Tag, "onConnectionInfoAvailable info is null");
        }
    }

    /**
     * device.deviceName should be unique, or else this method will not work correctly.
     * The device name is set in the Wifi Direct settings, there should not be 2 devices with the same name in a group (Village)
     * @param wifiP2pDeviceList
     */
    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        Log.d(P.Tag, "==Peers Available");
        List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
        peers.addAll(wifiP2pDeviceList.getDeviceList());
        Log.d(P.Tag, "peers.size(): " + peers.size());

        if (peers.size() == 0) {
            Log.w(P.Tag, "peers size is 0, calling restart...");
            restart();
        }

        for (WifiP2pDevice device: peers) {
            Log.d(P.Tag, "deviceName: " + device.deviceName);
            Log.d(P.Tag, "deviceAddress: " + device.deviceAddress);
            Log.d(P.Tag, "device.status: " + device.status);

            String deviceStatus = P.DevicesHelper.DeviceStatus.NA.toString();
            if (P.DevicesHelper.isDeviceIdInList(getApplicationContext(), device.deviceName)) {
                deviceStatus =  P.DevicesHelper.getDeviceStatus(getApplicationContext(), device.deviceName);
            }
            else {
                Log.d(P.Tag, "===device: "  + " not in list, setting status to NA.");
                P.DevicesHelper.addDeviceId(getApplicationContext(), device.deviceName);
                P.DevicesHelper.setDeviceIdStatus(getApplicationContext(),device.deviceName, P.DevicesHelper.DeviceStatus.NA);
            }

            if (device.status != WifiP2pDevice.CONNECTED) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;

                boolean isAbortConnection = false;
                if (senderReceiverType != null) {
                    if (senderReceiverType.equals(P.SENDER)) {
                        Log.i(P.TAG, "not Receiver, config.groupOwnerIntent 0");
                        config.groupOwnerIntent = 0;
                        if (deviceStatus.equals(P.DevicesHelper.DeviceStatus.Received) || deviceStatus.equals(P.DevicesHelper.DeviceStatus.SentAndReceived)) {
                            isAbortConnection = true;
                            Log.i(P.Tag, "isAbortConnection true, device already Received information");
                        }
                    } else {
                        Log.i(P.TAG, "setReceiver, config.groupOwnerIntent 15 ");
                        config.groupOwnerIntent = 15;
                        if (deviceStatus.equals(P.DevicesHelper.DeviceStatus.Sent) || deviceStatus.equals(P.DevicesHelper.DeviceStatus.SentAndReceived)) {
                            isAbortConnection = true;
                            Log.i(P.Tag, "isAbortConnection true, device already Sent information");
                        }
                    }
                }
                if (!isAbortConnection) {
                    connect(config);
                    P.setStatus(P.Status.Connecting);
                    currentDevice = device.deviceName;
                }
            }

        }
    }

    private void sendTestFile(String hostAddress) {
        String testFilePath  = P.getLocalTestFilePath(getApplicationContext());
        Log.d(P.Tag, "sendTestFile(): " + testFilePath);
        File testFile = new File(testFilePath);
        List<File> l = new ArrayList<File>();
        l.add(testFile);
        new FilesSendAsyncTask(getApplicationContext(), l, hostAddress, P.PORT).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void sendTestFolder(String hostAddress) {
        String testFolderPath = P.getTestFolderPath(getApplicationContext());
        Log.d(P.Tag, "sendTestFolder(): " + testFolderPath);
        File testFolder = new File(testFolderPath);
        new FilesSendAsyncTask(getApplicationContext(), testFolder, hostAddress, P.PORT).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }




    public class FileServerAsyncTask extends AsyncTask<Void, Integer, String> {
        PowerManager.WakeLock wakeLock = null;
        ServerSocket serverSocket = null;
        Socket client = null;
        boolean isCanceled = false;
        boolean isFinished = false;
        boolean isEnabled = true;

        public void close() {
            isEnabled = false;
            Log.i(P.TAG, "trying to close ServerSocket...");
            try {
                if (serverSocket != null)
                    serverSocket.close();
                else
                    Log.i(P.TAG, "ServerSocket is null, not closing");

            } catch (IOException e) {
                Log.e(P.TAG, "failed to close ServerSocket");
            }

            if (wakeLock != null) {
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                    Log.i(P.TAG, "Wake lock released");
                }
            }
            else{
                Log.i(P.TAG, "Wake lock is already released, doing nothing");
            }
            Log.i(P.TAG, "ServerSocket closed.");
        }

        public FileServerAsyncTask() {
        }


        public FileServerAsyncTask(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        public  String copyFileIn(InputStream inputStream, OutputStream out) {
            byte buf[] = new byte[1024];
            int len;
            boolean firstReadSection = true;
            String ret = null;
            String meta = null;
            try {
                while ((len = inputStream.read(buf)) != -1 && isCanceled == false) {
                    if (!firstReadSection) {
                        out.write(buf, 0, len);
                    }

                    // option 1: /wifi_direct/in.mp4v(file path)
                    // option 2: /wifi_direct/in.mp4:::videokit (filePath:::folder_name)

                    else {
                        meta = new String (buf).trim();
                        Log.d(P.TAG, "copyFileIn, first buf: " + meta);
                        if (meta.contains(META_DELIMITER)) {
                            String[] strs = meta.split(META_DELIMITER);
                            ret = strs[0];
                            folderFromSend = strs[1];
                        }
                        else {
                            folderFromSend = null;

                            ret = new String (buf).trim();
                            if (ret.equals(FINISH_STR)) {
                                Log.i(P.Tag, "got finish str on the receiver");
                                ret = null;
                                isFinished = true;
                                break;
                            }

                        }
                    }
                    firstReadSection = false;

                }

                if (isCanceled) {
                    Log.i(P.TAG, "broke out of the recieve loop.");
                }

                out.close();
                inputStream.close();


            } catch (IOException e) {
                Log.d(P.TAG, e.toString());
                return ret;
            }
            return ret;
        }

        // folderName = vid_compress, Avengers Age of Ultron (2015)
        // sentFilePath = /storage/emulated/0/wifi_direct_files/vid_compress/in.mp3
        //                  /storage/emulated/0/Download/Avengers Age of Ultron (2015)/Avengers.Age.of.Ultron.2015.720p.BluRay.x264.YIFY.mp4
        // outputFolder = /storage/emulated/0/wifi_direct_files
        private String createFolder(String folderName, String sentFilePath, String outputFolder) {

            String folderNameS = folderName.replaceAll("[()!?=<>@]","");
            String sentFilePathS = sentFilePath.replaceAll("[()!?=<>@]","");

            String[] strs = sentFilePathS.split(folderNameS);
            //Log.d(P.TAG, strs[0] + " " + strs[1]);
            String path1 = folderNameS + strs[1].substring(0, strs[1].lastIndexOf("/"));
            //Log.d(P.TAG, path1);
            String newFolderPath = outputFolder + "/" + path1;
            Log.d(P.TAG, "newFolderPath: " + newFolderPath);
            boolean isDirCreated = new File(newFolderPath).mkdirs();
            Log.d(P.TAG, "isDirCreated: " + isDirCreated);
            return newFolderPath;
        }


        @Override
        protected String doInBackground(Void... params) {

            Log.i(P.TAG, "FileServerAsyncTask doInBackground started");

            try {

                if (serverSocket == null) {
                    serverSocket = new ServerSocket(P.PORT);
                    Log.d(P.TAG, "Server: Socket opened: " + P.PORT);
                }
                // blocking till a client is connecting
                client = serverSocket.accept();

                Log.d(P.TAG, "Server: connection done");

                PowerManager powerManager = (PowerManager)getApplicationContext().getSystemService(POWER_SERVICE);
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FT_LOCK");
                Log.i(P.TAG, "Acquire wake lock");
                wakeLock.acquire();
                File f = null;

                String outputFolderFromPrefs = P.getOutputFolder(getApplicationContext());
                String outputFolderPath = null;
                if (outputFolderFromPrefs == null) {
                    Log.w(P.TAG, "outputFolderFromPrefs no set, using default");
                    outputFolderPath = Environment.getExternalStorageDirectory() + "/" + P.DEFAULT_OUTPUT_FOLDER_NAME;
                }
                else {
                    Log.i(P.TAG, "outputFolderFromPrefs is set: " + outputFolderFromPrefs);
                    outputFolderPath = outputFolderFromPrefs;
                }


                String pathNoExt = outputFolderPath + "/wifip2pshared";
                f = new File(pathNoExt);

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();

                Log.d(P.TAG, "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();

                String path = copyFileIn(inputstream, new FileOutputStream(f));
                if (path != null) {
                    if (folderFromSend == null) {
                        Log.d(P.Tag, "file copy");
                        String fileName = P.getFileNameFromFilePath(path);
                        String fullPath = outputFolderPath + "/" + fileName;
                        Log.d(P.TAG, "Rename to: " + fullPath);
                        File f2 = new File(fullPath);
                        boolean isRenameOK = f.renameTo(f2);
                        Log.d(P.TAG, "isRenameOK: " + isRenameOK);
                        //P.setStatus(P.Status.FileReceived);
                        return f2.getAbsolutePath();
                    }
                    else {
                        Log.d(P.Tag, "folder copy");
                        String folderPath = createFolder(folderFromSend, path, outputFolderPath);
                        String fileName = P.getFileNameFromFilePath(path);
                        String fullPath = folderPath + "/" + fileName;
                        Log.d(P.TAG, "trying to rename to: " + fullPath);
                        File f2 = new File(fullPath);
                        boolean isRenameOK = f.renameTo(f2);
                        Log.d(P.TAG, "isRenameOK: " + isRenameOK);
                        return f2.getAbsolutePath();

                    }
                }
                else {
                    Log.i(P.Tag, "Got finished message from sender");
                    P.setStatus(P.Status.ReceivedOK);
                    P.DevicesHelper.setDeviceIdStatusSmart(getApplicationContext(), currentDevice, P.DevicesHelper.DeviceStatus.Sent);
                    return null;
                }
            } catch (IOException e) {
                Log.e(P.TAG, e.getMessage());
                return null;
            } finally {
                if (wakeLock != null) {
                    if (wakeLock.isHeld()) {
                        wakeLock.release();
                        Log.i(P.TAG, "Wake lock released");
                    }
                }
                else{
                    Log.i(P.TAG, "Wake lock is already released, doing nothing");
                }
            }


        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

        }


        @Override
        protected void onPostExecute(String result) {

            if (isCanceled) {

            }
            else  {
                if (result != null) {

                }
            }

            // reseting isCanceled state for the next operation
            if (isCanceled) {
                Log.i(P.TAG, "Reseting isCanceled state for the next operation");
                isCanceled = false;
            }

            if (!isFinished) {
                try {
                    Log.d(P.TAG, "onPostExecute starting FileServerAsyncTask");
                    if (isEnabled) {
                        fileServerAsyncTask = new FileServerAsyncTask(serverSocket);
                        fileServerAsyncTask.execute();
                    }
                    else {
                        Log.i(P.TAG, "Not starting FileServerAsyncTask onPostExecute service is not enabled");
                    }

                } catch (Exception e) {
                    Log.w(P.TAG, e.getMessage(), e);
                }
            }
            else {
                Log.d(P.TAG, "isFinished is true, not starting new fileServerAsyncTask");
                //folder2send = null;
                folderFromSend = null;
                isFinished = false;
            }




        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

    }


    public class FilesSendAsyncTask extends AsyncTask<Void, Integer, Integer> {
        private static final int SOCKET_TIMEOUT = 2000;
        private Context context;
        List files = null;
        private String host = null;
        private int port = -1;
        private Socket socket = null;
        private PowerManager.WakeLock wakeLock = null;

        public FilesSendAsyncTask(Context context, List<File> files, String host, int port) {
            this.context = context;
            this.files = files;
            this.host = host;
            this.port = port;
            Log.i(P.Tag, "FilesSendAsyncTask: " + " sending files: " + files.size());
        }


        public FilesSendAsyncTask(Context context, File folder, String host, int port) {
            this.context = context;
            folder2send = folder;
            this.files = new ArrayList<File>();
            createFileListFromFolder();
            this.host = host;
            this.port = port;
        }


        private void traverse(File f) {
            if (f.isDirectory()) {
                Log.d(P.Tag,"Scanning dir: " + f.getAbsolutePath());
                File[] files1 = f.listFiles();
                for (int i = 0; i < files1.length; i++) {
                    traverse(files1[i]);
                }
                Log.d(P.Tag, "");
            }
            else {
                Log.d(P.Tag,f.getAbsolutePath());
                files.add(f);
            }

        }

        private void createFileListFromFolder() {
            traverse(folder2send);
        }


        public boolean copyFileOut(InputStream inputStream, OutputStream out, String path, long inputFileLength) {
            byte buf[] = new byte[1024];
            int len;
            boolean firstReadSection = true;
            int fileChunks = Math.round( inputFileLength / 1024);
            int chunksCounter = 0;
            try {
                while ((len = inputStream.read(buf)) != -1) {
                    if (!firstReadSection) {
                        out.write(buf, 0, len);
                        chunksCounter++;
                        int progress = Math.round(((float)chunksCounter / fileChunks) * 100);
                        publishProgress(progress);
                    }
                    else {
                        // write meta only the first time
                        StringBuffer extsb = new StringBuffer();
                        extsb.append(path);
                        if (folder2send != null) {
                            extsb.append(META_DELIMITER);
                            extsb.append(P.getFileNameFromFilePath(folder2send.getAbsolutePath()));
                        }
                        extsb.setLength(1024);
                        Log.d(P.TAG, "metadata (trimmed) : " + extsb.toString().trim());
                        out.write(extsb.toString().getBytes(), 0, 1024);
                        /////////////////////////////////
                        out.write(buf, 0, len);

                    }
                    firstReadSection = false;

                }
                Log.i(P.Tag, "closing output and input streams");
                out.close();
                inputStream.close();

            } catch (IOException e) {
                Log.d(P.TAG, e.toString());
                return false;
            }
            return true;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            Log.i(P.TAG, "FilesSendAsyncTask doInBackground started");
            if (files == null || files.size() < 1) {
                Log.e(P.Tag, "files to send are empty FilesSendAsyncTask ending");
            }
            try {
                File f = (File)files.get(fileIndex);
                String fileUri = f.getAbsolutePath();

                if (socket == null) {
                    Log.d(P.TAG, "Opening client socket...");
                    socket = new Socket();
                    socket.bind(null);
                    socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                }

                Log.d(P.TAG, "socket.isConnected(): " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                InputStream is = null;

                File file = null;
                try {
                    file = new File(fileUri);
                    is = new FileInputStream(file);

                    boolean isOK = copyFileOut(is, stream, fileUri, file.length());
                    Log.i(P.TAG, "file: " + fileUri + " is sent: " + isOK);

                } catch (FileNotFoundException e) {
                    Log.e(P.TAG, e.toString());
                }

            } catch (IOException e) {
                Log.e(P.TAG, e.getMessage());
                return -1;
            }
            return 0;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int p = values[0];

        }

        private void sendFinishMsg() {
            // send finish msg
            new Thread() {
                public void run() {
                    try {
                        folder2send = null;
                        Log.d(P.TAG, "socket.socket.isClosed(): " + socket.isClosed());
                        Log.d(P.TAG, "socket.isBound(): " + socket.isBound());

                        Log.d(P.TAG, "Opening new client socket...");
                        socket = new Socket();
                        socket.bind(null);
                        socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                        Log.d(P.TAG, "socket.isConnected(): " + socket.isConnected());
                        Log.d(P.TAG, "socket.getOutputStream()" );
                        OutputStream out = socket.getOutputStream();
                        StringBuffer extsb = new StringBuffer();
                        extsb.append(FINISH_STR);
                        extsb.setLength(1024);
                        Log.d(P.TAG, "str.getBytes().length: " + extsb.toString().getBytes().length);
                        Log.i(P.Tag, "sending finish string");
                        out.write(extsb.toString().getBytes(), 0, 1024);

                        if (socket.isConnected()) {
                            try {
                                Log.d(P.TAG, "closing socket");
                                socket.close();
                            } catch (IOException e) {
                                Log.e(P.TAG, e.getMessage());
                            }
                        }

                        P.setStatus(P.Status.SentOK);
                        P.DevicesHelper.setDeviceIdStatusSmart(getApplicationContext(), currentDevice, P.DevicesHelper.DeviceStatus.Received);
                    } catch (IOException e1) {
                        Log.e(P.Tag, "failed to send finish msg: " + e1.getMessage());
                        restart();
                    }

                }
            }.start();

        }



        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            boolean isOK = false;
            if (result == 0) isOK = true;
            if (result != null) {
                // statusText.setText("File copied: " + isOK);
                //TODO elih
                //Toast.makeText(getActivity(), "Copy finished", Toast.LENGTH_LONG).show();
                Log.i(P.Tag, "Copy finished, is OK: " + isOK);
            }



            Log.i(P.TAG, "fileIndex: " + fileIndex + " files.size(): " + files.size());
            if (fileIndex < files.size() - 1) {
                fileIndex++;
                Log.d(P.TAG, "Starting FilesSendAsyncTask with fileIndex: " + fileIndex);
                new FilesSendAsyncTask(getApplicationContext(), files, host, P.PORT).execute();
            }
            else {
                Log.d(P.TAG, "reseting fileIndex for the next iteration");
                fileIndex = 0;

                sendFinishMsg();
                Log.i(P.TAG, "Sleeping a little, letting the finish msg arrive");
                try {Thread.sleep(500);} catch (InterruptedException e) {}

                if (wakeLock.isHeld()) {
                    wakeLock.release();
                    Log.i(P.TAG, "Wake lock released");
                }
                else{
                    Log.i(P.TAG, "Wake lock is already released, doing nothing");
                }

            }

        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            PowerManager powerManager = (PowerManager)context.getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FT_LOCK");
            Log.i(P.TAG, "Acquire wake lock");
            wakeLock.acquire();
        }

    }


    private void removeGroup() {
        if (manager != null) {
            Log.i(P.Tag, "manager.removeGroup...");
            manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onFailure(int reasonCode) {
                    Log.d(P.TAG, "manager.removeGroup failed, Reason( BUSY-2, Error-0, P2P_UNSUPPORTED-1) :" + reasonCode);
                }

                @Override
                public void onSuccess() {
                    Log.d(P.TAG, "managerBase.removeGroup : OK" );
                }

            });
        }

        if (peers != null)
            peers.clear();
        else
            Log.i(P.Tag, "peers are null");


    }








}
