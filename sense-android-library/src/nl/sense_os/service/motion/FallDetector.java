/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.motion;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.BaseDataProducer;
import nl.sense_os.service.shared.DataConsumer;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.shared.SensorDataPoint.DataType;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.SystemClock;
import android.util.FloatMath;
import android.util.Log;

/**
 * Fall detection class is based on the fall detection algorithm proposed on:
 * http://www.ecnmag.com/Articles/2009/12/human-fall-detection/
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 * 
 * @see MotionSensor
 */
public class FallDetector extends BaseDataProducer implements DataConsumer {

	private class Interrupt {
		boolean FREE_FALL = false;
		boolean ACTIVITY = false;
		boolean INACTIVITY = false;
		@SuppressWarnings("unused")
		boolean BASELINE = false;
		boolean FALL = false;
		float stopFreeFall = 0;
		float stopActivity = 0;
		@SuppressWarnings("unused")
		float stopInactivity = 0;
	}

	private static final String TAG = "Fall Detector";

	private Interrupt interrupt;
	private long startInterrupt = 0;
	private float G = 9.81F;
	private float THRESH_FF = 0.6F * G; // Threshold acceleration for a free fall
	private float TIME_FF = 60F; // Time in msec of the free fall
	private float TIME_FF_DEMO = 200F; // Time in msec of the free fall
	private float THRESH_ACT = 2.0F * G; // Threshold for the activity
	private float TIME_FF_ACT = 200F; // Time between a free fall and activity (msec) 200msec was
	// standard 300 for low sampling
	private float THRESH_INACT = 1.3F * G; // Threshold for inactivity, default was 0.1875F*G;
	private float TIME_INACT = 2000F; // Time of inactivity (msec)
	private float TIME_ACT_INACT = 3500; // Time between an activity and inactivity
	@SuppressWarnings("unused")
	private float THRESH_INITIAL = 0.7F * G; // Threshold for the difference between the initial
	// status and after inactivity
	@SuppressWarnings("unused")
	private long time = 0; // Time of last function call
	public boolean demo = true; // For demoing only the free fall is used

	private boolean useInactivity = false; // Use the inactivity property to determine a fall

	private Context context;

	public FallDetector(Context context) {
		this.context = context;
		interrupt = new Interrupt();
	}

	private void activity(float accVecSum) {
		if (interrupt.stopFreeFall == 0)
			return;

		// If the threshold for a activity is reached
		// and if it is within the time frame after a fall
		// then there is activity
		if (accVecSum >= THRESH_ACT) {
			if (SystemClock.elapsedRealtime() - interrupt.stopFreeFall < TIME_FF_ACT) {
				startInterrupt = SystemClock.elapsedRealtime();
				interrupt.ACTIVITY = true;
			}
		}
		// If the activity is over
		// note the stop time of this activity
		else if (interrupt.ACTIVITY) {
			interrupt.stopActivity = SystemClock.elapsedRealtime();
			startInterrupt = 0;
			interrupt.ACTIVITY = false;
		}

		// The time for an activity has passed and there was never an activity interrupt
		// reset;
		if (SystemClock.elapsedRealtime() - interrupt.stopFreeFall > TIME_FF_ACT)
			if (interrupt.stopActivity == 0)
				reset();

		if (interrupt.ACTIVITY) {
			Log.w(TAG, "Activity!!!");
		}
	}

	private boolean fallDetected(float accVecSum) {
		// Log.d("Fall detection:", "time:"+(SystemClock.elapsedRealtime()-time));
		time = SystemClock.elapsedRealtime();

		if (interrupt.FALL || (demo && interrupt.FREE_FALL))
			reset();

		freeFall(accVecSum);

		if (demo) {
			if (interrupt.FREE_FALL) {
				reset();
				return true;
			}
		} else {
			activity(accVecSum);

			if (useInactivity) {
				if (!interrupt.INACTIVITY)
					inactivity(accVecSum);
			} else
				interrupt.FALL = interrupt.ACTIVITY;

			if (interrupt.FALL) {
				reset();
				return true;
			}
		}
		return false;
	}

	private void freeFall(float accVecSum) {
		if (accVecSum < THRESH_FF) {
			if (startInterrupt == 0)
				startInterrupt = SystemClock.elapsedRealtime();
			else if ((SystemClock.elapsedRealtime() - startInterrupt > TIME_FF && !demo)
					|| (SystemClock.elapsedRealtime() - startInterrupt > TIME_FF_DEMO && demo)) {
				// Log.v("Fall detection", "FF time:" + (SystemClock.elapsedRealtime() -
				// startInterrupt));
				interrupt.FREE_FALL = true;
			}
		} else if (interrupt.FREE_FALL) {
			interrupt.stopFreeFall = SystemClock.elapsedRealtime();
			interrupt.FREE_FALL = false;
			startInterrupt = 0;
		} else
			startInterrupt = 0;

		if (interrupt.FREE_FALL) {
			Log.w(TAG, "FALL!!!");
		}
	}

	private void inactivity(float accVecSum) {
		if (interrupt.stopActivity == 0)
			return;

		if (accVecSum < THRESH_INACT) {
			if (SystemClock.elapsedRealtime() - interrupt.stopActivity < TIME_ACT_INACT)
				if (startInterrupt == 0)
					startInterrupt = SystemClock.elapsedRealtime();

			if (startInterrupt != 0 && SystemClock.elapsedRealtime() - startInterrupt > TIME_INACT)
				interrupt.INACTIVITY = true;
		} else if (startInterrupt != 0 && !interrupt.INACTIVITY)
			reset();

		if (SystemClock.elapsedRealtime() - interrupt.stopActivity >= TIME_ACT_INACT
				&& startInterrupt == 0)
			reset();

		interrupt.FALL = interrupt.INACTIVITY;

		if (interrupt.INACTIVITY) {
			Log.w(TAG, "Inactivity!!!");
		}
	}

    @Override
	public boolean isSampleComplete() {
		// never unregister
		return false;
	}

    @Override
	public void onNewData(SensorDataPoint dataPoint) {

		if(dataPoint.getDataType() != DataType.SENSOREVENT)
			return;

		SensorEvent event = dataPoint.getSensorEventValue(); 
		// check if this is useful data point
		Sensor sensor = event.sensor;
		if (sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
			return;
		}

		float aX = event.values[1];
		float aY = event.values[0];
		float aZ = event.values[2];
		float accVecSum = FloatMath.sqrt(aX * aX + aY * aY + aZ * aZ);

		if (fallDetected(accVecSum)) {
			// send msg
			sendFallMessage(true);
		}
	}

	private void reset() {
		interrupt = new Interrupt();
		startInterrupt = 0;
	}

	public void sendFallMessage(boolean fall) {

		this.notifySubscribers();
		SensorDataPoint dataPoint = new SensorDataPoint(fall);
		dataPoint.sensorName = SensorNames.FALL_DETECTOR;
		dataPoint.sensorDescription = demo ? "demo fall" : "human fall";
		dataPoint.timeStamp = SNTP.getInstance().getTime();        
		this.sendToSubscribers(dataPoint);

		//TODO: implement MsgHandler as data processor
		Intent i = new Intent(context.getString(R.string.action_sense_new_data));
		i.putExtra(DataPoint.SENSOR_NAME, SensorNames.FALL_DETECTOR);
		i.putExtra(DataPoint.SENSOR_DESCRIPTION, demo ? "demo fall" : "human fall");
		i.putExtra(DataPoint.VALUE, fall);
		i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.BOOL);
		i.putExtra(DataPoint.TIMESTAMP, dataPoint.timeStamp);
		context.startService(i);
	}

    @Override
	public void startNewSample() {
		// not used
	}
}
