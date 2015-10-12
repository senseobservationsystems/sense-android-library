package nl.sense_os.datastorageengine.test;


import android.test.AndroidTestCase;
import android.util.Log;

public class TestHelloWorld extends AndroidTestCase {
    private static String TAG = "TestHelloWorld";

    public void setUp() throws Exception {
        Log.d(TAG, "setUp");
    }

    public void tearDown() throws Exception {
        Log.d(TAG, "tearDown");
    }

    public void testAdd() {
        Log.d(TAG, "testAdd");
        assertEquals("2 + 2", 4, 2 + 2);
    }

    public void testSubtract() {
        Log.d(TAG, "testSubtract");
        assertEquals("4 - 2", 2, 4 - 2);
    }
}
