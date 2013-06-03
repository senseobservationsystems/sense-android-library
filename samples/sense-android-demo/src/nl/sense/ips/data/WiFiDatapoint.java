package nl.sense.ips.data;

/** Simple WiFi Datapoint */
public class WiFiDatapoint {

	private String ssid;
	private String bssid;
	private int frequency;
	private int rssi;
	private String capabilities;

	public WiFiDatapoint(String ssid, String bssid, int frequency, int rssi, String capabilities) {
		super();
		this.ssid = ssid;
		this.bssid = bssid;
		this.frequency = frequency;
		this.rssi = rssi;
		this.capabilities = capabilities;
	}

	public String getSSID() {
		return ssid;
	}

	public String getBSSID() {
		return bssid;
	}

	public int getFrequency() {
		return frequency;
	}

	public int getRSSI() {
		return rssi;
	}

	public String getCapabilities() {
		return capabilities;
	}

}
