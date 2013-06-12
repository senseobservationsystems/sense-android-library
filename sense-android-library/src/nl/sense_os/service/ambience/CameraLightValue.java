/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.ambience;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.util.Log;

/**
 * Class that is responsible for taking camera preview images and converting them into light values.
 * Only works in Android 2.3 (?).
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 */
public class CameraLightValue {

	/**
	 * Interface for new camera light values.
	 * 
	 * @see CameraLightValue#getLightValue(int, CameraLightValueCallback)
	 */
	public interface CameraLightValueCallback {
		public void lightValueCallback(float lightValue, int camera_id);
	}

	/**
	 * Handles camera previews by passing calculating the light value from the preivew and passing
	 * it along to a {@link CameraLightValueCallback}.
	 */
	private class CameraPreviewCallback implements PreviewCallback {
		private int camera_id;
		CameraLightValueCallback camlightCallback;

		public CameraPreviewCallback(int camera_id, CameraLightValueCallback camlightCallback) {
			this.camera_id = camera_id;
			this.camlightCallback = camlightCallback;
		}

		private float calculateLightValue(byte[] data, int width, int height) {
			try {
				float lux = 0;
				// Data is YCrCb NV21 encoding
				// the first height*width are Y: luminance values

				// calculate the average luminance
				for (int i = 0; i < height * width; i++) {
					// conversion to java unsigned byte
					if (data[i] < 0)
						lux += (data[i] + 256);
					else
						lux += data[i];
				}
				return lux /= ((float) width * height);
			} catch (Exception e) {
				Log.e(TAG, "Error calculating the light value", e);
				return -1;
			}
		}

		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			try {
				int width = camera.getParameters().getPreviewSize().width;
				int height = camera.getParameters().getPreviewSize().height;
				// important set the callback to null or the android camera thread fails
				cameraDevices[camera_id].setPreviewCallback(null);
				cameraDevices[camera_id].stopPreview();
				cameraDevices[camera_id].release();
				cameraDevices[camera_id] = null;
				float lightValue = calculateLightValue(data.clone(), width, height);
				camlightCallback.lightValueCallback(lightValue, camera_id);
			} catch (Exception e) {
				Log.e(TAG, "Error while processing preview frame:" + e.getMessage());
				cameraDevices[camera_id] = null;
			}
		}
	}

	private String TAG = "Camera Light Value";
	private Camera[] cameraDevices;
		
	public CameraLightValue() {	
		cameraDevices = new Camera[getNumberOfCameras()];
	}

	@SuppressLint("NewApi")
	@TargetApi(9)
	public boolean getLightValue(final int camera_id,
			final CameraLightValueCallback camlightCallback) {		
		// this camera code only works for API > 9 and API < 14
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD) {
			try {
				if (camera_id > getNumberOfCameras() - 1)
					return false;
				if (cameraDevices[camera_id] != null)
					return false;
				else {
					cameraDevices[camera_id] = Camera.open(camera_id);
					Camera.Parameters parameters = cameraDevices[camera_id].getParameters();
					parameters.setExposureCompensation(0);
					parameters.setWhiteBalance("daylight");					
					cameraDevices[camera_id].setPreviewCallback(new CameraPreviewCallback(
							camera_id, camlightCallback));
					cameraDevices[camera_id].setPreviewDisplay(null);
					cameraDevices[camera_id].setPreviewTexture(new SurfaceTexture(10));
					cameraDevices[camera_id].startPreview();
					
				}
				return true;
			} catch (Exception e) {
				Log.e(TAG, "Error getting camera " + camera_id + " " + e.getMessage());
				cameraDevices[camera_id] = null;
				try {
					cameraDevices[camera_id].release();
				} catch (Exception e2) {
					Log.e(TAG, "Error releasing the camera " + camera_id + " " + e2.getMessage());
				}

				return false;
			}

		} else {
			Log.w(TAG, "Camera is not available in this Android version: " + Build.VERSION.SDK_INT);
			return false;
		}
	};

	@TargetApi(9)
	public int getNumberOfCameras() {
		try {
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD) {
				
				return 2;
			
			} else {
				return 1;
			}
		} catch (Exception e) {
			Log.e(TAG, "Error getting the number of camera's " + e.getMessage());
			return 0;
		}
	}
}
