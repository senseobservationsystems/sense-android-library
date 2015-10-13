package nl.sense_os.datastorageengine.test;

import android.test.AndroidTestCase;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;

import nl.sense_os.datastorageengine.HTTPUtil;

public class TestHTTPUtil extends AndroidTestCase {
    static String TAG = "TestHTTPUtil";

    public void testRequestGet() throws IOException, RuntimeException, JSONException {
        HTTPUtil.Response response = HTTPUtil.request("get", new URL("http://www.google.nl/"), null, null);

        Log.d(TAG, "response: body.length=" + response.body.length());
        Log.d(TAG, "response: body=" + response.body);

        assertTrue("should return a body", response.body.length() > 0);
    }
}
