package nl.sense.ips.data;

/** Java equivalent of CommonSense JsonObject */
public class Datapoint {

	private String value;
	private double date; 	// milliseconds

	public Datapoint(String value, double date) {
		super();
		this.value = value;
		this.date = date;
	}

	public String getValue() {
		return value;
	}

	public double getDate() {
		return date;
	}

}
