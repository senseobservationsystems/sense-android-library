package nl.sense.demo;

import java.io.IOException;

import nl.sense.ips.data.Parameters;
import nl.sense.ips.filters.Locator;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import nl.sense_os.cortex.dataprocessor.SitStand;
import nl.sense_os.cortex.dataprocessor.StepCounter;
import nl.sense_os.platform.SensePlatform;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SensorData.SensorNames;

public class NerdsData {
	static final long historySize = 100;
	
	private SensePlatform sensePlatform;

	private AggregateData<String> motion;
	private AggregateData<String> audioVolume;
	private AggregateData<String> sitStandTime;
	private AggregateData<String> indoorPosition;
	private AggregateData<String> steps;
	
	//some real time data
	private double lastMotion;
	private double lastAudio;
	private double highestAudio = 0;
	private String lastIndoorPosition;
	private double totalSteps;
	private double stepsTime;
	private double lastStepsPerMinute;
	
	private SitStand sitStand;
	private StepCounter stepCounter;
	
	protected static NerdsData instance;
	
	public static NerdsData getInstance(SensePlatform platform) {
		if (instance == null)
			instance = new NerdsData(platform);
		return instance;
	}

	private NerdsData(SensePlatform platform) {
		sensePlatform = platform;

		sitStand = new SitStand("activity", sensePlatform.getService().getSenseService());
		sitStand.enable();
		
		stepCounter = new StepCounter("step counter", sensePlatform.getService().getSenseService());
		
		// motion, in categories "low", "medium", "high". Use accelerometer as
		// some devices lack linear acceleration
		motion = new AggregateData<String>(sensePlatform,
				SensorNames.ACCELEROMETER, null) {
			Long previousTimestamp = null;

			@Override
			public void aggregateSensorDataPoint(JSONObject dataPoint) {
				try {
					long timestamp = dataPoint.getLong("date");
					JSONObject value = new JSONObject(
							dataPoint.getString("value"));// dataPoint.getJSONObject("value");

					double x = value.getDouble("x-axis");
					double y = value.getDouble("y-axis");
					double z = value.getDouble("z-axis");
					double magnitude = Math.sqrt(x * x + y * y + z * z);
					final double G = 9.81;
					double linAccMagnitude = magnitude - G;
					
					lastMotion = linAccMagnitude;

					String bin = linAccMagnitude < 2 ? "low"
							: linAccMagnitude < 5 ? "medium" : "high";
					long dt = 0;
					if (previousTimestamp != null) {
						dt = Math.abs(timestamp - previousTimestamp);
						// longer than 5 minutes means missing data
						if (dt > 5 * 60 * 1000)
							dt = 0;
					}
					previousTimestamp = timestamp;
					addBinValue(bin, dt);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		};
		motion.importData();

		// audio volume in categories "low", "medium", "high"
		audioVolume = new AggregateData<String>(sensePlatform,
				SensorNames.NOISE, null) {
			private Long previousTimestamp;

			@Override
			public void aggregateSensorDataPoint(JSONObject dataPoint) {
				try {
					long timestamp = dataPoint.getLong("date");
					double db = dataPoint.getDouble("value");
					lastAudio = db;
					if (db > highestAudio) highestAudio = db;
					String bin = db < 50 ? "low" : db < 70 ? "medium" : "high";
					long dt = 0;
					if (previousTimestamp != null) {
						dt = Math.abs(timestamp - previousTimestamp);
						// longer than 5 minutes means missing data
						if (dt > 5 * 60 * 1000)
							dt = 0;
					}
					previousTimestamp = timestamp;
					addBinValue(bin, dt);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		};
		audioVolume.importData();
		
		/* Sit stand time */
		// time spent sitting and standing
		sitStandTime = new AggregateData<String>(sensePlatform,
				"activity", null) {
			private Long previousTimestamp;

			@Override
			public void aggregateSensorDataPoint(JSONObject dataPoint) {
				try {
					String bin = dataPoint.getString("value");
					//try to parse as json
					try {		
					 bin = new JSONObject(bin).getString("value");
					} catch (JSONException e) {
					}
					final long timestamp = dataPoint.getLong("date");
					
					final String binValue = bin;
					//add data point to the sensor
					try {
						Thread sendData =  new Thread() { public void run() {
							sensePlatform.addDataPoint("activity", "activity", "sit/stand", "string", binValue, timestamp);
							
						}};
						sendData.start();
					} catch (Exception e) {
						Log.e(TAG, "Failed to add activity data point!", e);
					}
					

					long dt = 0;
					if (previousTimestamp != null) {
						dt = Math.abs(timestamp - previousTimestamp);
						// longer than 5 minutes means missing data
						if (dt > 5 * 60 * 1000)
							dt = 0;
					}
					previousTimestamp = timestamp;
					addBinValue(bin, dt);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		};
		sitStandTime.importData();
		
		steps = new AggregateData<String>(sensePlatform,
				"step counter", null) {
			private Long previousTimestamp;

			@Override
			public void aggregateSensorDataPoint(JSONObject dataPoint) {
				try {
					 String bin = dataPoint.getString("value");
					final long timestamp = dataPoint.getLong("date");
					try {		
						 bin = new JSONObject(bin).getString("value");
						} catch (JSONException e) {
						}
					//add data point to the sensor	
					final String binValue = bin;					
					try {
						Thread sendData =  new Thread() { public void run() {
							sensePlatform.addDataPoint("step counter", "step counter", "step counter", "json", binValue, timestamp);
							
						}};
						sendData.start();
					} catch (Exception e) {
						Log.e(TAG, "Failed to add step counter data point!", e);
					}
					
					
					JSONObject value;
					//try to parse as json		
					value = new JSONObject(bin);
					
					double total = value.getLong("total steps");
					long spm = value.getLong("steps");

					long dt = 10;
					if (previousTimestamp != null) {
						dt = Math.abs(timestamp - previousTimestamp);
						// longer than 1 minutes means missing data
						if (dt > 1 * 60 * 1000)
							dt = 10;
					}
					previousTimestamp = timestamp;
					stepsTime += dt;
					totalSteps = total;
					lastStepsPerMinute = spm;
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		};
		steps.importData();
		
		
		
		indoorPosition = new AggregateData<String>(sensePlatform, SensorNames.WIFI_SCAN, null) {
			private Long previousTimestamp;
			private ArrayList<JSONObject> dataPoints = new ArrayList<JSONObject>();
			private Parameters params = new Parameters();
			private Locator locator = new Locator(params);
			private long previousUpdate=0; 
			
			@Override
			public void aggregateSensorDataPoint(JSONObject dataPoint)   {
				long timestamp;
				try {
					timestamp = dataPoint.getLong("date");
				} catch (JSONException e) {
					e.printStackTrace();
					return;
				}

				//keep a list of the last maxWifiPoints data points
				dataPoints.add(dataPoint);
				while (dataPoints.size() > 100)//params.getNumDatapoints())
					dataPoints.remove(0);
				
				//don't update too often
				if (timestamp - previousUpdate < 10*1000l)
					return;
				previousUpdate = timestamp;
				
				//use the list to find a location
				JSONArray array = new JSONArray();
				for (JSONObject o : dataPoints)
					array.put(o);
				double[] deviceLocation = locator.computeLocation(array);
				//transform location into a zone
				String zone = "zone " + Math.round(deviceLocation[0]);
				if (deviceLocation[0] < 0)
					zone = "unknown";
				
				final String zoneValue = zone;
				final long timeStampValue = timestamp;
				//store the location as a sensor data point
				try {
					Thread sendData =  new Thread() { public void run() {
						sensePlatform.addDataPoint("indoor position", "indoor position", "indoor position", "string", zoneValue, timeStampValue);						
					}};
					sendData.start();
				} catch (Exception e) {
					Log.e(TAG, "Failed to add indoor position data point!", e);
				}
				
				lastIndoorPosition = zone;
				
				
				//update the heatmap
				long dt = 0;
				if (previousTimestamp != null) {
					dt = Math.abs(timestamp - previousTimestamp);
					// longer than 5 minutes means missing data
					if (dt > 5 * 60 * 1000)
						dt = 0;
				}
				previousTimestamp = timestamp;
				addBinValue(zone, dt);
				
			}
		};
		
		indoorPosition.importData();
		
		
		//for real time data tracking
		//audioSeries = new DataTimeSeries(sensePlatform, "noise_sensor", "noise_sensor", historySize);
		//motionSeries = new DataTimeSeries(sensePlatform, SensorNames.ACCELEROMETER, null, historySize);
		//indoorPositionSeries = new DataTimeSeries(sensePlatform, "indoor position", null, 10);
	}

	/* Steps */
	// TODO: add steps time series data

	/** Return step data of the user.
	 * 
	 * @return json object containing mean steps per minute and total steps taken
	 */
	public JSONObject getMyStepsData() {

		double mean = 0;
		if (stepsTime > 0)
			mean = totalSteps / stepsTime;
		String dummy = "{\"mean\":"+mean+",\"total\":"+totalSteps+"}";
		try {
			return new JSONObject(dummy);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	/** Return step data of the group.
	 * 
	 * @return json object containing mean steps per minute and total steps taken
	 */
	public JSONObject getGroupStepsData() {
		try {
			return new JSONObject(sensePlatform.getLastDataForSensor("337193").getString("value"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/* Motion */

	/** Return motion data of the user.
	 * 
	 * @return json object containing percentage of motion in "low", "medium" and "high"
	 */
	public JSONObject getMyMotionData() {
		// String dummy = "{\"low\":20, \"high\":20, \"medium\":60}";
		ArrayList<AggregateData<String>.DataValue<String>> hist = motion
				.getSorted();
		double sum = 0;
		for (AggregateData<String>.DataValue<String> value : hist) {
			sum += value.sum();
		}
		HashMap<String, Double> normalised = new HashMap<String, Double>(
				hist.size());
		for (AggregateData<String>.DataValue<String> value : hist) {
			normalised.put(value.bin(), value.sum() / sum);
		}

		return new JSONObject(normalised);
	}

	/** Return motion data of the group.
	 * 
	 * @return json object containing percentage of motion in "low", "medium" and "high"
	 */
	public JSONObject getGroupMotionData() {
		try {
			return new JSONObject(sensePlatform.getLastDataForSensor("337194").getString("value"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/* Audio volume */

	/** Return audio volume data of the user.
	 * 
	 * @return json object containing percentage of audio in "low", "medium" and "high"
	 */
	public JSONObject getMyAudioVolumeData() {
		// String dummy = "{\"low\":20, \"high\":20, \"medium\":60}";
		ArrayList<AggregateData<String>.DataValue<String>> hist = audioVolume
				.getSorted();
		double sum = 0;
		for (AggregateData<String>.DataValue<String> value : hist) {
			sum += value.sum();
		}
		HashMap<String, Double> normalised = new HashMap<String, Double>(
				hist.size());
		for (AggregateData<String>.DataValue<String> value : hist) {
			normalised.put(value.bin(), value.sum() / sum);
		}

		return new JSONObject(normalised);
	}

	/** Return audio volume data of the group.
	 * 
	 * @return json object containing percentage of audio in "low", "medium" and "high"
	 */
	public JSONObject getGroupAudioVolumeData() {
		try {
			return new JSONObject(sensePlatform.getLastDataForSensor("337195").getString("value"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/* Sit/Stand */
	/** Return sit/stand time of the user
	 * 
	 * @return json object containing seconds sitting and seconds standing
	 */
	public JSONObject getMySitStandData() {
		ArrayList<AggregateData<String>.DataValue<String>> hist = sitStandTime.getSorted();

		HashMap<String, Double> totalTimes = new HashMap<String, Double>(
				hist.size());
		for (AggregateData<String>.DataValue<String> value : hist) {
			totalTimes.put(value.bin(), value.sum() / 1000.0); //convert from milliseconds to seconds
		}

		return new JSONObject(totalTimes);
	}

	/** Return sit/stand time of the group
	 * 
	 * @return json object containing seconds sitting and seconds standing
	 */
	public JSONObject getGroupSitStandData() {
		try {
			return new JSONObject(sensePlatform.getLastDataForSensor("337196").getString("value"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/* Heatmap */

	/** Return heatmap of the user
	 * 
	 * @return json object containing seconds spend at each zone
	 */
	public JSONObject getMyPositionHeatmap() {
		ArrayList<AggregateData<String>.DataValue<String>> hist = indoorPosition.getSorted();

		HashMap<String, Double> totalTimes = new HashMap<String, Double>(
				hist.size());
		for (AggregateData<String>.DataValue<String> value : hist) {
			totalTimes.put(value.bin(), value.sum() / 1000.0); //convert from milliseconds to seconds
		}

		return new JSONObject(totalTimes);
	}

	/** Return heatmap of the group
	 * 
	 * @return json object containing seconds spend at each zone
	 */
	public JSONObject getGroupPositionHeatmap() {
		try {
			return new JSONObject(sensePlatform.getLastDataForSensor("337197").getString("value"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Join the nerds group. This function should be called to join the Night of
	 * the Nerds group. After joining data is shared with the group and the user
	 * has acces to the group sensors. Note, this function can be called multiple
	 * times without problems.
	 */
	public void joinNerdsGroup() {
		new Thread() {
			public void run() {
				String nerdsGroupId = "6352";
				try {
					SenseApi.joinGroup(sensePlatform.getContext(), nerdsGroupId);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	/* Some getters for real time data */
	
	/**
	 * Return the last motion
	 */
	public double getRTMotion() {
		return lastMotion;
	}
	
	/** 
	 * Return the last audio volume
	 */
	public double getRTAudioVolume() {
		return lastAudio;
	}
	
	/** 
	 * Return the highest audio volume
	 */
	public double getHighestAudioVolume() {
		return highestAudio;
	}
	
	/** 
	 * Return the last indoor position "zone 1" etc. and "unknown"
	 */
	public String getRTIndoorPosition() {
		return lastIndoorPosition;
	}
	
}
