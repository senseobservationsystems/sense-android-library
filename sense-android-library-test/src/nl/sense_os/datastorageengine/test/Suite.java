package nl.sense_os.datastorageengine.test;

import android.util.Log;

import junit.framework.Test;
import junit.framework.TestSuite;

public class Suite extends TestSuite {
    private static String TAG = "Suite";

    public static Test suite() {
        return new TestSuite(TestHelloWorld.class);
//        return new TestSuite(TestHelloWorld.class, Test2.class, Test3.class, ...);
    }

    public void setUp() throws Exception {
        Log.d(TAG, "Global setUp");
        System.out.println(" Global setUp ");
    }

    public void tearDown() throws Exception {
        Log.d(TAG, "Global tearDown");
    }
}