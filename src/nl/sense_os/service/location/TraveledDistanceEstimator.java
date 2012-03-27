package nl.sense_os.service.location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;

import android.location.Location;

/***
 * This class can be used to estimate traveled distance over a location trace,
 * this class is especially suited to deal with sparse and/or noisy points.
 * 
 * Implementation: 
 * (outlier filter)
 *  - first apply a median filter to remove points with a relative bad accuracy
 *  - second filter points for which we need an infeasible speed, add them to a blacklist
 *  - third filter points that are in the blacklist; this is needed since sometimes there are
 *   rogue network points that don't get filtered when the number of points is scarce
 * (cluster)
 *  - cluster points that are within each other accuracy, the cluster points is estimated as the
 *   weighted average of the points, with weights inversely proportional to the accuracies
 * 
 * @author pim
 */

public class TraveledDistanceEstimator {
	/**
	 * private class to store locations, each variable in the class is
	 * modifiable as opposed to the android Location class
	 */
	private class PositionPoint {
		double timestamp;
		double lat;
		double lon;
		double acc = Double.MAX_VALUE;

		PositionPoint(double ts, double lat, double lon, double acc) {
			timestamp = ts;
			this.lat = lat;
			this.lon = lon;
			this.acc = acc;
		}

		public PositionPoint(PositionPoint p) {
			timestamp = p.timestamp;
			lat = p.lat;
			lon = p.lon;
			acc = p.acc;
		}

		public PositionPoint(Location p) {
			timestamp = p.getTime();
			lat = p.getLatitude();
			lon = p.getLongitude();
			if (p.hasAccuracy())
				acc = p.getAccuracy();
		}

		/*
		 * Note, equals and hashCode ignore the timestamp as that one is
		 * irrelevant for the medianFilter. A bit ugly but hey, it's a private
		 * class
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof PositionPoint) {
				PositionPoint pp = (PositionPoint) obj;
				return lat == pp.lat && lon == pp.lon && acc == pp.acc;
			} else
				return false;
		}

		@Override
		public int hashCode() {
			return new Double(lat).hashCode() ^ new Double(lon).hashCode()
					^ new Double(acc).hashCode();
		}
	}

	// parameters of the median filter, accept values within
	// MEDIAN_FILTER_FACTOR * ValueAtQuantile over the last WINDOW_SIZE points
	final double WINDOW_SIZE = 7;
	private static final double MEDIAN_FILTER_FACTOR = 2;
	private static final double MEDIAN_FILTER_QUANTILE = 0.5;
	// parameters of the blacklisting
	private static final double BLACKLIST_TIMEOUT = 3 * 3600;
	private static final double MIN_DISTANCE = 30;
	private static final int CLEANUP_SIZE = 100; // number of entries in the
													// blacklist that triggers a
													// cleanup

	// parameters for the algorithm
	final double MIN_ACCURACY = 1;
	final double MAX_SPEED = 100; // anything over 100 m/s is supposed to be
									// nonsense
	final double MIN_CLUSTER_DISTANCE = 10;
	final double ACCURACY_FACTOR = 1.2;

	// variables for online calculation
	double totalDistance_ = 0; // traveled distance
	double weightSum_ = 0; // total weight used for the current cluster
	PositionPoint clusterCenter_;
	PositionPoint lastClusterCenter_;

	ArrayList<PositionPoint> window_ = new ArrayList<PositionPoint>();
	Hashtable<PositionPoint, Double> blackList_ = new Hashtable<PositionPoint, Double>();

	// boolean to indicate whether getDistance included the distance from the
	// last cluster so we won't count it multiple times
	boolean skipLastDistance = false;

	public TraveledDistanceEstimator() {
	}

	/***
	 * Reset estimated distance, keep estimating from the last added location.
	 * If you don't want this, create a new instance instead.
	 */
	synchronized public void reset() {
		lastClusterCenter_ = null;
		totalDistance_ = 0;
	}

	private double getAccuracyAtQuantile(double p) {
		// order by accuracy
		ArrayList<PositionPoint> ordered = new ArrayList<PositionPoint>(window_);
		Collections.sort(ordered, new Comparator<PositionPoint>() {

			@Override
			public int compare(PositionPoint o1, PositionPoint o2) {
				return Double.compare(o1.acc, o2.acc);
			}
		});

		int idxQuantile = (int) (Math.min(window_.size(), WINDOW_SIZE) * p);
		return ordered.get(idxQuantile).acc;
	}

	synchronized public void addPoint(final Location l) {
		PositionPoint pp = new PositionPoint(l);

		/* Median filter for accuracy outliers */
		window_.add(pp);
		while (window_.size() > WINDOW_SIZE) {
			window_.remove(0);
		}

		if (pp.acc > MEDIAN_FILTER_FACTOR
				* getAccuracyAtQuantile(MEDIAN_FILTER_QUANTILE))
			return; // reject as outlier, but don't blacklist

		/* speed check */
		if (clusterCenter_ != null) {
			double distance = distanceBetween(clusterCenter_, pp);
			double dt = pp.timestamp - clusterCenter_.timestamp;
			if (distance > dt * MAX_SPEED + clusterCenter_.acc + pp.acc
					+ MIN_DISTANCE) {
				blackList(pp); // blacklist, so we can avoid this point in the
								// future
				return; // reject as outlier
			}
		}

		/* check blacklist */
		if (isBlackListed(pp)) {
			blackList(pp); // and update timestamp of this entry
			return;
		}

		/* passed all checks, process the point */
		processPoint(pp);
	}

	private boolean isBlackListed(PositionPoint p) {
		Double timestamp = blackList_.get(p);
		if (timestamp == null)
			return false;
		if (p.timestamp - timestamp > BLACKLIST_TIMEOUT)
			return false;

		return true;
	}

	private void blackList(PositionPoint p) {
		blackList_.put(p, p.timestamp);

		// clean up
		if (blackList_.size() > CLEANUP_SIZE) {
			ArrayList<PositionPoint> deletes = new ArrayList<TraveledDistanceEstimator.PositionPoint>();
			for (PositionPoint key : blackList_.keySet()) {
				if (p.timestamp - key.timestamp > BLACKLIST_TIMEOUT)
					deletes.add(key);
			}
			for (PositionPoint key : deletes)
				blackList_.remove(key);
		}
	}

	private void processPoint(final PositionPoint p) {
		// ugly hack, instead also the first point should be filtered
		if (clusterCenter_ == null) {
			// TODO: also filter first points... For now just create a new
			// cluster at the first point
			clusterCenter_ = new PositionPoint(p);
		}
		// calculate distance between center and new point
		double distance = distanceBetween(clusterCenter_, p);

		// determine whether the point is part of a new cluster
		if (distance > (clusterCenter_.acc + p.acc) * ACCURACY_FACTOR
				+ MIN_CLUSTER_DISTANCE) {
			if (lastClusterCenter_ != null) {
				if (skipLastDistance == false)
					totalDistance_ += distanceBetween(lastClusterCenter_,
							clusterCenter_);
				skipLastDistance = false;
			}
			lastClusterCenter_ = new PositionPoint(clusterCenter_);
			weightSum_ = 0;
			clusterCenter_ = new PositionPoint(p);
		}

		// update center of this cluster
		updateCenter(new PositionPoint(p));
	}

	synchronized public double getTraveledDistance() {
		//
		double lastDistance = 0;
		if (lastClusterCenter_ != null && skipLastDistance == false) {
			lastDistance = distanceBetween(clusterCenter_, lastClusterCenter_);
			skipLastDistance = true;
		}

		return totalDistance_ + lastDistance;
	}

	/***
	 * update center position using the location p
	 * 
	 */
	private void updateCenter(PositionPoint p) {
		/*
		 * center is a weighted average of the given position points, the
		 * weights are inversely proportional to the accuracy. To avoid problems
		 * with infinite weight a minimum accuracy is defined. This is an online
		 * calculation.
		 */

		// restore weighted version of the current center
		double lat = clusterCenter_.lat * weightSum_, lon = clusterCenter_.lon
				* weightSum_;
		// accuracy isn't weighted
		double acc = clusterCenter_.acc;
		// weight of this position is inversely proportional to the accuracy
		double weight = 1.0 / (Math.max(p.acc, MIN_ACCURACY));
		// accumulate weight so we can normalize the weight later
		weightSum_ += weight;
		// update values
		lat += weight * p.lat;
		lon += weight * p.lon;
		lat /= weightSum_;
		lon /= weightSum_;
		// accuracy of averaged position is a bit harder to calculate, we'll
		// just (over)estimate it with the minimum accuracy
		acc = Math.min(acc, p.acc);

		// update center, just to be sure create a new object
		clusterCenter_ = new PositionPoint(p.timestamp, lat, lon, acc);
	}

	/* some convenient methods to calculate distances between two points */
	@SuppressWarnings("unused")
	private static double distanceBetween(double lat1, double lon1,
			double lat2, double lon2) {
		float[] result = new float[1];
		Location.distanceBetween(lat1, lon1, lat2, lon2, result);
		return result[0];
	}

	private static double distanceBetween(PositionPoint p1, PositionPoint p2) {
		float[] result = new float[1];
		Location.distanceBetween(p1.lat, p1.lon, p2.lat, p2.lon, result);
		return result[0];
	}
}