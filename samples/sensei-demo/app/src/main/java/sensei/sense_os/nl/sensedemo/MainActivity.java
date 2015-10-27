package sensei.sense_os.nl.sensedemo;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(new Runnable() {
            @Override
            public void run() {
                // clear data from the previous user
                SetupUser.clearUserPreferences(MainActivity.this);
                StoreAndSyncData.clearSensePreferences(MainActivity.this);

                // create a random user, login and join the public sense group
                SetupUser.setup(MainActivity.this);
                // create a sensor share it with the public sense group
                StoreAndSyncData.setup(MainActivity.this);
                // store sensor data
                StoreAndSyncData.storeSensorData(MainActivity.this, "OK");
                // upload the data immediately
                StoreAndSyncData.flushData(MainActivity.this);
            }
        }).start();
    }
}
