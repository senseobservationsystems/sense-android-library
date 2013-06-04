package nl.sense.ips.data;

/**
 * <b>Indoor Positioning System Preferences</b>
 * 
 * This class downloads preferences from CommonSense which are used for localization
 * 
 * @author Ruud Henken <ruud@sense-os.nl>
 */

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import android.util.Log;

public class Parameters {
	private static final String TAG = "Parameters";
	private int numDatapoints; // = 35;
	private double timeWindow; // = 60.0d * 60.0d * 7.0d; // in seconds
	private int minRSSI; // = -70; // in dB
	private HashMap<String, int[]> landmarks; // = new HashMap<String, int[]>();

	public Parameters() {
		String jsonString = generateJson();
		parseJson(jsonString);
	}

	/** This method returns a set of landmarks */
	public HashMap<String, int[]> getLandmarks() {
		return this.landmarks;
	}

	/** This method returns the timeWindow size */
	public double getTimeWindow() {
		return this.timeWindow;
	}

	/** This method returns the minimum RSSI value of a measurement */
	public int getMinRSSI() {
		return this.minRSSI;
	}

	public int getNumDatapoints() {
		return numDatapoints;
	}

	public int[][] getLandmarks(LinkedHashMap<String, Double> observation) {
		int[][] result = new int[observation.size()][2];

		int i = 0;
		for (Entry<String, Double> entry : observation.entrySet()) {

			Log.i(TAG, "KEY WE ARE USING: " + entry.getKey());
			result[i] = landmarks.get(entry.getKey());
			i++;
		}
		return result;
	}

	/** example preference file */
	private String generateJson() {
		JsonObject l1 = new JsonObject();
		l1.addProperty("bssid", "a0:f3:c1:cf:bb:88");
		l1.addProperty("ssid", "SENSE_AP1");
		l1.addProperty("x", 1);
		l1.addProperty("y", 1);

		JsonObject l2 = new JsonObject();
		l2.addProperty("bssid", "a0:f3:c1:c5:b0:90");
		l2.addProperty("ssid", "SENSE_AP2");
		l2.addProperty("x", 2);
		l2.addProperty("y", 1);

		JsonObject l3 = new JsonObject();
		l3.addProperty("bssid", "a0:f3:c1:cf:ab:8a");
		l3.addProperty("ssid", "SENSE_AP3");
		l3.addProperty("x", 3);
		l3.addProperty("y", 1);

		JsonObject l4 = new JsonObject();
		l4.addProperty("bssid", "a0:f3:c1:c5:b0:a6");
		l4.addProperty("ssid", "SENSE_AP4");
		l4.addProperty("x", 4);
		l4.addProperty("y", 1);

		JsonObject l5 = new JsonObject();
		l5.addProperty("bssid", "a0:f3:c1:cf:ab:76");
		l5.addProperty("ssid", "SENSE_AP5");
		l5.addProperty("x", 5);
		l5.addProperty("y", 1);

		JsonObject l6 = new JsonObject();
		l6.addProperty("bssid", "A0:F3:C1:D4:16:50");
		l6.addProperty("ssid", "SENSE_AP6");
		l6.addProperty("x", 6);
		l6.addProperty("y", 1);

		JsonObject l7 = new JsonObject();
		l7.addProperty("bssid", "A0:F3:C1:D4:16:24");
		l7.addProperty("ssid", "SENSE_AP7");
		l7.addProperty("x", 7);
		l7.addProperty("y", 1);

		JsonObject l8 = new JsonObject();
		l8.addProperty("bssid", "64:70:02:EB:2E:14");
		l8.addProperty("ssid", "SENSE_AP8");
		l8.addProperty("x", 8);
		l8.addProperty("y", 1);

		JsonArray landmarks = new JsonArray();
		landmarks.add(l1);
		landmarks.add(l2);
		landmarks.add(l3);
		landmarks.add(l4);
		landmarks.add(l5);
		landmarks.add(l6);
		landmarks.add(l7);
		landmarks.add(l8);
		
		JsonObject prefs = new JsonObject();
		prefs.addProperty("numDatapoints", 100);
		prefs.addProperty("timeWindow", 30 * 1); // in sec.
		prefs.addProperty("minRSSI", -70); // in dB.
		prefs.addProperty("landmarks", landmarks.toString());

		return prefs.toString();
	}

	/** parse preferences file */
	private void parseJson(String jsonString) {
		Settings s = new Gson().fromJson(jsonString, Settings.class);
		JsonArray landmarks = (JsonArray) new JsonParser().parse(s.getLandmarks());

		HashMap<String, int[]> landmarkList = new HashMap<String, int[]>();
		for (int i = 0; i < landmarks.size(); i++) {
			Landmark l = new Gson().fromJson(landmarks.get(i), Landmark.class);
			int[] location = { l.getX(), l.getY() };
			landmarkList.put(l.getBssid(), location);
		}

		// set data members
		this.numDatapoints = s.getNumDatapoints();
		this.minRSSI = s.getMinRSSI();
		this.timeWindow = s.getTimeWindow();
		this.landmarks = landmarkList;

		// // now lets see the class values
		// Log.i(TAG, "now lets see class values");
		// // DEBUG
		// Log.i(TAG, "minRSS: " + this.getMinRSSI());
		// Log.i(TAG, "numDatapoints: " + this.getNumDatapoints());
		// Log.i(TAG, "timewindow: " + this.getTimeWindow());
		//
		// // DEBUG
		// for (Entry<String, int[]> entry : this.landmarks.entrySet()) {
		// Log.i(TAG, "bssid: " + entry.getKey());
		// Log.i(TAG, "x: " + entry.getValue()[0] + ", y: " + entry.getValue()[1]);
		// Log.i(TAG, "");
		// }
	}
}
