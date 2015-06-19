package nl.sense_os.service.phonestate;

import java.util.ArrayList;
import java.util.Iterator;

import android.util.Log;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.SensorDataPoint;

/**
 * Provides filter functionality for the proximity sensor. Faulty measurements are
 * not uncommon in the proximity sensor of some Android devices. These errors usually show up as
 * single peaks in the data. This is undesired behaviour since the sleep time tracker is dependent on 
 * this sensor. 
 * This class filters only single peaks in the measurements. 
 * 
 * @author ronald@sense-os.nl
 *
 */
public class ProximityFilter {

	private static final String TAG = ProximityFilter.class.getSimpleName();

	private static ArrayList<SensorDataPoint> cache;
	private static ProximityFilter instance;
	private long CACHE_TIME = 15 * 60 * 1000l; //default cache time.

	/**
	 * 
	 * @return singleton instance of ProximityFilter
	 * 
	 */
	public static ProximityFilter getInstance() {

		if (null == instance) {
			cache = new ArrayList<SensorDataPoint>();
			instance = new ProximityFilter();
			return instance;
		}
		return instance;
	}
	
	/**
	 * Set the cache time for filtering. If the time between two
	 * measurements is bigger than this value, no filtering will be performed
	 * on those measurements. 
	 * @param cacheTime
	 */
	public void setCacheTime(long cacheTime){
		
		CACHE_TIME = cacheTime;
	}

	private ProximityFilter() {

	}

	/**
	 * Takes the latest dataPoint of the proximity sensor and returns
	 * a list of filtered dataPoints. Single peaks are filtered out of the
	 * sequence. In a sequence of equal results, the filter will return the 
	 * input dataPoint immediately. When two dataPoints with different result are given,
	 * no dataPoint is returned. The will return 2 dataPoints upon next call,
	 * either filtering a single peak out or 
	 * 
	 * @param dataPoint
	 *            new proximity sensor datapoint 
	 * @return list of filtered datapoints. Can be 0, 1 or 2 datapoints,
	 *         depending on the sequence of proximity results.
	 */
	public Iterator<SensorDataPoint> filter(SensorDataPoint dataPoint) {

		// no cache yet. set cache and return current datapoint.
		if (cache.size() == 0) {
			cache.add(dataPoint);
			ArrayList<SensorDataPoint> res = new ArrayList<SensorDataPoint>();
			res.add(dataPoint);
			return res.iterator();
		}

		// current datapoint has a different value as cached point.
		// add current datapoint to cache. wait for next point to see if
		// its a single peak.
		// return no datapoints.
		if (cache.size() == 1) {
			SensorDataPoint lastDatapoint = cache.get(0);

			
			if (lastDatapoint.getFloatValue() != dataPoint.getFloatValue()
					&& lastDatapoint.timeStamp + CACHE_TIME > dataPoint.timeStamp) {
				
				cache.add(dataPoint);
				ArrayList<SensorDataPoint> res = new ArrayList<SensorDataPoint>();
				return res.iterator();		
				
			// current datapoint has the same value as cached.
			// replace cache and return current datapoint.
			} else {
				cache.clear();
				cache.add(dataPoint);

				ArrayList<SensorDataPoint> res = new ArrayList<SensorDataPoint>();
				res.add(dataPoint);
				return res.iterator();

			}
		}

		else {
			SensorDataPoint lastDatapoint = cache.get(1);
		
			// single peak detected. change previous value accordingly and
			// replace cache with current datapoint. return current and
			// previous datapoint.
			if (lastDatapoint.getFloatValue() != dataPoint.getFloatValue()
					&& lastDatapoint.timeStamp + CACHE_TIME > dataPoint.timeStamp) {
				
				cache.clear();
				cache.add(dataPoint);

				lastDatapoint.setFloatValue((float) dataPoint.getFloatValue());

				ArrayList<SensorDataPoint> res = new ArrayList<SensorDataPoint>();
				res.add(lastDatapoint);
				res.add(dataPoint);
				return res.iterator();
				
			// no single peak detected. return current and previous datapoint.
			// replace cache with current datapoint.	
			} else {
				
				cache.clear();
				cache.add(dataPoint);

				ArrayList<SensorDataPoint> res = new ArrayList<SensorDataPoint>();
				res.add(lastDatapoint);
				res.add(dataPoint);
				return res.iterator();
			}
		}
	}
}
