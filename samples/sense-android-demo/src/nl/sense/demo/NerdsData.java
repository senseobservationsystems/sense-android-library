package nl.sense.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import nl.sense_os.platform.SensePlatform;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SensorData.SensorNames;

public class NerdsData {
	private String TAG = "Realtime data";
	private SensePlatform sensePlatform;

	private AggregateData<String> motion;
	private AggregateData<String> audioVolume;

	public NerdsData(SensePlatform platform) {
		sensePlatform = platform;

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
					String bin = db < 40 ? "low" : db < 60 ? "medium" : "high";
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
	}

	/* Steps */
	// TODO: add steps time series data

	/** Return step data of the user.
	 * 
	 * @return json object containing mean steps per minute and total steps taken
	 */
	public JSONObject getMyStepsData() {

		String dummy = "{\"mean\":60, \"total\":500}";
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
		String dummy = "{\"total\":80000, \"mean\":50}";
		try {
			return new JSONObject(dummy);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
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
		String dummy = "{\"low\":20, \"high\":20, \"medium\":60}";
		try {
			return new JSONObject(dummy);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
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
		String dummy = "{\"low\":20, \"high\":30, \"medium\":50}";
		try {
			return new JSONObject(dummy);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	/* Sit/Stand */
	/** Return sit/stand time of the user
	 * 
	 * @return json object containing seconds sitting and seconds standing
	 */
	public JSONObject getMySitStandData() {
		String dummy = "{\"sit\":180.0, \"stand\":3600.0}";
		try {
			return new JSONObject(dummy);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	/** Return sit/stand time of the group
	 * 
	 * @return json object containing seconds sitting and seconds standing
	 */
	public JSONObject getGroupSitStandData() {
		String dummy = "{\"mean sit\":100.0, \"mean stand\":4200.0}";
		try {
			return new JSONObject(dummy);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	/* Heatmap */

	/** Return heatmap of the user
	 * 
	 * @return json object containing seconds spend at each zone
	 */
	public JSONObject getMyPositionHeatmap() {
		String dummy = "{\"zone 1\":60, \"zone 2\":180}";
		try {
			return new JSONObject(dummy);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	/** Return heatmap of the group
	 * 
	 * @return json object containing seconds spend at each zone
	 */
	public JSONObject getGroupPositionHeatmap() {
		String dummy = "{\"zone 1\":600, \"zone 2\":2000, \"zone 15\":1800}";
		try {
			return new JSONObject(dummy);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
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
}
