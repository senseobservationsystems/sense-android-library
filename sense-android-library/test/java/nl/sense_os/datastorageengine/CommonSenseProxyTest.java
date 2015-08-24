package nl.sense_os.datastorageengine;

import android.util.Log;

import org.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.io.IOException;

import nl.sense_os.service.BuildConfig;

/**
 * {@link CommonSenseProxyTest} tests whether {@link CommonSenseProxy} covers the full CommonSense API
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class CommonSenseProxyTest {

    private static final String TAG = "CommonSenseProxyTest";

    /** TODO move to common/suite extension of {@link RobolectricTestRunner} */
    @BeforeClass
    public static void setupLogging() {
        ShadowLog.stream = System.out;
    }

    @Test(expected=IOException.class)
    public void doLoginTest() throws IOException, JSONException {
        Log.d(TAG, "Started doLoginTest()");
        final String appKey = "myAppKey", userName = "myUser", userPass = "myPass";

        final CommonSenseProxy cs = new CommonSenseProxy(false, appKey);
        Log.d(TAG, "Created proxy for application key: "+appKey);

        final String sessionID = cs.loginUser(userName, userPass);
        Log.d(TAG, "Logged in as "+userName+" with session ID: "+sessionID);

        cs.logoutCurrentUser(sessionID);
        Log.d(TAG, "Logged out from session ID: " + sessionID);

        Log.d(TAG, "Completed doLoginTest()");
    }

}
