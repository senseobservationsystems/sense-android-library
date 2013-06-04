package nl.sense.demo;

import nl.sense_os.platform.SensePlatform;
import nl.sense_os.service.SenseServiceStub;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.Ambience;
import nl.sense_os.service.constants.SensePrefs.Main.DevProx;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * Main activity of the Sense Platform Demo. This activity is created to demonstrate the most
 * important use cases of the Sense Platform library in Android. The goal is to provide useful code
 * snippets that you can use in your own Android project.<br/>
 * <br/>
 * The activity has a trivial UI, but automatically performs the following tasks when it is started.
 * <ul>
 * <li>Create a {@link SensePlatform} instance for communication with the Sense service.</li>
 * <li>Log in as user "foo".</li>
 * <li>Set some sensing preferences, e.g. sample rate and sync rate.</li>
 * <li>Start a couple of sensor modules.</li>
 * <li>Send data for a non-standard sensor: "position_annotation".</li>
 * <li>Get data from a certain sensor.</li>
 * </ul>
 * This class implements the {@link ServiceConnection} interface so it can receive callbacks from
 * the SensePlatform object.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 * @author Pim Nijdam <pim@sense-os.nl>
 */
public class MainActivity extends Activity {

    private static final String TAG = "Sense Demo";
    private static final String DEMO_SENSOR_NAME = "demo";
    private SenseApplication application;

    private void flushData() {
        Log.v(TAG, "Flush buffers");
        application.getSensePlatform().flushData();
        showToast(R.string.msg_flush_data);
    }

    private void getLocalData() {
        Log.v(TAG, "Get data from CommonSense");

        // start new Thread to prevent NetworkOnMainThreadException
        new Thread() {
            public void run() {

                JSONArray data;
                try {
                    data = application.getSensePlatform().getLocalData(DEMO_SENSOR_NAME, 10);

                    // show message
                    showToast(R.string.msg_query_local, data.length());

                } catch (IllegalStateException e) {
                    Log.w(TAG, "Failed to query remote data", e);
                    showToast(R.string.msg_error, e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG, "Failed to parse remote data", e);
                    showToast(R.string.msg_error, e.getMessage());
                }
            };
        }.start();
    }

    /**
     * An example how to get data from a sensor
     */
    private void getRemoteData() {
        Log.v(TAG, "Get data from CommonSense");

        // start new Thread to prevent NetworkOnMainThreadException
        new Thread() {
            public void run() {

                JSONArray data;
                try {
                    data = application.getSensePlatform().getData(DEMO_SENSOR_NAME, true, 10);

                    // show message
                    showToast(R.string.msg_query_remote, data.length());

                } catch (IllegalStateException e) {
                    Log.w(TAG, "Failed to query remote data", e);
                    showToast(R.string.msg_error, e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG, "Failed to parse remote data", e);
                    showToast(R.string.msg_error, e.getMessage());
                }
            };
        }.start();
    }

    /**
     * Handles clicks on the UI
     * 
     * @param v
     */
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.buttonLogin:
            startActivity(new Intent(this, LoginActivity.class));
            break;
        case R.id.buttonPrefs:
            setPreferences();
            break;
        case R.id.buttonStart:
            startSense();
            break;
        case R.id.buttonStop:
            stopSense();
            break;
        case R.id.buttonDataPoint:
            insertData();
            break;
        case R.id.buttonFlush:
            flushData();
            break;
        case R.id.buttonLocalData:
            getLocalData();
            break;
        case R.id.buttonRemoteData:
            getRemoteData();
            break;
        default:
            Log.w(TAG, "Unexpected button pressed: " + v);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // the activity needs to be part of a SenseApplication so it can talk to the SensePlatform
        application = (SenseApplication) getApplication();
    }

    /**
     * An example of how to upload data for a custom sensor.
     */
    private void insertData() {
        Log.v(TAG, "Insert data point");

        // Description of the sensor
        final String name = DEMO_SENSOR_NAME;
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
                application.getSensePlatform().addDataPoint(name, displayName, description,
                        dataType, value, timestamp);
                
                NerdsData realtimeData = NerdsData.getInstance(application.getSensePlatform());
                Log.v(TAG, "group audio: "+realtimeData.getGroupAudioVolumeData());
                Log.v(TAG, "group motion: "+realtimeData.getGroupMotionData());
                Log.v(TAG, "group heatmap: "+realtimeData.getGroupPositionHeatmap());
                Log.v(TAG, "group sit/stand: "+realtimeData.getGroupSitStandData());
                Log.v(TAG, "group steps: "+realtimeData.getGroupStepsData());
                Log.v(TAG, "my audio: "+realtimeData.getMyAudioVolumeData());
                Log.v(TAG, "my motion: "+realtimeData.getMyMotionData());
                Log.v(TAG, "my heatmap: "+realtimeData.getMyPositionHeatmap());
                Log.v(TAG, "my sit/stand: "+realtimeData.getMySitStandData());
                Log.v(TAG, "my steps: "+realtimeData.getMyStepsData());
                Log.v(TAG, "rt motion: "+realtimeData.getRTMotion());
                Log.v(TAG, "rt audio: "+realtimeData.getRTAudioVolume());
                Log.v(TAG, "rt position: "+realtimeData.getRTIndoorPosition());
            }
        }.start();

        // show message
        showToast(R.string.msg_sent_data, name);
    }

    /**
     * Sets up the Sense service preferences
     */
    private void setPreferences() {
        Log.v(TAG, "Set preferences");

        SenseServiceStub senseService = application.getSensePlatform().getService();

        // turn off some specific sensors
        senseService.setPrefBool(Ambience.LIGHT, true);
        senseService.setPrefBool(Ambience.CAMERA_LIGHT, false);
        senseService.setPrefBool(Ambience.PRESSURE, false);

        // turn on specific sensors
        senseService.setPrefBool(Ambience.MIC, true);
        // NOTE: spectrum might be too heavy for the phone or consume too much energy
        senseService.setPrefBool(Ambience.AUDIO_SPECTRUM, false);
        
        senseService.setPrefBool(DevProx.BLUETOOTH, false);
        senseService.setPrefBool(DevProx.WIFI, true);

        // set how often to sample
        // 1 := rarely (~every 15 min)
        // 0 := normal (~every 5 min)
        // -1 := often (~every 10 sec)
        // -2 := real time (this setting affects power consumption considerably!)
        senseService.setPrefString(SensePrefs.Main.SAMPLE_RATE, "-1");

        // set how often to upload
        // 1 := eco mode (buffer data for 30 minutes before bulk uploading)
        // 0 := normal (buffer 5 min)
        // -1 := often (buffer 1 min)
        // -2 := real time (every new data point is uploaded immediately)
        senseService.setPrefString(SensePrefs.Main.SYNC_RATE, "-2");

        // show message
        showToast(R.string.msg_prefs_set);
    }

    private void startSense() {
        Log.v(TAG, "Start Sense");

        SenseServiceStub senseService = application.getSensePlatform().getService();

        // enable some specific sensor modules
        senseService.togglePhoneState(true);
        senseService.toggleAmbience(true);
        senseService.toggleMotion(true);
        senseService.toggleLocation(false);
        senseService.toggleDeviceProx(true);

        // enable main state
        senseService.toggleMain(true);

		NerdsData.getInstance(application.getSensePlatform()).joinNerdsGroup();
    }

    private void stopSense() {
        Log.v(TAG, "Stop Sense");
        application.getSensePlatform().getService().toggleMain(false);
    }

    private void showToast(final int resId, final Object... formatArgs) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                CharSequence msg = getString(resId, formatArgs);
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
