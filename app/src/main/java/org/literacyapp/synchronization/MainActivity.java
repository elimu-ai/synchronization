package org.literacyapp.synchronization;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.bluelinelabs.logansquare.LoganSquare;
import com.peak.salut.Callbacks.SalutCallback;
import com.peak.salut.Callbacks.SalutDataCallback;
import com.peak.salut.Callbacks.SalutDeviceCallback;
import com.peak.salut.Salut;
import com.peak.salut.SalutDataReceiver;
import com.peak.salut.SalutDevice;
import com.peak.salut.SalutServiceData;

import org.literacyapp.synchronization.jsonmodel.JsonFileModel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class MainActivity extends AppCompatActivity implements SalutDataCallback, View.OnClickListener {
    private static final String SERVICE_NAME = "LITERACYAPP_SYNCHRONIZATION_SERVICE";
    private static final int PORT = 47474;
    private static final String TEST_FILE_PATH = Environment.getExternalStorageDirectory() + "/img.jpg";
    private SalutDataReceiver dataReceiver;
    private SalutServiceData serviceData;
    private Salut network;
    private Button setupNetworkButton;
    private Button discoverServicesButton;
    private Button sendFileButton;
    private SalutDevice targetDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(getClass().getName(), "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Create a data receiver object that will bind the callback with some instantiated object from our app
        dataReceiver = new SalutDataReceiver(this, this);

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        //Populate the details for our service
        serviceData = new SalutServiceData(SERVICE_NAME, PORT, deviceId);

        /*Create an instance of the Salut class, with all of the necessary data from before.
        * We'll also provide a callback just in case a device doesn't support WiFi Direct, which
        * Salut will tell us about before we start trying to use methods.*/
        network = new Salut(dataReceiver, serviceData, new SalutCallback() {
            @Override
            public void call() {
                Log.e(getClass().getName(), "Sorry, but this device does not support WiFi Direct.");
            }
        });

        setupNetworkButton = (Button)findViewById(R.id.setupNetwork);
        setupNetworkButton.setOnClickListener(this);

        discoverServicesButton = (Button)findViewById(R.id.discoverServices);
        discoverServicesButton.setOnClickListener(this);

        sendFileButton = (Button)findViewById(R.id.sendFile);
        sendFileButton.setOnClickListener(this);
        sendFileButton.setEnabled(false);
    }

    @Override
    public void onDataReceived(Object data) {
        Log.d(getClass().getName(), "Received network data.");
        try
        {
            JsonFileModel jsonFileModel = LoganSquare.parse(data.toString().replace("\"{","{").replace("}\"","}").replace("\\",""), JsonFileModel.class);
            File file = new File(TEST_FILE_PATH);
            byteArrayToBitmapFile(jsonFileModel.byteArray, file);
            Log.i(getClass().getName(), "File stored under: " + file.getAbsolutePath());
        }
        catch (IOException ex)
        {
            Log.e(getClass().getName(), "Failed to parse network data.");
        }
    }

    private void setupNetwork()
    {
        if(!network.isRunningAsHost)
        {
            network.startNetworkService(new SalutDeviceCallback() {
                @Override
                public void call(SalutDevice salutDevice) {
                    Toast.makeText(getApplicationContext(), "Device: " + salutDevice.instanceName + " connected.", Toast.LENGTH_SHORT).show();
                    targetDevice = salutDevice;
                    enableFileSending();
                }
            });

            setupNetworkButton.setText("Stop Service");
            discoverServicesButton.setAlpha(0.5f);
            discoverServicesButton.setClickable(false);
        }
        else
        {
            network.stopNetworkService(false);
            setupNetworkButton.setText("Start Service");
            discoverServicesButton.setAlpha(1f);
            discoverServicesButton.setClickable(true);
        }
    }

    private void discoverServices()
    {
        if(!network.isRunningAsHost && !network.isDiscovering)
        {
            network.discoverNetworkServices(new SalutCallback() {
                @Override
                public void call() {
                    network.registerWithHost(network.foundDevices.get(0), new SalutCallback() {
                        @Override
                        public void call() {
                            Log.d(getClass().getName(), "We're now registered.");
                            enableFileSending();
                        }
                    }, new SalutCallback() {
                        @Override
                        public void call() {
                            Log.d(getClass().getName(), "We failed to register.");
                        }
                    });
                }
            }, true);
            discoverServicesButton.setText("Stop Discovery");
            setupNetworkButton.setAlpha(0.5f);
            setupNetworkButton.setClickable(false);
        }
        else
        {
            network.stopServiceDiscovery(true);
            discoverServicesButton.setText("Discover Services");
            setupNetworkButton.setAlpha(1f);
            setupNetworkButton.setClickable(false);
        }
    }

    @Override
    public void onClick(View view) {
        // Check if Wifi is enabled. Enable if not.
        if (!Salut.isWiFiEnabled(getApplicationContext())){
            WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(true);
        }

        switch (view.getId()){
            case R.id.setupNetwork:
                setupNetwork();
                break;
            case R.id.discoverServices:
                discoverServices();
                break;
            case R.id.sendFile:
                sendFile();
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(network.isRunningAsHost)
            network.stopNetworkService(true);
        else
            network.unregisterClient(true);
    }

    private byte[] bitmapFileToByteArray(File file){
        if (file.exists()){
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
            try {
                ObjectOutputStream out = new ObjectOutputStream(bos);
                out.writeObject(file);
                out.flush();
                return bos.toByteArray();
            } catch (IOException e) {
                Log.e(getClass().getName(), null, e);
                return null;
            }
        } else {
            Log.i(getClass().getName(), "bitmapFileToByteArray: File " + file.getAbsolutePath() + " does not exist.");
            return null;
        }
    }

    private void byteArrayToBitmapFile(byte[] byteArray, File file){
        try {
            Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(file));
        } catch (IOException e) {
            Log.e(getClass().getName(), null, e);
        }
    }

    private void enableFileSending(){
        sendFileButton.setEnabled(true);
    }

    private void sendFile(){
        File file = new File(TEST_FILE_PATH);
        JsonFileModel jsonFileModel = new JsonFileModel();
        jsonFileModel.byteArray = bitmapFileToByteArray(file);
        if (jsonFileModel.byteArray != null){
            String data = null;
            try {
                data = LoganSquare.serialize(jsonFileModel);
                if (network.isRunningAsHost){
                    if (targetDevice != null){
                        network.sendToDevice(targetDevice, data, new SalutCallback() {
                            @Override
                            public void call() {
                                Log.e(getClass().getName(), "sendToDevice: deviceName: " + targetDevice.deviceName + " Failure!");
                            }
                        });
                    }
                } else {
                    network.sendToHost(data, new SalutCallback() {
                        @Override
                        public void call() {
                            Log.e(getClass().getName(), "sendToHost: Failure!");
                        }
                    });
                }
            } catch (IOException e) {
                Log.e(getClass().getName(), null, e);
            }
        }
    }
}
