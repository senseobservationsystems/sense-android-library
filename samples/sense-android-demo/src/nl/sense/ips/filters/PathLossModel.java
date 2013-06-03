package nl.sense.ips.filters;

/**
 * <b>Path Loss Model</b>
 * 
 * This class is a path loss model implementation to translate Received signal strength indication (RSSI) values (in dB) into distances (in m).
 * 
 * @author Ruud Henken <ruud@sense-os.nl>
 * @version 1.0
 */
public class PathLossModel {

	private static double MAXRSS = -70;
	private static double MAXDISTANCE = 25.0d;

	/**
	 * gamma should be between 2 and 4 for indoor applications and < 2 for free space
	 */
	private static double gamma = 4.0d; // original value
	// private static double gamma = 2.5d;

	/**
	 * reference distance (in m)
	 */
	private static double d0 = 20.0d; // original value
	// private static double d0 = 20.0d;

	/**
	 * power transmitted (in dB) by AP
	 */
	private static double Pt = -40.0d; // original value

	// private static double Pt = -25.0d;

	/**
	 * This method computes the distance to an AP (in m) given an RSS value
	 * 
	 * Typical RSS values for indoor applications range from -30 to -90 dB.
	 * 
	 * @param RSS
	 * @return
	 */
	public static double computeDistance(double RSS) {
		if (RSS < MAXRSS) {
			return MAXDISTANCE;
		}

		if (RSS > 0) {
			throw new IllegalArgumentException("Invalid RSS value. Number is too damn high!");
		}

		// method to compute constant AP value
		double K = -20.0 * Math.log10((4 * Math.PI * d0) / gamma);

		// computed distance (in m)
		return d0 * Math.exp((RSS - Pt - K) * (-1.0d / (10.0d * gamma)) * (Math.log(2) + Math.log(5)));
	}

	/**
	 * setter for gamma
	 * 
	 * @param gamma
	 */
	public void setGamma(double gamma) {
		PathLossModel.gamma = gamma;
	}

	/**
	 * setter for reference distance
	 * 
	 * @param d0
	 */
	public void setReferenceDistance(double d0) {
		PathLossModel.d0 = d0;
	}

	/**
	 * setter for transmitted power
	 * 
	 * @param Pt
	 */
	public void setPowerTransmitted(double Pt) {
		PathLossModel.Pt = Pt;
	}

	/**
	 * private constructor to prevent instantiation
	 */
	private PathLossModel() {
	}
}
