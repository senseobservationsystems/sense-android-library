package nl.sense.demo;

import nl.sense_os.platform.SensePlatform;
import nl.sense_os.service.SenseServiceStub;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.Ambience;
import nl.sense_os.service.constants.SensePrefs.Main.Location;

import org.json.JSONArray;

import android.app.Activity;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

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
    private SenseApplication application;

    /**
     * An example how to get data from a sensor
     */
    private void getData() {
        try {
            JSONArray data = application.getSensePlatform().getData("position", true, 10);
            Log.v(TAG, "Received: '" + data + "'");

        } catch (Exception e) {
            Log.e(TAG, "Failed to get data!", e);
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
    private void sendData() {
        // Description of the sensor
        String name = "position_annotation";
        String displayName = "Annotation";
        String dataType = "json";
        String description = name;
        // the value to be sent, in json format
        String value = "{\"latitude\":\"51.903469\",\"longitude\":\"4.459865\",\"comment\":\"What a nice quiet place!\"}"; // json
                                                                                                                           // value
        long timestamp = System.currentTimeMillis();
        try {
            application.getSensePlatform().addDataPoint(name, displayName, description, dataType,
                    value, timestamp);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add data point!", e);
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.buttonLogin:
            startActivity(new Intent(this, LoginActivity.class));
            break;
        case R.id.buttonDataPoint:
            sendData();
            break;
        case R.id.buttonLocalData:
            // TODO
            break;
        case R.id.buttonRemoteData:
            getData();
            break;
        default:
            Log.w(TAG, "Unexpected button pressed: " + v);
        }
    }

    /**
     * Sets up the Sense service preferences and logs in
     */
    private void setupSense() {
        try {

            // turn off some specific sensors
            SenseServiceStub service = application.getSensePlatform().getService();
            service.setPrefBool(Ambience.LIGHT, false);
            service.setPrefBool(Ambience.CAMERA_LIGHT, false);
            service.setPrefBool(Ambience.PRESSURE, false);

            // turn on specific sensors
            service.setPrefBool(Ambience.MIC, true);
            // NOTE: spectrum might be too heavy for the phone or consume too much energy
            service.setPrefBool(Ambience.AUDIO_SPECTRUM, true);
            service.setPrefBool(Location.GPS, true);
            service.setPrefBool(Location.NETWORK, true);
            service.setPrefBool(Location.AUTO_GPS, true);

            // set how often to sample
            // 1 := rarely (~every 15 min)
            // 0 := normal (~every 5 min)
            // -1 := often (~every 10 sec)
            // -2 := real time (this setting affects power consumption considerably!)
            service.setPrefString(SensePrefs.Main.SAMPLE_RATE, "0");

            // set how often to upload
            // 1 := eco mode (buffer data for 30 minutes before bulk uploading)
            // 0 := normal (buffer 5 min)
            // -1 := often (buffer 1 min)
            // -2 := real time (every new data point is uploaded immediately)
            service.setPrefString(SensePrefs.Main.SYNC_RATE, "-2");

        } catch (Exception e) {
            Log.e(TAG, "Exception while setting up Sense library.", e);
        }
    }
}
