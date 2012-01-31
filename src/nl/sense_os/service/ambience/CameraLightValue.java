/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.ambience;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;

public class CameraLightValue{	
		
	private String TAG = "Camera Light Value";
	
	public Camera[] cameraDevices;
		
	public CameraLightValue() {
		cameraDevices = new Camera[getNumberOfCameras()];		
	}
	
	public int getNumberOfCameras()
	{
		try
		{
			return Camera.getNumberOfCameras();
		}
		catch(Exception e)
		{
			Log.e(TAG, "Error getting the number of camera's "+ e.getMessage());
			return 0;
		}
	}
	
	public boolean getLightValue(final int camera_id, final CameraLightValueCallback camlightCallback)
	{	
		try
		{			
			if(camera_id > Camera.getNumberOfCameras()-1)
				return false;
			if(cameraDevices[camera_id] != null)
				return false;
			else			
			{
				cameraDevices[camera_id] = Camera.open(camera_id);	
				cameraDevices[camera_id].setPreviewCallback(new CameraPreviewCallback(camera_id, camlightCallback));		
				cameraDevices[camera_id].startPreview();				
			}
			// what happens to the preview callback if this camera is removed by the garbage collection?
			return true;
		}
		catch (Exception e)
		{
			Log.e(TAG, "Error getting camera " + camera_id +" "+e.getMessage());
			cameraDevices[camera_id] = null;
			return false;
		}
	}	
	
	public class CameraPreviewCallback implements PreviewCallback 
	{
		private int camera_id;
		CameraLightValueCallback camlightCallback;
		
		CameraPreviewCallback(int camera_id, CameraLightValueCallback camlightCallback)
		{
			this.camera_id 			= camera_id;
			this.camlightCallback 	= camlightCallback;
		}
		
		public void onPreviewFrame(byte[] data, Camera camera)
		{		
			try
			{
				int width = camera.getParameters().getPreviewSize().width;
				int height = camera.getParameters().getPreviewSize().height;
				// important set the callback to null or the android camera thread fails
				cameraDevices[camera_id].setPreviewCallback(null);
				cameraDevices[camera_id].stopPreview();
				cameraDevices[camera_id].release();
				cameraDevices[camera_id] = null;	
				float lightValue = calculateLightValue(data.clone(), width, height);
				camlightCallback.lightValueCallback(lightValue, camera_id);							
			}
			catch(Exception e)
			{
				Log.e(TAG, "Error while processing preview frame:"+e.getMessage());				
				cameraDevices[camera_id] = null;
			}
		}
		
		private float calculateLightValue(byte[] data, int width, int height)
		{
			try
			{
				Log.d(TAG, "Calculating light value");
				float lux = 0;
				// Data is YCrCb NV21 encoding
				// the first height*width are Y: luminance values
				
				// calculate the average luminance
				for (int i = 0; i < height*width; i++)	
				{
					// conversion to java unsigned byte 
					if(data[i] < 0)
						lux += (data[i]+255);				
					else
						lux += data[i];			
				}
	
				return lux /= ((float)width*height);
			}
			catch(Exception e)
			{
				Log.e(TAG, "Error calculating the light value", e);
				return -1;
			}
		}
	}; 
		
	public interface CameraLightValueCallback
	{				
		public void lightValueCallback(float lightValue, int camera_id);
	}
}
