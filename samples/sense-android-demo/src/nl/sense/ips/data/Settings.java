package nl.sense.ips.data;


public class Settings {

	private int numDatapoints = 35;
	private double timeWindow = 60.0d; // in seconds
	private int minRSSI = -70; // in dB
	private String landmarks;

	public Settings(int numDatapoints, double timeWindow, int minRSSI, String landmarks) {
		this.numDatapoints = numDatapoints;
		this.timeWindow = timeWindow;
		this.minRSSI = minRSSI;
		this.landmarks = landmarks;
	}

	public int getNumDatapoints() {
		return numDatapoints;
	}

	public double getTimeWindow() {
		return timeWindow;
	}

	public int getMinRSSI() {
		return minRSSI;
	}

	public String getLandmarks() {
		return landmarks;
	}

}
