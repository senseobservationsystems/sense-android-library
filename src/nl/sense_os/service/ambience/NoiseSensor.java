/*
 ************************************************************************************************************
 *     Copyright (C)  2010 Sense Observation Systems, Rotterdam, the Netherlands.  All rights reserved.     *
 ************************************************************************************************************
 */
package nl.sense_os.service.ambience;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import nl.sense_os.service.Constants;
import nl.sense_os.service.MsgHandler;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnInfoListener;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class NoiseSensor extends PhoneStateListener {

    /**
     * Calculates the 'noise power' in a sound sample, and passes it to the MsgHandler.
     */
    private class CalcNoiseTask extends AsyncTask<Void, Void, Void> {

        private static final String TAG = "Sense CalcNoiseTask";

        /**
         * @return the noise power of the current buffer. In case of an error, -1 is returned.
         */
        private double calculateDb() {

            double dB = 0;
            try {
                if (!isEnabled) {
                    Log.d(TAG, "Noise sensor is disabled, skipping noise power calculation...");
                    return -1;
                }

                byte[] buffer = new byte[BUFFER_SIZE];
                int readBytes = 0;
                if (audioRecord != null) {
                    readBytes = audioRecord.read(buffer, 0, BUFFER_SIZE);
                }

                if (readBytes < 0) {
                    Log.e(TAG, "Error reading AudioRecord buffer: " + readBytes);
                    return -1;
                }

                for (int x = 0; x < readBytes; x++) {
                    dB += Math.abs(buffer[x]);
                }
                dB /= (double) readBytes;

            } catch (Exception e) {
                Log.e(TAG, "Exception calculating noise Db!", e);
                return -1;
            }

            return dB;
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                // calculate the noise power
                double dB = calculateDb();

                if (dB < 0) {
                    // there was an error calculating the noise power
                } else {
                    Log.i(TAG, "Sampled noise level: " + dB);

                    // pass message to the MsgHandler
                    Intent sensorData = new Intent(MsgHandler.ACTION_NEW_MSG);
                    sensorData.putExtra(MsgHandler.KEY_SENSOR_NAME, NAME_NOISE);
                    sensorData.putExtra(MsgHandler.KEY_VALUE, Double.valueOf(dB).floatValue());
                    sensorData.putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_FLOAT);
                    sensorData.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
                    NoiseSensor.this.context.startService(sensorData);
                }
            } finally {
                // stop audio recording
                if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {

                    try {
                        audioRecord.stop();
                        Log.i(TAG, "Stopped recording sound...");
                    } catch (IllegalStateException e) {
                        // audioRecord is probably already stopped..?
                    }
                    audioRecord.release();
                    audioRecord = null;
                }
            }

            return null;
        }

        @Override
        protected void onCancelled() {
            // nothing to do
            // Log.d(TAG, "Cancelled...");
        }

        @Override
        protected void onPostExecute(Void result) {
            // reschedule listen thread
            if (isEnabled) {

                TimerTask task = new TimerTask() {

                    @Override
                    public void run() {

                        if (false == isEnabled) {
                            // Log.d(TAG, "Did not start noise record task: sensor is disabled...");
                            return;
                        }

                        if (recNoiseTask == null
                                || recNoiseTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
                            recNoiseTask = new RecNoiseTask();
                            recNoiseTask.execute();
                        } else {
                            // Log.d(TAG, "Didn't start noise record: task is already active...");
                        }
                    }
                };
                new Timer().schedule(task, listenInterval);
            }
        }
    }

    /**
     * Initializes the AudioRecord, starts recording the ambient noise and schedules the
     * CalcNoiseTask to read the recorded buffer.
     */
    private class RecNoiseTask extends AsyncTask<Void, Void, Boolean> {

        private static final String TAG = "Sense RecNoiseTask";

        @Override
        protected Boolean doInBackground(Void... params) {

            boolean isRecording = false;
            if (isEnabled) {

                boolean init = initAudioRecord();

                if (init) {
                    Log.i(TAG, "Start recording sound...");
                    try {
                        audioRecord.startRecording();
                        isRecording = true;
                    } catch (Exception e) {
                        Log.e(TAG, "Exception starting recording!", e);
                        isRecording = false;
                    }

                } else {
                    Log.w(TAG, "Did not start recording: AudioRecord could not be initialized!");
                    isRecording = false;
                }
            } else {
                // Log.d(TAG, "Did not start recording: noise sensor is disabled...");
                isRecording = false;
            }

            return isRecording;
        }

        @Override
        protected void onCancelled() {

            // Log.d(TAG, "Cancelled...");

            if (null != audioRecord) {
                try {
                    audioRecord.stop();
                } catch (IllegalStateException e) {
                    // ignore exception: probably was not recording
                } finally {
                    audioRecord.release();
                    audioRecord = null;
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean isRecording) {

            if (null != isRecording && isRecording) {
                // schedule task to stop recording and calculate the noise
                TimerTask task = new TimerTask() {

                    @Override
                    public void run() {

                        if (false == isEnabled) {
                            // Log.d(TAG, "Did not start noise calc task: sensor is disabled...");
                            return;
                        }

                        if (null == calcNoiseTask
                                || calcNoiseTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
                            calcNoiseTask = new CalcNoiseTask();
                            calcNoiseTask.execute();
                        } else {
                            // Log.d(TAG, "Did not start noise calc task: it is already active...");
                        }
                    }
                };
                new Timer().schedule(task, RECORDING_TIME_NOISE);
            }
        }
    }

    private class SoundStreamThread implements Runnable {

        @Override
        public void run() {

            try {
                // cameraDevice = android.hardware.Camera.open();
                // Parameters params = cameraDevice.getParameters();
                // String effect = "mono";
                // params.set("effect", effect);
                // cameraDevice.setParameters(params);
                // recorder.setCamera(cameraDevice);
                if (isCalling) {
                    recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_UPLINK);
                } else {
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                }
                // recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                // recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
                final String fileName = recordFileName + fileCounter + ".3gp";
                fileCounter = (++fileCounter) % MAX_FILES;
                new File(recordFileName).createNewFile();
                String command = "chmod 666 " + fileName;
                Runtime.getRuntime().exec(command);
                recorder.setOutputFile(fileName);
                recorder.setMaxDuration(RECORDING_TIME_STREAM);
                recorder.setOnInfoListener(new OnInfoListener() {

                    @Override
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
                                i.putExtra(MsgHandler.KEY_VALUE, fileName);
                                i.putExtra(MsgHandler.KEY_DATA_TYPE,
                                        Constants.SENSOR_DATA_TYPE_FILE);
                                i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
                                NoiseSensor.this.context.startService(i);

                                if (isEnabled && listenInterval == -1
                                        && tmp.equals(soundStreamThread)) {
                                    soundStreamThread = new SoundStreamThread();
                                    soundStreamHandler.post(soundStreamThread);
                                }

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

    private static final String TAG = "Sense NoiseSensor";
    private static final String NAME_NOISE = "noise_sensor";
    private static final String NAME_MIC = "microphone";
    private static final int MAX_FILES = 60;
    private static final int DEFAULT_SAMPLE_RATE = 8000;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(DEFAULT_SAMPLE_RATE,
            AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT) * 10;
    private static final int RECORDING_TIME_NOISE = 2000;
    private static final int RECORDING_TIME_STREAM = 60000;
    private AudioRecord audioRecord;
    private boolean isEnabled = false;
    private boolean isCalling = false;
    private int listenInterval; // Update interval in msec
    private Context context;
    private RecNoiseTask recNoiseTask = null;
    private CalcNoiseTask calcNoiseTask = null;
    private SoundStreamThread soundStreamThread = null;
    private Handler soundStreamHandler = new Handler(Looper.getMainLooper());
    private MediaRecorder recorder = null;
    private int fileCounter = 0;
    private String recordFileName = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/sense/micSample";
    private TelephonyManager telMgr;

    public NoiseSensor(Context context) {
        this.context = context;
        this.telMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    /**
     * Disables the noise sensor, stopping the sound recording and unregistering it as phone state
     * listener.
     */
    public void disable() {
        // Log.v(TAG, "Noise sensor disabled...");

        isEnabled = false;
        pauseListening();

        this.telMgr.listen(this, PhoneStateListener.LISTEN_NONE);
    }

    /**
     * Enables the noise sensor, starting the sound recording and registering it as phone state
     * listener.
     */
    public void enable(int interval) {
        // Log.v(TAG, "Noise sensor enabled...");

        listenInterval = interval;
        isEnabled = true;
        // startListening(); // listening is started in onCallStateChanged

        this.telMgr.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * @return <code>true</code> if {@link #audioRecord} was initialized successfully
     */
    private boolean initAudioRecord() {
        // Log.d(TAG, "Initializing AudioRecord instance...");

        if (null != audioRecord) {
            Log.w(TAG, "AudioRecord object is already present! Releasing it...");
            // release the audioRecord object and stop any recordings that are running
            pauseListening();
        }

        // create the AudioRecord
        if (isCalling) {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_UPLINK,
                    DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        } else {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, DEFAULT_SAMPLE_RATE,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    BUFFER_SIZE);
        }

        if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            Log.w(TAG, "Failed to create AudioRecord!");
            Log.w(TAG,
                    "format: " + audioRecord.getAudioFormat() + " source: "
                            + audioRecord.getAudioSource() + " channel: "
                            + audioRecord.getChannelConfiguration() + " buffer size: "
                            + BUFFER_SIZE);
            return false;
        }

        // initialized OK
        return true;
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {

        // Log.v(TAG, "Call state changed...");

        try {
            if (state == TelephonyManager.CALL_STATE_OFFHOOK
                    || state == TelephonyManager.CALL_STATE_RINGING) {
                isCalling = true;
            } else {
                isCalling = false;
            }

            pauseListening();

            // recording while not calling is disabled
            if (isEnabled && state == TelephonyManager.CALL_STATE_IDLE) {
                startListening();
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in onCallStateChanged!", e);
        }
    }

    private void pauseListening() {

        // Log.v(TAG, "Pause listening for noise level...");

        try {
            // clear any old noise sensing threads
            if (recNoiseTask != null) {
                try {
                    // Log.d(TAG, "Cancel RecNoiseTask...");
                    recNoiseTask.cancel(true);
                } finally {
                    recNoiseTask = null;
                }
            }
            if (calcNoiseTask != null) {
                try {
                    // Log.d(TAG, "Cancel CalcNoiseTask...");
                    calcNoiseTask.cancel(true);
                } finally {
                    calcNoiseTask = null;
                }
            }
            if (soundStreamThread != null) {
                soundStreamHandler.removeCallbacks(soundStreamThread);
                soundStreamThread = null;
            }

            if (audioRecord != null) {
                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.stop();
                }
                audioRecord.release();
                audioRecord = null;
            }

            if (listenInterval == -1 && recorder != null) {
                recorder.stop();
                recorder.reset(); // You can reuse the object by going back to setAudioSource() step
                // recorder.release(); // Now the object cannot be reused
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in pauseListening!", e);
        }
    }

    private void startListening() {
        // Log.v(TAG, "Start listening for the noise level...");

        try {
            if (listenInterval == -1) {
                // create dir
                (new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/sense"))
                        .mkdir();
                recorder = new MediaRecorder();
                if (soundStreamThread != null) {
                    soundStreamHandler.removeCallbacks(soundStreamThread);
                }
                soundStreamThread = new SoundStreamThread();
                soundStreamHandler.post(soundStreamThread);
            } else {

                // beware of any old noise sensing threads
                if (recNoiseTask == null
                        || recNoiseTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
                    recNoiseTask = new RecNoiseTask();
                    recNoiseTask.execute();
                } else {
                    // Log.d(TAG, "Did not start noise record task: it is already active...");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception in startListening:" + e.getMessage());
        }
    }
}
