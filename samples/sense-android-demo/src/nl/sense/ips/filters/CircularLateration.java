package nl.sense.ips.filters;

import Jama.Matrix;

/**
 * <b>Multilateration</b>
 * 
 * Multilateration according to Kushki et al, WLAN Positioning Systems, page 34.
 * 
 * Keep in mind that landmarks and observations list should be of the same length and order. This method assumes the observation (distances) i corresponds to landmark i.
 * 
 * @author Ruud Henken <ruud@sense-os.nl>
 * @version 1.0
 */
public class CircularLateration {

	private Matrix H;		// N*2 matrix, geographical distances between access points
	private Matrix B;		// N*1 matrix, signal strength + geographical distances

	private int[][] landmarks; // N*2 array, where N is the number of landmarks

	/**
	 * Constructor.
	 * 
	 * Landmarks (Access Point locations) should be provided as an N*2 array where N is the number of landmarks.
	 * 
	 * @param landmarks
	 */
	public CircularLateration(int[][] landmarks) {
		this.landmarks = landmarks;
		setH();
	}

	/**
	 * This method computes distances between reference points and stores it in a local matrix. Needs to be computed only once for each area.
	 * 
	 */
	private void setH() {
		double[][] vals = new double[landmarks.length - 1][2];

		for (int row = 0; row < landmarks.length - 1; row++) {
			vals[row][0] = landmarks[row + 1][0] - landmarks[0][0];
			vals[row][1] = landmarks[row + 1][1] - landmarks[0][1];
		}

		this.H = new Matrix(vals);
	}

	private void setB(double[] distances) {
		checkValidInput(distances);

		double[][] vals = new double[landmarks.length - 1][1];

		double v3 = (Math.pow(landmarks[0][0], 2) + Math.pow(landmarks[0][1], 2));

		for (int landmark = 0; landmark < landmarks.length - 1; landmark++) {
			double v1 = (Math.pow(distances[0], 2) - Math.pow(distances[landmark + 1], 2));
			double v2 = (Math.pow(landmarks[landmark + 1][0], 2) + Math.pow(landmarks[landmark + 1][1], 2));

			vals[landmark][0] = v1 + v2 - v3;
		}

		this.B = new Matrix(vals).times(0.5).copy();
	}

	/**
	 * This method runs some validity checks on the data.
	 * 
	 * @param distances
	 *            N*2 array of distances to Access Points
	 * @throws This
	 *             method throws an Exception if the provided distances array does not meet the specified criteria
	 */
	private void checkValidInput(double[] distances) {
		// check for minimum of observations
		if (distances.length < 3) {
			throw new IllegalArgumentException("Found length: " + distances.length + ". Should be at least 3");
		}

		// check for negative path length
		for (int i = 0; i < distances.length; i++) {
			if (distances[i] < 0) {
				throw new IllegalArgumentException("Found negative path length");
			}
		}
	}

	/**
	 * <b>Compute Device Location.</b>
	 * 
	 * 
	 * <p>
	 * This method computes the device location based on circular lateration with N number of references points.
	 * </p>
	 * 
	 * @param distances
	 *            The distances array should be of size N (equal to the number of landmarks)
	 * @return This method returns an array of size 2 which contains respectively an x and y location.
	 */
	public double[] computeLocation(double[] distances) {
		setB(distances);
		return ((this.H.transpose().times(this.H)).inverse()).times(this.H.transpose()).times(this.B).transpose().getColumnPackedCopy();
	}
}