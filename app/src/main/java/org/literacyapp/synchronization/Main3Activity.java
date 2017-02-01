package org.literacyapp.synchronization;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.w3c.dom.Text;

public class Main3Activity extends AppCompatActivity {

    private boolean isUIUpdateRunning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        Intent intent = new Intent(getApplicationContext(), WiFiDirectService.class);
        startService(intent);
        updateUI ();
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
    }

    private void updateUI () {
        final TextView statusText = ((TextView) findViewById(R.id.statusTextView));
        new Thread() {
            public void run() {
                Log.d(P.Tag,"Update UI started");
                try {
                    while (true) {

                        if (isUIUpdateRunning == false) {
                            Log.d(P.Tag, "isRun is false, breaking");
                            break;
                        }
                        try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
                        final String status = P.getStatus().toString();
                        Log.d(P.Tag, "check state");
                        runOnUiThread(new Runnable() { public void run() { statusText.setText(status); } });
                    }

                } catch(Exception e) {
                    Log.e("threadmessage",e.getMessage(), e);
                }
            }
        }.start();
    }
}
