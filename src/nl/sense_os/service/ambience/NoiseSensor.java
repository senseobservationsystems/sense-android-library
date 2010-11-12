/*
 ************************************************************************************************************
 *     Copyright (C)  2010 Sense Observation Systems, Rotterdam, the Netherlands.  All rights reserved.     *
 ************************************************************************************************************
 */
package nl.sense_os.service.ambience;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Environment;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import nl.sense_os.app.SenseSettings;
import nl.sense_os.service.MsgHandler;

import java.io.File;

public class NoiseSensor extends PhoneStateListener {
	private class NoiseSensorThread implements Runnable {

		protected class CalculateNoiseThread implements Runnable {
			public CalculateNoiseThread() {
			}

			private double calculateDB() {
				double dB = 0;
				try {
					if (!isListening)
						return 0;
					byte[] buffer = new byte[bufferSize];
					int readBytes = 0;
					if (audioRec != null)
						readBytes = audioRec.read(buffer, 0, bufferSize);

					for (int x = 0; x < readBytes; x++)
						dB += Math.abs(buffer[x]);
					dB /= (double) buffer.length;
					// Log.d(TAG, "buffer length " + buffer.length + " buffer size: " + bufferSize +
							// " dB: " + dB);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return dB;
			}

			public void run() {
				double dB = calculateDB();				
				// pass message to the MsgHandler
				Intent i = new Intent(MsgHandler.ACTION_NEW_MSG);
				i.putExtra(MsgHandler.KEY_SENSOR_NAME, NAME_NOISE);
				i.putExtra(MsgHandler.KEY_VALUE, Double.valueOf(dB).floatValue());
				i.putExtra(MsgHandler.KEY_DATA_TYPE, SenseSettings.SENSOR_DATA_TYPE_FLOAT);
				i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
				NoiseSensor.this.context.startService(i);

				// stop audio recording
				if (audioRec != null && audioRec.getState() == AudioRecord.STATE_INITIALIZED) {
					audioRec.stop();
				}

				// reschedule listen thread
				if (isListening) {
					noiseThreadHandler.postDelayed(noiseThread = new NoiseSensorThread(),
							listenInterval);
				}
			}
		}

		public NoiseSensorThread() {
		}

		public void run() {
			if (audioRec == null) {
				Log.d(TAG, "AudioRec is null.");
				return;
			}
			Log.d(TAG, "Start NoiseSensorThread...");

			if (isListening) {
				if (audioRec.getState() != AudioRecord.STATE_INITIALIZED) {
					// Strange... sometimes the audioRec is not initialized when it is automatically
					// started when the service restarts...
					startListening(listenInterval);
				} else {
					audioRec.startRecording();
					calculateNoiseHandler.postDelayed(
							calculateNoiseThread = new CalculateNoiseThread(), sampleTime);
				}
			}
		}
	}

	class SoundStreamThread implements Runnable {
		public void run() {

			try {
				// cameraDevice = android.hardware.Camera.open();
				// Parameters params = cameraDevice.getParameters();
				// String effect = "mono";
				// params.set("effect", effect);
				// cameraDevice.setParameters(params);
				// recorder.setCamera(cameraDevice);
				if (isCalling)
					recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_UPLINK);
				else
					recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
				// recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
				recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
				// recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
				recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);

				new File(recordFileName).createNewFile();
				String command = "chmod 666 " + recordFileName;
				Runtime.getRuntime().exec(command);
				recorder.setOutputFile(recordFileName);
				recorder.setMaxDuration(sampleTimeStream);
				recorder.setOnInfoListener(new OnInfoListener() {

					public void onInfo(MediaRecorder mr, int what, int extra) {

						if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
							try {

								// recording is done, upload file
								recorder.stop();
								recorder.reset();
								// wait until finished otherwise it will be overwritten
								SoundStreamThread tmp = soundStreamThread;

								// pass message to the MsgHandler
								Intent i = new Intent(MsgHandler.ACTION_NEW_MSG);
								i.putExtra(MsgHandler.KEY_SENSOR_NAME, NAME_MIC);
								i.putExtra(MsgHandler.KEY_VALUE, recordFileName);
								i.putExtra(MsgHandler.KEY_DATA_TYPE,
										SenseSettings.SENSOR_DATA_TYPE_STRING);
								i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
								NoiseSensor.this.context.startService(i);

								if (isListening && listenInterval == -1
										&& tmp.equals(soundStreamThread))
									soundStreamHandler
									.post(soundStreamThread = new SoundStreamThread());

							} catch (Exception e) {
								e.printStackTrace();
							}
						}

					}
				});

				recorder.prepare();
				recorder.start();

			} catch (final Exception e) {
				Log.d(TAG, "Error while recording sound:", e);
			}
		}
	}

	private static final int DEFAULT_SAMPLE_RATE = 8000;
	private static final String TAG = "Sense NoiseSensor";
	private static final String NAME_NOISE = "noise_sensor";
	private static final String NAME_MIC = "microphone";
	private AudioRecord audioRec;
	private int bufferSize = 4096;
	private Handler calculateNoiseHandler = new Handler();
	private boolean listeningEnabled = false;
	private boolean isListening = false;
	private int listenInterval; // Update interval in msec
	private NoiseSensorThread noiseThread = null;
	private NoiseSensorThread.CalculateNoiseThread calculateNoiseThread = null;
	private Context context;
	private SoundStreamThread soundStreamThread = null;
	private Handler noiseThreadHandler = new Handler();
	private Handler soundStreamHandler = new Handler();
	private MediaRecorder recorder = null;

	private String recordFileName = Environment.getExternalStorageDirectory().getAbsolutePath()
	+ "/micSample.3gp";
	private boolean isCalling = false;
	// private Camera cameraDevice = null;
	int sampleTime = 2000;
	int sampleTimeStream = 10000;

	public NoiseSensor(Context context) {
		this.context = context;
	}

	public int getSampleTime() {
		return sampleTime;
	};

	@Override
	public void onCallStateChanged(int state, String incomingNumber) {

		try
		{
			if (state == TelephonyManager.CALL_STATE_OFFHOOK
					|| state == TelephonyManager.CALL_STATE_RINGING) {
				isCalling = true;
			} else {
				isCalling = false;
			}

			pauzeListening();

			// recording while not calling is disabled
			if (!isListening && listeningEnabled && state == TelephonyManager.CALL_STATE_IDLE) {
				startListening(listenInterval);
			}
		}catch(Exception e)
		{
			Log.e(TAG, "Error in onCallStateChanged:"+e.getMessage());
		}
	}

	public void setSampleTime(int sTime) {
		sampleTime = sTime;
	}

	public void startListening(int interval) {
		try{
		listenInterval = interval;
		listeningEnabled = true;
		isListening = true;

		Thread t = new Thread() {
			public void run() {
				if (listenInterval == -1) {
					recorder = new MediaRecorder();
					if (soundStreamThread != null) {
						soundStreamHandler.removeCallbacks(soundStreamThread);
					}
					soundStreamThread = new SoundStreamThread();
					soundStreamHandler.post(soundStreamThread);
				} else {
					bufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLE_RATE,
							AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
					if (isCalling) {
						audioRec = new AudioRecord(MediaRecorder.AudioSource.VOICE_UPLINK,
								DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
								AudioFormat.ENCODING_PCM_16BIT, bufferSize);
					} else {
						audioRec = new AudioRecord(MediaRecorder.AudioSource.MIC,
								DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
								AudioFormat.ENCODING_PCM_16BIT, bufferSize);
					}
					if (audioRec.getState() == AudioRecord.STATE_UNINITIALIZED) {
						Log.d(TAG,
								"Uninitialized AudioRecord format: " + audioRec.getAudioFormat()
								+ " source: " + audioRec.getAudioSource() + " channel:"
								+ audioRec.getChannelConfiguration() + " buffersize:"
								+ bufferSize);
						return;
					}

					// clear any old noise sensing threads
					if (noiseThread != null) {
						noiseThreadHandler.removeCallbacks(noiseThread);
					}
					noiseThread = new NoiseSensorThread();
					noiseThreadHandler.postDelayed(noiseThread, listenInterval);
				}
			}
		};
		t.start();
		}catch(Exception e)
		{
			Log.e(TAG, "Error in startListening:"+e.getMessage());
		}
	}

	private void pauzeListening() {
		try {
			isListening = false;
			// clear any old noise sensing threads
			if (noiseThread != null) {
				noiseThreadHandler.removeCallbacks(noiseThread);
				noiseThread = null;
			}
			if (soundStreamThread != null) {
				soundStreamHandler.removeCallbacks(soundStreamThread);
				soundStreamThread = null;
			}
			if (calculateNoiseThread != null) {
				calculateNoiseHandler.removeCallbacks(calculateNoiseThread);
				calculateNoiseThread = null;
			}

			if (audioRec != null) {
				if (audioRec.getState() == AudioRecord.STATE_INITIALIZED) {
					audioRec.stop();
				}
				audioRec.release();
				audioRec = null;
			}

			if (listenInterval == -1 && recorder != null) {
				recorder.stop();
				recorder.reset(); // You can reuse the object by going back to setAudioSource() step
				// recorder.release(); // Now the object cannot be reused
			}
		} catch (Exception e) {
			Log.e(TAG, "Error in pauzeListening: "+ e.getMessage());
		}
	}

	public void stopListening() {
		listeningEnabled = false;
		isListening = false;
		pauzeListening();
	}
}
