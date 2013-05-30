package nl.sense.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import nl.sense_os.platform.SensePlatform;
import nl.sense_os.service.shared.DataProcessor;
import nl.sense_os.service.shared.SensorDataPoint;

public abstract class AggregateData<BinType extends Comparable<? super BinType>> implements DataProcessor {
	class DataValue<InnerBinType extends Comparable<? super BinType>> {
		private long n=0;
		private double mean=0;
		private InnerBinType bin;
		
		public DataValue(InnerBinType bin) {
			this.bin = bin;
		}

		public void add(double x) {
			n = n + 1;
			double delta = x - mean;
			mean = mean + delta/n;
		}

		public long size() {return n;}
		public double mean() {return mean;}
		public double sum() {return mean * n;}
		public InnerBinType bin() {return bin;}
		
		public String toString() {
			return ""+bin.toString()+": n="+n+", mean="+mean+", sum="+sum();
		}
	}

	protected HashMap<BinType, DataValue<BinType>> values_ = new HashMap<BinType, DataValue<BinType>>();
	protected String sensorName_;
	protected String sensorDescription_;
	protected boolean isComplete = false;
	protected SensePlatform sensePlatform_;
	public final String TAG = "AggregateData"; 

	public AggregateData (SensePlatform platform, String sensorName, String description) {
		sensePlatform_ = platform;
		sensorName_ = sensorName;
		sensorDescription_ = description;
		
		subscribe();
	}
	
	private void subscribe() {
		sensePlatform_.getService().getSenseService().subscribeDataProcessor(sensorName_, this);
	}
	
	public void importData() {
        new Thread() {
            public void run() {
                JSONArray data;
                try {
                    data = sensePlatform_.getLocalData(sensorName_, 10000);
                    //fill motion with old data
                    Log.v(TAG, "Importing "+data.length()+" data points.");
                    for (int i=0; i < data.length(); i++) {
                		aggregateSensorDataPoint(data.getJSONObject(i));
                    }
                } catch (IllegalStateException e) {
                    Log.w(TAG, "Failed to query remote data", e);
                } catch (JSONException e) {
                    Log.w(TAG, "Failed to parse remote data", e);
                }
            };
        }.start();
	}

	@Override
	public void startNewSample() {
		isComplete = false;
	}

	@Override
	public boolean isSampleComplete() {
		return isComplete;
	}

	@Override
	public void onNewData(SensorDataPoint dataPoint) {
		isComplete = true;

		//check the input type
		if (dataPoint.sensorName.compareTo(sensorName_) != 0) return;
		if (sensorDescription_ != null && dataPoint.sensorDescription.compareTo(sensorDescription_) != 0) return;
		
		//call the method that aggregates the data
		JSONObject json = new JSONObject();
		try {
			json.put("date", dataPoint.timeStamp);
			String key = "value";
			switch (dataPoint.getDataType()) {
			case ARRAYLIST:
				json.put(key, dataPoint.getArrayValue());
				break;
			case BOOL:
				json.put(key, dataPoint.getBoolValue());
				break;
			case DOUBLE:
				json.put(key, dataPoint.getDoubleValue());
				break;
			case FLOAT:
				json.put(key, dataPoint.getFloatValue());
				break;
			case INT:
				json.put(key, dataPoint.getIntValue());
				break;
			case JSON:
				json.put(key, dataPoint.getJSONValue());
				break;
			case STRING:
				json.put(key, dataPoint.getStringValue());
				break;
			case FILE:
			case JSONSTRING:
			case SENSOREVENT:
			default:
				
				Log.v(TAG, "Ai, unknown data type");
				//ignore this data point...
				throw new Exception("Unexpected data type");
			}
			
			aggregateSensorDataPoint(json);
			
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Aggregate a sensor data point.
	 * This abstract function is responsible for putting data into the AggregateData object.
	 */
	abstract public void aggregateSensorDataPoint(JSONObject dataPoint);

	public void addBinValue(BinType bin, double value) {
		DataValue<BinType> v = values_.get(bin);
		if (v == null) {
			v = new DataValue<BinType>(bin);
			values_.put(bin, v);
		}
		v.add(value);
	}
	
	public ArrayList<DataValue<BinType>> getSorted() {
		ArrayList<BinType> bins = new ArrayList<BinType>(values_.keySet());
		Collections.sort(bins);
		ArrayList<DataValue<BinType>> values = new ArrayList<DataValue<BinType>>(bins.size());
		
		for (BinType bin : bins) {
			values.add(values_.get(bin));
		}
		return values;
	}
}
