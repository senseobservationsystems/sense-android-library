/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.ambience;

import java.io.File;
import java.math.BigDecimal;

import nl.sense_os.service.ambience.FFT;
import nl.sense_os.service.MsgHandler;
import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.Ambience;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.ctrl.Controller;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.PeriodicPollAlarmReceiver;
import nl.sense_os.service.shared.PeriodicPollingSensor;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.shared.SensorDataPoint.DataType;
import nl.sense_os.service.subscription.BaseSensor;
import nl.sense_os.service.subscription.DataProducer;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Main sound sensor class. Uses Android MediaRecorder class to capture small bits of audio and
 * convert it into sensor data. The AudioRecord is sampled periodically by setting an alarm
 * broadcast that starts a NoiseSampleJob or SoundStreamJob.
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 * @author Steven Mulder <steven@sense-os.nl>
 * 
 * @see LoudnessSensor
 */
public class NoiseSensor extends BaseSensor implements PeriodicPollingSensor {

    /**
     * Runnable that performs one noise sample. Starts the recording, reads the buffer contents,
     * calculates the noise power and sends the measurement to the {@link MsgHandler}. Also
     * schedules the next sample job.
     */
    private class NoiseSampleJob implements Runnable {

        private static final int DEFAULT_SAMPLE_RATE = 44100;
        /*
         * samples per second * 2 seconds, 2 bytes
         */
        private static final int RECORDING_TIME_NOISE = 5000;
        private static final int BYTES_PER_SAMPLE = 2;
        private static final int BUFFER_SIZE = (int) ((float) DEFAULT_SAMPLE_RATE
                * (float) BYTES_PER_SAMPLE * ((float) RECORDING_TIME_NOISE / 1000f));
        private AudioRecord audioRecord;
        private int FFT_BANDWITH_RESOLUTION = 10;
        private int FFT_MAX_HZ = 1000;

        // power of 2

        private float[] audioToFloat(byte[] buffer, int readBytes) {
            float[] samples = new float[readBytes / 2];
            int cnt = 0;
            for (int x = 0; x < readBytes - 1; x = x + 2) {
                double sample = 0;
                for (int b = 0; b < BYTES_PER_SAMPLE; b++) {
                    int v = (int) buffer[x + b];
                    if (b < BYTES_PER_SAMPLE - 1 || BYTES_PER_SAMPLE == 1) {
                        v &= 0xFF;
                    }
                    sample += v << (b * 8);
                }
                samples[cnt++] = (float) sample;
            }
            return samples;
        }

        /**
         * @param samples
         *            The sound data float values to calculate the power for.
         * 
         * @return the noise power of the current buffer. In case of an error, -1 is returned.
         */
        private double calculateDb(float[] samples) {

            double dB = 0;
            try {
                if (!active) {
                    Log.w(TAG, "Noise sensor is disabled, skipping noise power calculation...");
                    return -1;
                }

                if (samples.length <= 0) {
                    Log.e(TAG, "Error reading AudioRecord buffer: " + samples.length);
                    return -1;
                }
                double ldb = 0;
                for (int x = 0; x < samples.length; ++x) {
                    ldb += ((double) samples[x] * (double) samples[x]);
                }

                ldb /= (double) samples.length;
                dB = 10.0 * Math.log10(ldb);

            } catch (Exception e) {
                Log.e(TAG, "Exception calculating noise Db!", e);
                return -1;
            }

            // Log.d(TAG, "noise in db " + dB);
            return dB;
        }

        private double[] calculateSpectrum(float[] samples) {
        	if (samples == null || samples.length == 0)
        		return null;
        	int nrSamples = (int) Math.pow(2, (int) (Math.log(samples.length) / Math.log(2)));
        	int nrBands = (int) Math.ceil((float)DEFAULT_SAMPLE_RATE/2f / (float)FFT_BANDWITH_RESOLUTION);
        	if(nrBands > (int) Math.ceil((float)FFT_MAX_HZ/(float)FFT_BANDWITH_RESOLUTION))
        		nrBands = (int) Math.ceil((float)FFT_MAX_HZ/(float)FFT_BANDWITH_RESOLUTION);
        	
        	FFT fft = new FFT(nrSamples, DEFAULT_SAMPLE_RATE);	                        
        	fft.forward(copyOfRange(samples, 0, nrSamples));

        	double[] bins = new double[nrBands];	            	           
        	// computing averages does not work with small sample sizes, compute our own
        	for (int i = 0; i < nrBands; i++)
        	{	            	
        		float avg = 0;
        		for (int j = 0; j < FFT_BANDWITH_RESOLUTION; j++) {
        			avg += fft.getFreq(i*FFT_BANDWITH_RESOLUTION+j);					
        		}
        		avg /= FFT_BANDWITH_RESOLUTION;	            	
        		bins[i] = 10.0 * Math.log10(avg);           	
        	}
        	return bins;
        }

        // java versions before 6 don't have Arrays.copyOfRange, so make our own
        private float[] copyOfRange(float[] array, int start, int end) {
            if (end < start || start < 0)
                throw new IndexOutOfBoundsException(); // isn't there a
            // RangeException??
            if (array.length < end - start)
                throw new IndexOutOfBoundsException();
            float[] copy = new float[end - start];
            for (int i = start, j = 0; i < end; i++, j++)
                copy[j] = array[i];

            return copy;
        }

        /**
         * @return <code>true</code> if {@link #audioRecord} was initialized successfully
         */
        private boolean initAudioRecord() {
            // Log.d(TAG, "Initializing AudioRecord instance");

            if (null != audioRecord) {
                Log.w(TAG, "AudioRecord object is already present! Release it");
                // release the audioRecord object and stop any recordings that
                // are running
                stopSampling();
            }

            // create the AudioRecord
            try {
                int audioSource = -1, channelConfig = -1;
                if (calling) {
                    audioSource = MediaRecorder.AudioSource.VOICE_UPLINK;
                    channelConfig = AudioFormat.CHANNEL_IN_DEFAULT;

                } else {
                    audioSource = MediaRecorder.AudioSource.MIC;
                    channelConfig = AudioFormat.CHANNEL_IN_MONO;

                }
                audioRecord = new AudioRecord(audioSource, DEFAULT_SAMPLE_RATE, channelConfig,
                        AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Failed to create the audiorecord!", e);
                return false;
            }

            if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                Log.w(TAG, "Failed to create AudioRecord!");
                // Log.d(TAG, "format: " + audioRecord.getAudioFormat() +
                // " source: " +
                // audioRecord.getAudioSource() + " channel: " +
                // audioRecord.getChannelConfiguration() + " buffer size: " +
                // BUFFER_SIZE);
                return false;
            }

            // initialized OK
            return true;
        }

        @Override
        public void run() {

            if (active && !calling) {

                boolean init = initAudioRecord();

                if (init) {
                    long startTimestamp = SNTP.getInstance().getTime();
                    try {
                        Log.i(TAG, "Start recording for sound level measurement...");
                        audioRecord.startRecording();

                        // schedule task to stop recording and calculate the
                        // noise
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
                                // Log.d(TAG, "Read " + readResult + " bytes");
                                readCount += readResult;
                                if (readCount >= totalBuffer.length) {
                                    // Log.d(TAG, "Buffer overflow");
                                    break;
                                }
                            }
                        }

                        float[] samples = audioToFloat(totalBuffer, readCount);
                        double dB = -1;
                        double[] spectrum = null;
                        if (samples != null) {
                            SharedPreferences mainPrefs = context.getSharedPreferences(
                                    SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
                            if (mainPrefs.getBoolean(Ambience.MIC, true)) {
                                dB = calculateDb(samples);
                                controller.checkNoiseSensor(dB);
                            }
                            if (mainPrefs.getBoolean(Ambience.AUDIO_SPECTRUM, true))
                                spectrum = calculateSpectrum(samples);
                        }

                        if (dB != -1 && !Double.valueOf(dB).isNaN()) {
                            // Log.d(TAG, "Sampled noise level: " + dB);

                            notifySubscribers();
                            SensorDataPoint dataPoint = new SensorDataPoint(dB);
                            dataPoint.sensorName = SensorNames.NOISE;
                            dataPoint.sensorDescription = SensorNames.NOISE;
                            dataPoint.timeStamp = startTimestamp;
                            sendToSubscribers(dataPoint);

                            // pass message to the MsgHandler
                            Intent sensorData = new Intent(
                                    context.getString(R.string.action_sense_new_data));
                            sensorData.putExtra(DataPoint.SENSOR_NAME, SensorNames.NOISE);
                            sensorData.putExtra(DataPoint.VALUE,
                                    BigDecimal.valueOf(dB).setScale(2, 0).floatValue());
                            sensorData.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
                            sensorData.putExtra(DataPoint.TIMESTAMP, startTimestamp);
                            context.startService(sensorData);
                        }

                        if (spectrum != null) {
                            JSONObject jsonSpectrum = new JSONObject();
                            jsonSpectrum.put("bandwidth", FFT_BANDWITH_RESOLUTION);                            
                            JSONArray jsonSpectrumArray = new JSONArray();
                            for (int i = 0; i < spectrum.length; i++)
                            {
                            	if (spectrum[i] == Double.POSITIVE_INFINITY)
                                	jsonSpectrumArray.put(140);
                                 else if (spectrum[i] != Double.NaN && spectrum[i] != Double.NEGATIVE_INFINITY) // normal case
                                	 jsonSpectrumArray.put(Math.round(spectrum[i]));
                                 else	
                                	jsonSpectrumArray.put(0);                                
                            }
                            
                            jsonSpectrum.put("spectrum", jsonSpectrumArray);
                            notifySubscribers();
                            SensorDataPoint dataPoint = new SensorDataPoint(jsonSpectrum);
                            dataPoint.sensorName = SensorNames.AUDIO_SPECTRUM;
                            dataPoint.sensorDescription = "audio spectrum (dB)";
                            dataPoint.timeStamp = startTimestamp;
                            sendToSubscribers(dataPoint);

                            Intent sensorData = new Intent(
                                    context.getString(R.string.action_sense_new_data));
                            sensorData.putExtra(DataPoint.SENSOR_NAME, SensorNames.AUDIO_SPECTRUM);
                            sensorData
                                    .putExtra(DataPoint.SENSOR_DESCRIPTION, "audio spectrum (dB)");
                            sensorData.putExtra(DataPoint.VALUE, jsonSpectrum.toString());
                            sensorData.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
                            sensorData.putExtra(DataPoint.TIMESTAMP, startTimestamp);
                            context.startService(sensorData);
                        }

                        if (dB != -1 && !Double.valueOf(dB).isNaN()) {
                            loudnessSensor.onNewNoise(startTimestamp, dB);
                            autoCalibratedNoiseSensor.onNewNoise(startTimestamp, dB);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Exception starting noise recording! " + e, e);
                    } finally {
                        stopRecording();
                        // if real time, post, since alarm won't repeat
                        if (getSampleRate() == -1) {
                            /*
                             * Some code to align on the second, but it doesn't seem to work :-(
                             * Calendar now = Calendar.getInstance(); // calculate offset of the
                             * local clock int offset = (int) (System.currentTimeMillis() - SNTP
                             * .getInstance().getTime()); // align the start time on a second
                             * Calendar startTime = (Calendar) now.clone();
                             * startTime.set(Calendar.SECOND, 0);
                             * startTime.set(Calendar.MILLISECOND, 0); // correct for the difference
                             * in local time and ntp // time startTime.roll(Calendar.MILLISECOND,
                             * offset);
                             * 
                             * // advance to the next second until the start time // is at least 100
                             * ms in the future while (startTime.getTimeInMillis() -
                             * now.getTimeInMillis() <= 100) { startTime.roll(Calendar.SECOND, 1); }
                             * noiseSampleJob = new NoiseSampleJob();
                             * noiseSampleHandler.postAtTime(noiseSampleJob,
                             * startTime.getTimeInMillis());
                             */
                            noiseSampleHandler.post(noiseSampleJob);
                        }
                    }

                } else {
                    Log.w(TAG, "Did not start recording: AudioRecord could not be initialized!");
                }

            } else {
                // did not start recording: noise sensor is disabled
            }
        }

        /**
         * Stops the recording and releases the AudioRecord object, making it unusable.
         */
        protected void stopRecording() {

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
                if (calling) {
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

                                notifySubscribers();
                                SensorDataPoint dataPoint = new SensorDataPoint(fileName);
                                dataPoint.sensorName = SensorNames.MIC;
                                dataPoint.sensorDescription = SensorNames.MIC;
                                dataPoint.setDataType(DataType.FILE);
                                dataPoint.timeStamp = SNTP.getInstance().getTime();
                                sendToSubscribers(dataPoint);

                                // pass message to the MsgHandler
                                Intent i = new Intent(context
                                        .getString(R.string.action_sense_new_data));
                                i.putExtra(DataPoint.SENSOR_NAME, SensorNames.MIC);
                                i.putExtra(DataPoint.VALUE, fileName);
                                i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FILE);
                                i.putExtra(DataPoint.TIMESTAMP, dataPoint.timeStamp);
                                context.startService(i);

                                if (isActive() && getSampleRate() == -1
                                        && tmp.equals(soundStreamJob)) {
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
                Log.e(TAG, "Error while recording sound:", e);
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
            }
        }
    }

    private static NoiseSensor instance = null;

    private static final String TAG = "Sense NoiseSensor";

    /**
     * Factory method to get the singleton instance.
     * 
     * @param context
     * @return instance
     */
    public static NoiseSensor getInstance(Context context) {
        if (instance == null) {
            instance = new NoiseSensor(context);
        }
        return instance;
    }

    private boolean active = false;
    private boolean calling = false;
    private Context context;
    private Handler soundStreamHandler = new Handler(Looper.getMainLooper());
    private SoundStreamJob soundStreamJob = null;
    private Handler noiseSampleHandler = new Handler();
    private NoiseSampleJob noiseSampleJob = null;
    private LoudnessSensor loudnessSensor;
    private AutoCalibratedNoiseSensor autoCalibratedNoiseSensor;
    private Controller controller;
    private PeriodicPollAlarmReceiver pollAlarmReceiver;
    private PhoneStateListener phoneStateListener = new PhoneStateListener() {

        /**
         * Pauses sensing when the phone is used for calling, and starts it again after the call.
         */
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            // Log.d(TAG, "Call state changed");
            try {
                if (state == TelephonyManager.CALL_STATE_OFFHOOK
                        || state == TelephonyManager.CALL_STATE_RINGING) {
                    calling = true;
                } else {
                    calling = false;
                }

                stopSampling();

                // recording while calling is disabled
                if (isActive() && state == TelephonyManager.CALL_STATE_IDLE && !calling) {
                    startSampling();
                }

            } catch (Exception e) {
                Log.e(TAG, "Exception in onCallStateChanged!", e);
            }
        }
    };

    /**
     * Constructor.
     * 
     * @param context
     * @see #getInstance(Context)
     */
    protected NoiseSensor(Context context) {
        this.context = context;
        pollAlarmReceiver = new PeriodicPollAlarmReceiver(this);
        controller = Controller.getController(context);
        loudnessSensor = LoudnessSensor.getInstance(context);
        autoCalibratedNoiseSensor = AutoCalibratedNoiseSensor.getInstance(context);
    }

    @Override
    public void doSample() {
        Log.v(TAG, "Do sample");

        // clear old sample jobs
        if (noiseSampleJob != null) {
            noiseSampleJob.stopRecording();
            noiseSampleHandler.removeCallbacks(noiseSampleJob);
        }

        // start sample job
        if (active /* && listenInterval != -1 */) {
            noiseSampleJob = new NoiseSampleJob();
            // noiseSampleHandler = new Handler();
            noiseSampleHandler.post(noiseSampleJob);
        }
    }

    public DataProducer getLoudnessSensor()
    {
    	return loudnessSensor;
    }
    public DataProducer getAutoCalibratedNoiseSensor() {
        return autoCalibratedNoiseSensor;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    /**
     * Starts the sound sensing jobs.
     */
    private void startSampling() {
        Log.v(TAG, "Start sound sensor sampling");

        try {

            // different job if the listen interval is "real-time"
            if (getSampleRate() == -1) {
                // start recording
                if (soundStreamJob != null) {
                    soundStreamHandler.removeCallbacks(soundStreamJob);
                }
                soundStreamJob = new SoundStreamJob(0);
                soundStreamHandler.post(soundStreamJob);

            } else {

                // register for periodic polls at the scheduler
                pollAlarmReceiver.start(context);

                // do first sample immediately
                doSample();
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception in startSensing:" + e.getMessage());
        }
    }

    /**
     * Enables the noise sensor, starting the sound recording and registering it as phone state
     * listener.
     */
    @Override
    public void startSensing(long sampleDelay) {
        Log.v(TAG, "Start sensing");
        stopSensing();
        setSampleRate(sampleDelay);
        active = true;

        // registering the phone state listener will trigger a call to startListening()
        TelephonyManager telMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        telMgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * Stops any active sensing jobs, and stops and cleans up the AudioRecord.
     */
    private void stopSampling() {
        Log.v(TAG, "Stop sound sensor sample");

        try {

            // stop the sound recordings/*
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
     * Disables the noise sensor, stopping the sound recording and unregistering it as phone state
     * listener.
     */
    @Override
    public void stopSensing() {
        Log.v(TAG, "Stop sensing");

        active = false;
        stopSampling();
        pollAlarmReceiver.stop(context);
        TelephonyManager telMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        telMgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
    }
}
