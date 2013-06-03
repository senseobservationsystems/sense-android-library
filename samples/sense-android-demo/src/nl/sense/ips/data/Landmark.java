package nl.sense.ips.data;

public class Landmark {

	private String ssid;
	private String bssid;
	private int x;
	private int y;

	public Landmark(String ssid, String bssid, int x, int y) {
		this.ssid = ssid;
		this.bssid = bssid;
		this.x = x;
		this.y = y;
	}

	public String getSsid() {
		return ssid;
	}

	public String getBssid() {
		return bssid;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

}
