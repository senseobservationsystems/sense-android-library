package nl.sense.ips.filters;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import nl.sense.ips.data.Datapoint;
import nl.sense.ips.data.Parameters;
import nl.sense.ips.data.WiFiDatapoint;

import org.json.JSONArray;

import com.google.gson.Gson;

import android.util.Log;

/**
 * 
 * @author Ruud Henken <ruud@sense-os.nl>
 * 
 */

public class Locator {

	private static final String TAG = "Locator";
	private Parameters params;

	/**
	 * Constructor
	 * 
	 */
	public Locator(Parameters params) {
		this.params = params;
	}

	/**
	 * This method creates an observation matrix from the scanResult
	 * 
	 * @param scanResult
	 *            The scanResult should be a CommonSense JSONArray with values and dates, where the values should be of the type WiFiDatapoint. Dates should be of type double.
	 * @return This method returns an array with average RSSI values for each landmark that is present in the scanResult AND a is also registered as a landmark. For more information on registering
	 *         landmarks, please see the Preferences file
	 * @throws Exception
	 *             Throws an Exception if the JSONArray contains invalid values
	 */
	private LinkedHashMap<String, Double> parse(JSONArray scanResult) throws Exception {
		// timing parameters, time window can be changed in the preferences file
		double curTime = System.currentTimeMillis() / 1000.0d; // in sec.
		double startTime = curTime - params.getTimeWindow(); // in sec.

		LinkedHashMap<String, Double> observations = new LinkedHashMap<String, Double>();
		LinkedHashMap<String, Integer> observationCount = new LinkedHashMap<String, Integer>();

		for (int i = 0; i < scanResult.length(); i++) {
			Log.v(TAG, "Item: "+scanResult.get(i).toString());
			Datapoint dp = new Gson().fromJson(scanResult.get(i).toString(), Datapoint.class);
			WiFiDatapoint wdp = new Gson().fromJson(dp.getValue(), WiFiDatapoint.class);

			// time of observation
			double date = dp.getDate() / 1000.0d;	// in sec.

			// Log.i(TAG, "datapoint time: " + date);
			// Log.i(TAG, "allowed time: " + startTime);
			// Log.i(TAG, "timewindow: " + params.getTimeWindow());

			if (date > startTime) { 												// check time consistency
				if (wdp.getRSSI() < 0.0d && wdp.getRSSI() > params.getMinRSSI()) { 	// check for feasibility of RSS value
					// String key = wdp.getSSID();
					String key = wdp.getBSSID();
					if (params.getLandmarks().containsKey(key)) { 					// check if measurement is from a known landmark

						String uniqueString = wdp.getBSSID();
						// String uniqueString = wdp.getSSID();

						double RSSI = wdp.getRSSI();
						// Log.i(TAG, "using landmark: '" + key + "' with RSSI: " + RSSI);

						if (observations.containsKey(uniqueString)) {
							double value = observations.get(uniqueString) + RSSI;
							int count = observationCount.get(uniqueString) + 1;

							observations.put(uniqueString, value);
							observationCount.put(uniqueString, count);
						} else {
							observations.put(uniqueString, RSSI);
							observationCount.put(uniqueString, 1);
						}
					}
				}
			}
		}

		// compute average over measurements.
		for (Entry<String, Double> entry : observations.entrySet()) {
			String key = entry.getKey();
			observations.put(key, ((double) entry.getValue() / observationCount.get(key)));
		}

		return observations;
	}

	/**
	 * This method applies the PathLossModel on each item in the vector
	 * 
	 * <p>
	 * NB. This method assumes each item in the RSSI vector is feasible. Please remove any invalid RSSI values before calling this function.
	 * </p>
	 * 
	 * @param RSSI
	 *            A vector containing RSSI values
	 * @return
	 * 
	 */
	private double[] computeDistances(double[] RSSI) {
		double[] distances = new double[RSSI.length];

		for (int i = 0; i < distances.length; i++) {
			distances[i] = PathLossModel.computeDistance(RSSI[i]);
		}

		return distances;
	}

	/**
	 * Compute Device location
	 * 
	 * This method computes the location of the mobile device based on the scanResult
	 * 
	 * @param scanResult
	 *            The scanResult should be a CommonSense JSONArray with values and dates, where the values should be of the type WiFiDatapoint. Dates should be of type double.
	 */
	public double[] computeLocation(JSONArray scanResult) {
		double[] deviceLocation = new double[2];
		double[] pos = new double[2];
		try {
			LinkedHashMap<String, Double> observation = parse(scanResult);

			pos = findBestAP(observation);

			// double[] RSSI = map2Array(observation);
			// double[] distances = computeDistances(RSSI);
			// int[][] landmarks = params.getLandmarks(observation);
			//
			// CircularLateration cl = new CircularLateration(landmarks);
			// deviceLocation = cl.computeLocation(distances);

		} catch (Exception e) {
			Log.e(TAG, "Error parsing scanResult: " + scanResult);
			e.printStackTrace();
		}

		// return deviceLocation;
		return pos;
	}

	private double[] map2Array(LinkedHashMap<String, Double> map) {
		double[] result = new double[map.size()];

		int i = 0;
		for (Entry<String, Double> entry : map.entrySet()) {
			result[i] = entry.getValue();
			i++;
		}

		return result;
	}

	private double[] findBestAP(LinkedHashMap<String, Double> observation) {

		double best = Double.NEGATIVE_INFINITY;
		String key = "";

		for (Entry<String, Double> entry : observation.entrySet()) {
			Log.i(TAG, "value: " + entry.getValue());
			if (entry.getValue() > best) {
				best = entry.getValue();
				key = entry.getKey();
			}
		}

		if (best == Double.NEGATIVE_INFINITY) {
			double[] pos = { -1, -1 };
			return pos;
		}

		double x = params.getLandmarks().get(key)[0];
		double y = params.getLandmarks().get(key)[1];
		double[] pos = { x, y };
		return pos;
	}
}
