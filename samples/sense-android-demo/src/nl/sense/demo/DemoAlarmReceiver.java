package nl.sense.demo;

import java.util.Random;

import nl.sense_os.service.constants.SenseDataTypes;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DemoAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "DemoAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.v(TAG, "Received alarm to finish the test");

        insertData();
        // MotionSensor.getInstance(context).doSample();
        flushData();
        Random random = new Random();
        long range = (long) (7 * 60000) - (long) (2 * 60000) + 1;
        long fraction = (long) (range * random.nextDouble());
        int randomNumber = (int) (fraction + (2 * 60000));
        DemoAlarmTool.schedule(context, randomNumber);
    }

    /**
     * An example of how to upload data for a custom sensor.
     */
    public void insertData() {
        Log.v(TAG, "Insert data point");

        // Description of the sensor
        final String name = MainActivity.DEMO_SENSOR_NAME;
        final String displayName = "demo data";
        final String dataType = SenseDataTypes.JSON;
        final String description = name;
        // the value to be sent, in json format
        final String value = "{\"foo\":\"bar\",\"baz\":\"quux\"}";
        final long timestamp = System.currentTimeMillis();

        // start new Thread to prevent NetworkOnMainThreadException
        new Thread() {

            @Override
            public void run() {
                MainActivity.application.getSensePlatform().addDataPoint(name, displayName,
                        description,
                        dataType, value, timestamp);
            }
        }.start();

    }

    public void flushData() {
        Log.v(TAG, "Flush buffers");
        MainActivity.application.getSensePlatform().flushData();
    }

}

