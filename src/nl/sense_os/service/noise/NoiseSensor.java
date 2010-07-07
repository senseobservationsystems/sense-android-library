package nl.sense_os.service.noise;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import nl.sense_os.service.MsgHandler;
import nl.sense_os.service.noise.NoiseSensorStream.SoundStreamServerThread;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;

public class NoiseSensor extends PhoneStateListener {
	class NoiseSensorThread implements Runnable 
{
    
    class CalculateNoiseThread implements Runnable 
    {
    	public CalculateNoiseThread() 
    	{ }

    	private double calculateDB()
    	{
    		double dB = 0;
    		try			
    		{				
    			if(!listening)
    				return 0;
    			byte[] buffer = new byte[bufferSize];
    			int readBytes = 0;
    			if(audioRec != null)
    				readBytes = audioRec.read(buffer, 0, bufferSize);			

    			for (int x = 0; x < readBytes; x++)				
    				dB += Math.abs(buffer[x]);	
    			dB /= (double)buffer.length;
    			Log.d(TAG, "buffer length"+ buffer.length + " buffer size:"+ bufferSize +" dB:" +dB);
    		} 	
    		catch (Exception e) 
    		{							
    			e.printStackTrace();
    		}
    		return dB;
    	}
    	public void run() 
    	{						
    		double dB = calculateDB();
    		msgHandler.sendSensorData("noise_sensor", "" + dB);
    		if(audioRec != null && audioRec.getState() == AudioRecord.STATE_INITIALIZED)
    			audioRec.stop();
    		Log.d(TAG,"Done recording.");
    		if(listening)
    			noiseThreadHandler.postDelayed(new NoiseSensorThread(), listenInterval);				
    	}
    }

    public NoiseSensorThread() 
    { }

    public void run() 
    {		
    	if(audioRec == null)
    	{
    		Log.d(TAG,"AudioRec is null.");
    		return;
    	}
    	Log.d(TAG,"Start recording...");
    	if(listening)
    	{
    		audioRec.startRecording();						
    		calculateNoiseHandler.postDelayed(new CalculateNoiseThread(), sampleTime);
    	}			
    }
}
	class SoundStreamThread implements Runnable {
		public void run() {	

			try {
//				cameraDevice = android.hardware.Camera.open();
//				Parameters params = cameraDevice.getParameters();
//				String effect = "mono";
//				params.set("effect", effect);
//				cameraDevice.setParameters(params);
//				recorder.setCamera(cameraDevice);
				
				recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
				//recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);				
				recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
				//recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
				recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
								
				new File(recordFileName).createNewFile();
				String command = "chmod 666 " + recordFileName; 
				Runtime.getRuntime().exec(command); 
				recorder.setOutputFile(recordFileName);
				recorder.setMaxDuration(sampleTimeStream);
				recorder.setOnInfoListener(new OnInfoListener() {

					public void onInfo(MediaRecorder mr, int what, int extra) {
						if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED)
						{
							try
							{	
						
							// recording is done, upload file							
							recorder.stop();
							recorder.reset();
							// wait until finished otherwise it will be overwritten
							msgHandler.uploadFile("microphone", recordFileName); 
							if(listening && listenInterval == -1) 
								soundStreamHandler.post(new SoundStreamThread());
							}catch(Exception e)
							{
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
	private static final String TAG = "NoiseSensor";
	private AudioRecord audioRec; 
	private int bufferSize = 4096; 	
	private Handler calculateNoiseHandler = new Handler(); 	
	private MsgHandler msgHandler 	= null;
	private boolean listening 	= false;
	private int listenInterval;				// Update interval in msec	
	private Handler noiseThreadHandler = new Handler();
	private Handler soundStreamHandler = new Handler();	
	private MediaRecorder recorder = null;	
	private String recordFileName = Environment.getExternalStorageDirectory().getAbsolutePath()+"/micSample.3gp";
	//private Camera cameraDevice = null;
	int sampleTime			= 2000;
	int sampleTimeStream 	= 10000;
		
	public NoiseSensor(MsgHandler handler)
	{
		this.msgHandler = handler;				
	}
	
	public int getSampleTime()
	{
		return sampleTime;
	};
	
	@Override
    public void onCallStateChanged(int state, String incomingNumber) {
        Log.d(TAG, "Call state changed.");
          
       if(state != TelephonyManager.CALL_STATE_IDLE)       
       {
    	   stopListening();
    	   
       }
       else if(listening)
    	   startListening(listenInterval);
    	   
        super.onCallStateChanged(state, incomingNumber);
    }
		
	public void setSampleTime(int sTime)
	{
		sampleTime = sTime;
	}

	public void startListening(int interval)
	{
		listenInterval = interval;		
		listening = true;
		
		Thread t = new Thread() {
			public void run() 
			{
				if(listenInterval == -1)
				{		
					recorder = new MediaRecorder();
					soundStreamHandler.post(new SoundStreamThread());
				}
				else
				{
					bufferSize =  AudioRecord.getMinBufferSize(DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
					audioRec = new AudioRecord(MediaRecorder.AudioSource.MIC, DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

					if(audioRec.getState() == AudioRecord.STATE_UNINITIALIZED )
					{
						Log.d(TAG, "Uninitialized AudioRecord format: "+audioRec.getAudioFormat()+" source: "+audioRec.getAudioSource() + " channel:"+audioRec.getChannelConfiguration() +" buffersize:" + bufferSize);
						return;
					}
					noiseThreadHandler.postDelayed(new NoiseSensorThread(), listenInterval);
				}
			}
		};
		t.start();

	}
		
	public void stopListening()
	{	
		try{
			
		
		listening = false;
		if(audioRec != null)
			audioRec.release();
		if(listenInterval == -1 && recorder != null)
		{
			recorder.stop();
			recorder.reset();   // You can reuse the object by going back to setAudioSource() step
			//recorder.release(); // Now the object cannot be reused
		}
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
