/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.ambience;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import nl.sense_os.service.MsgHandler;
import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;

import java.io.File;
import java.math.BigDecimal;

public class NoiseSensor extends PhoneStateListener {

    /**
     * Receiver for periodic alarm broadcast that wakes up the device and starts a noise
     * measurement.
     */
    private class AlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            // clear old sample jobs
            if (noiseSampleJob != null) {
                noiseSampleJob.stopRecording();
                noiseSampleHandler.removeCallbacks(noiseSampleJob);
            }

            // start sample job
            if (isEnabled && listenInterval != -1) {
                noiseSampleJob = new NoiseSampleJob();
                noiseSampleHandler.post(noiseSampleJob);
            }
        }
    }

    /**
     * Runnable that performs one noise sample. Starts the recording, reads the buffer contents,
     * calculates the noise power and sends the measurement to the {@link MsgHandler}. Also
     * schedules the next sample job.
     */
    private class NoiseSampleJob implements Runnable {

        private static final int DEFAULT_SAMPLE_RATE = 8000;
        /*
         * samples per second * 2 seconds, 2 bytes
         */
        private static final int BUFFER_SIZE = DEFAULT_SAMPLE_RATE * 2 * 2;
        private static final int RECORDING_TIME_NOISE = 2000;
        private AudioRecord audioRecord;

        /**
         * @param buffer
         *            The sound bytes to calculate the power for.
         * @param readBytes
         *            The number of usable bytes in the buffer.
         * @return the noise power of the current buffer. In case of an error, -1 is returned.
         */
        private double calculateDb(byte[] buffer, int readBytes) {

            double dB = 0;
            try {
                if (!isEnabled) {
                    Log.w(TAG, "Noise sensor is disabled, skipping noise power calculation...");
                    return -1;
                }

                if (readBytes <= 0) {
                    Log.e(TAG, "Error reading AudioRecord buffer: " + readBytes);
                    return -1;
                }
                double ldb = 0;
                for (int x = 0; x < readBytes - 1; x = x + 2) {
                    // it looks like little endian
                    double val = buffer[x + 1] << 8 | buffer[x];
                    ldb += val * val;
                    // dB += Math.abs(buffer[x]);
                }

                ldb /= (double) readBytes / 2;
                dB = 20.0 * Math.log10(Math.sqrt(ldb));

            } catch (Exception e) {
                Log.e(TAG, "Exception calculating noise Db!", e);
                return -1;
            }

            return dB;
        }

        /**
         * @return <code>true</code> if {@link #audioRecord} was initialized successfully
         */
        private boolean initAudioRecord() {
            Log.v(TAG, "Initializing AudioRecord instance...");

            if (null != audioRecord) {
                Log.w(TAG, "AudioRecord object is already present! Releasing it...");
                // release the audioRecord object and stop any recordings that are running
                stopSampling();
            }

            // create the AudioRecord
            try {
                int audioSource = -1, channelConfig = -1;
                if (isCalling) {
                    if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
                        audioSource = MediaRecorder.AudioSource.VOICE_UPLINK;
                        channelConfig = AudioFormat.CHANNEL_CONFIGURATION_DEFAULT;
                    } else {
                        audioSource = MediaRecorder.AudioSource.VOICE_UPLINK;
                        channelConfig = AudioFormat.CHANNEL_IN_DEFAULT;
                    }
                } else {
                    if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
                        audioSource = MediaRecorder.AudioSource.MIC;
                        channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
                    } else {
                        audioSource = MediaRecorder.AudioSource.MIC;
                        channelConfig = AudioFormat.CHANNEL_IN_MONO;
                    }
                }
                audioRecord = new AudioRecord(audioSource, DEFAULT_SAMPLE_RATE, channelConfig,
                        AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Failed to create the audiorecord!", e);
                return false;
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
        public void run() {

            if (isEnabled && !isCalling) {

                boolean init = initAudioRecord();

                if (init) {

                    try {
                        Log.i(TAG, "Start recording for sound level measurement...");
                        audioRecord.startRecording();

                        // schedule task to stop recording and calculate the noise
                        long now = System.currentTimeMillis();
                        byte[] totalBuffer = new byte[BUFFER_SIZE];
                        int readCount = 0;
                        while (audioRecord != null
                                && System.currentTimeMillis() < now + RECORDING_TIME_NOISE) {
                            int chunkSize = Math.min(256, totalBuffer.length - readCount);
                            int readResult = audioRecord.read(totalBuffer, readCount, chunkSize);
                            if (readResult < 0) {
                                Log.e(TAG, "Error reading AudioRecord: " + readResult);
                                readCount = readResult;
                                break;
                            } else {
                                // Log.v(TAG, "Read " + readResult + " bytes");
                                readCount += readResult;
                                if (readCount >= totalBuffer.length) {
                                    // Log.v(TAG, "Buffer overflow");
                                    break;
                                }
                            }
                        }

                        double dB = calculateDb(totalBuffer, readCount);

                        if (dB < 0 || Double.valueOf(dB).isNaN()) {
                            // there was an error calculating the noise power
                            Log.w(TAG, "Impossible noise value: " + dB + ". No new data point.");

                        } else {
                            // Log.v(TAG, "Sampled noise level: " + dB);

                            // pass message to the MsgHandler
                            Intent sensorData = new Intent(
                                    context.getString(R.string.action_sense_new_data));
                            sensorData.putExtra(DataPoint.SENSOR_NAME, SensorNames.NOISE);
                            sensorData.putExtra(DataPoint.VALUE,
                                    BigDecimal.valueOf(dB).setScale(2, 0).floatValue());
                            sensorData.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
                            sensorData.putExtra(DataPoint.TIMESTAMP, System.currentTimeMillis());
                            context.startService(sensorData);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Exception starting noise recording!", e);
                    } finally {
                        stopRecording();
                    }

                } else {
                    Log.w(TAG, "Did not start recording: AudioRecord could not be initialized!");
                }

            } else {
                // Log.v(TAG, "Did not start recording: noise sensor is disabled...");
            }
        }

        /**
         * Stops the recording and releases the AudioRecord object, making it unusable.
         */
        public void stopRecording() {

            try {
                if (audioRecord != null) {
                    if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {

                        try {
                            audioRecord.stop();
                            Log.i(TAG, "Stopped recording for sound level measurement...");
                        } catch (IllegalStateException e) {
                            // audioRecord is probably already stopped..?
                        }
                    }
                    audioRecord.release();
                    audioRecord = null;

                }
            } catch (Exception e) {
                Log.e(TAG, "Exception while stopping noise sample recording", e);
            }
        }
    }

    /**
     * Runnable that starts one sound stream recording. Afterwards, the recording is sent to the
     * {@link MsgHandler}. Also schedules the next sample job.
     */
    private class SoundStreamJob implements Runnable {

        private static final int MAX_FILES = 60;
        private static final int RECORDING_TIME_STREAM = 60000;
        private MediaRecorder recorder = null;
        private String recordFileName = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/sense/micSample";
        private int fileCounter;

        public SoundStreamJob(int fileCounter) {
            this.fileCounter = fileCounter;

            // create directory to put the sound recording
            new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/sense")
                    .mkdir();
            recorder = new MediaRecorder();
        }

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
                                SoundStreamJob tmp = soundStreamJob;

                                // pass message to the MsgHandler
                                Intent i = new Intent(context
                                        .getString(R.string.action_sense_new_data));
                                i.putExtra(DataPoint.SENSOR_NAME, SensorNames.MIC);
                                i.putExtra(DataPoint.VALUE, fileName);
                                i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FILE);
                                i.putExtra(DataPoint.TIMESTAMP, System.currentTimeMillis());
                                context.startService(i);

                                if (isEnabled && listenInterval == -1 && tmp.equals(soundStreamJob)) {
                                    fileCounter = ++fileCounter % MAX_FILES;
                                    soundStreamJob = new SoundStreamJob(fileCounter);
                                    soundStreamHandler.post(soundStreamJob);
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

        /**
         * Stops the recording and releases the MediaRecorder object, making it unusable.
         */
        public void stopRecording() {

            // clean up the MediaRecorder if the mic sensor was using it
            if (recorder != null) {
                try {
                    recorder.stop();
                } catch (IllegalStateException e) {
                    // probably already stopped
                }

                // if we reset, we can reuse the object by going back to setAudioSource() step
                recorder.reset();

                // if we release instead of reset, the object cannot be reused
                // recorder.release();
                // recorder = null;
            }
        }
    }

    private static final String TAG = "Sense NoiseSensor";
    private static final int REQID = 0xF00;
    private static final String ACTION_NOISE = "nl.sense_os.service.NoiseSample";
    private boolean isEnabled = false;
    private boolean isCalling = false;
    private int listenInterval; // Update interval in msec
    private Context context;
    private final Handler soundStreamHandler = new Handler(Looper.getMainLooper());
    private SoundStreamJob soundStreamJob = null;
    private final Handler noiseSampleHandler = new Handler();
    private NoiseSampleJob noiseSampleJob = null;
    private final AlarmReceiver alarmReceiver = new AlarmReceiver();

    public NoiseSensor(Context context) {
        this.context = context;
    }

    /**
     * Disables the noise sensor, stopping the sound recording and unregistering it as phone state
     * listener.
     */
    public void disable() {
        Log.v(TAG, "Noise sensor disabled...");

        isEnabled = false;
        stopSampling();

        TelephonyManager telMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        telMgr.listen(this, PhoneStateListener.LISTEN_NONE);
    }

    /**
     * Enables the noise sensor, starting the sound recording and registering it as phone state
     * listener.
     */
    public void enable(int interval) {
        Log.v(TAG, "Noise sensor enabled...");

        listenInterval = interval;
        isEnabled = true;

        // registering the phone state listener will trigger a call to startListening()
        TelephonyManager telMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        telMgr.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * Pauses sensing when the phone is used for calling, and starts it again after the call.
     */
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

            stopSampling();

            // recording while calling is disabled
            if (isEnabled && state == TelephonyManager.CALL_STATE_IDLE && !isCalling) {
                startSampling();
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in onCallStateChanged!", e);
        }
    }

    /**
     * Stops any active sensing jobs, and stops and cleans up the AudioRecord.
     */
    private void stopSampling() {
        Log.v(TAG, "Stop sound sensor sampling...");

        try {

            // stop the alarms
            AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarms.cancel(PendingIntent.getBroadcast(context, REQID, new Intent(ACTION_NOISE), 0));
            try {
                context.unregisterReceiver(alarmReceiver);
            } catch (IllegalArgumentException e) {
                // ignore
            }

            // stop the sound recordings
            if (soundStreamJob != null) {
                soundStreamJob.stopRecording();
                soundStreamHandler.removeCallbacks(soundStreamJob);
                soundStreamJob = null;
            }
            if (noiseSampleJob != null) {
                noiseSampleJob.stopRecording();
                noiseSampleHandler.removeCallbacks(noiseSampleJob);
                noiseSampleJob = null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception in pauseListening!", e);
        }
    }

    /**
     * Starts the sound sensing jobs.
     */
    private void startSampling() {
        Log.v(TAG, "Start sound sensor sampling...");

        try {

            // different job if the listen interval is "real-time"
            if (listenInterval == -1) {

                // start recording
                if (soundStreamJob != null) {
                    soundStreamHandler.removeCallbacks(soundStreamJob);
                }
                soundStreamJob = new SoundStreamJob(0);
                soundStreamHandler.post(soundStreamJob);
            } else {

                context.registerReceiver(alarmReceiver, new IntentFilter(ACTION_NOISE));

                Intent alarm = new Intent(ACTION_NOISE);
                PendingIntent alarmOperation = PendingIntent.getBroadcast(context, REQID, alarm, 0);
                AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                mgr.cancel(alarmOperation);
                mgr.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
                        listenInterval, alarmOperation);
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception in startSensing:" + e.getMessage());
        }
    }
}
