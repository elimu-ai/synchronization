package org.literacyapp.synchronization;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private boolean isUIUpdateRunning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkForPermissionsMAndAbove();
        P.copyTestFileFromAssetsToLocalAppFolderIfNeeded(getApplicationContext());
        P.createTestFolderIfNeeded(getApplicationContext());
        setContentView(R.layout.activity_main);
        P.DevicesHelper.cleanDeviceIds(getApplicationContext());
        if (isPermissionsGranted()) {
            if (P.isControllerServiceAlarmOn(getApplicationContext()) == 1 || P.isControllerServiceAlarmOn(getApplicationContext()) == -1)
                P.startControllerServiceAlarmIfNotActive(getApplicationContext());
            else
                P.stopControllerServiceAlarm(getApplicationContext());
        }
        else {
            Log.w(P.Tag, "Permissions no granted, not setting alarm");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isUIUpdateRunning = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isUIUpdateRunning = true;
        updateUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        Log.i(P.Tag, "onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_alarm_start:
                Log.i(P.Tag, "action_alarm_start selected");
                P.startControllerServiceAlarmIfNotActive(getApplicationContext());
                P.setControllerServiceAlarm(getApplicationContext(), 1);
                return true;

            case R.id.action_alarm_stop:
                Log.i(P.Tag, "action_stop_controller selected");
                P.stopControllerServiceAlarm(getApplicationContext());
                P.setControllerServiceAlarm(getApplicationContext(), 0);
                return true;
            case R.id.action_stop_wifi_direct:
                Log.i(P.Tag, "action_stop_wifi_direct selected");
                Intent i1 = new Intent(getApplicationContext(), WiFiDirectService.class);
                stopService(i1);
                return true;

            case R.id.action_stop_controller:
                Log.i(P.Tag, "action_stop_controller selected");
                Intent i5 = new Intent(getApplicationContext(), ControllerService.class);
                stopService(i5);
                return true;

            case R.id.action_delete_groups:
                Log.i(P.Tag, "action_delete_groups selected");
                GroupDeleteHelper gh = new GroupDeleteHelper(getApplicationContext(), true);
                gh.deleteGroups();
                return true;

            case R.id.action_prefs:
                Log.i(P.Tag, "action_prefs selected");
                startActivity(new Intent(getApplicationContext(), P.class));
                return true;

            case R.id.action_start:
                Log.i(P.Tag, "action_start selected");
                Intent i4 = new Intent(getApplicationContext(), ControllerService.class);
                startService(i4);
                return true;


            case R.id.action_start_sender:
                Log.i(P.Tag, "action_start sender selected");
                P.DevicesHelper.cleanDeviceIds(getApplicationContext());
                GroupDeleteHelper gh2 = new GroupDeleteHelper(getApplicationContext(), true);
                gh2.deleteGroups();
                Intent i2 = new Intent(getApplicationContext(), WiFiDirectService.class);
                i2.putExtra("sender_receiver", P.SENDER);
                startService(i2);
                return true;

            case R.id.action_start_receiver:
                Log.i(P.Tag, "action_start receiver selected");
                P.DevicesHelper.cleanDeviceIds(getApplicationContext());
                GroupDeleteHelper gh3 = new GroupDeleteHelper(getApplicationContext(), true);
                gh3.deleteGroups();
                Intent i3 = new Intent(getApplicationContext(), WiFiDirectService.class);
                i3.putExtra("sender_receiver", P.RECEIVER);
                startService(i3);
                return true;


            case R.id.action_about:
                Log.i(P.Tag, "action_about");
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.about)).
                        setMessage(getAboutText()).
                        setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User clicked OK button
                            }
                        });

                AlertDialog about  = builder.create();
                about.show();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private String getAboutText() {
        StringBuffer b = new StringBuffer();
        b.append(getString(R.string.created_by) + "\n" + getString(R.string.version) + ":  ");
        b.append(P.getVersionName(getApplicationContext()) + "\n");
        return b.toString();
    }

    private void updateUI () {
        final TextView statusText = ((TextView) findViewById(R.id.statusTextView));
        final TextView devicesStatusText = ((TextView) findViewById(R.id.statusDevices));
        new Thread() {
            public void run() {
                //Log.d(P.Tag,"Status Update started");
                try {
                    while (true) {

                        if (isUIUpdateRunning == false) {
                            Log.d(P.Tag, "Stopping status update.");
                            break;
                        }
                        try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
                        final String status = P.getStatus().toString();
                        final String type = P.SENDER_RECEIVER_TYPE;
                        //Log.d(P.Tag, "check state");
                        runOnUiThread(new Runnable() { public void run() {
                            String textStr = null;
                            if (type != null) {
                                textStr = type + " " + status;
                            }
                            else {textStr = status;}
                            statusText.setText(textStr);
                            devicesStatusText.setText(P.DevicesHelper.getDevicesStatusAsString(getApplicationContext()));
                        } });
                    }

                } catch(Exception e) {
                    Log.e("threadmessage",e.getMessage(), e);
                }
            }
        }.start();
    }

    private boolean isPermissionsGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                    ||
                    checkSelfPermission(
                            Manifest.permission.ACCESS_WIFI_STATE)
                            != PackageManager.PERMISSION_GRANTED

                    ) {
                return false;
            }
            else {
                return true;
            }

        }
        else {
            return true;
        }

    }

    protected void checkForPermissionsMAndAbove() {
        Log.i(P.Tag, "checkForPermissions() called");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Here, thisActivity is the current activity
            if (checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                    ||
                    checkSelfPermission(
                            Manifest.permission.ACCESS_WIFI_STATE)
                            != PackageManager.PERMISSION_GRANTED

                    ) {


                // No explanation needed, we can request the permission.
                requestPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_WIFI_STATE,
                                Manifest.permission.WAKE_LOCK,
                                Manifest.permission.CHANGE_WIFI_STATE,
                        },
                        0);



            }
            // permission already granted
            else {
                Log.i(P.Tag, "permission already granted");
            }
        }

    }
}
