package nl.sense_os.phonegap;

import android.os.Bundle;
import android.util.Log;

import com.phonegap.DroidGap;

public class PhoneGapExamples extends DroidGap {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TAG, "Initializing DroidGap...");
        super.init();

        Log.v(TAG, "Loading URL...");
        super.loadUrl("file:///android_asset/www/phonegap.html");
    }
}
