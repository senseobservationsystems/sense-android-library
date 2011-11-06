package nl.sense_os.phonegap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class ExampleApp extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_choice);
    }

    public void showPhoneGap(View v) {
        startActivity(new Intent(this, PhoneGapExamples.class));
    }

    public void showSenseExamples(View v) {
        startActivity(new Intent(this, SenseExamples.class));
    }
}
