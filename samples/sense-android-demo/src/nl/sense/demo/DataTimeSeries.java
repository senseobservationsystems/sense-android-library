package nl.sense.demo;

import java.util.ArrayList;

import org.json.JSONObject;

import android.util.Log;
import nl.sense_os.platform.SensePlatform;
import nl.sense_os.service.shared.DataProcessor;
import nl.sense_os.service.shared.SensorDataPoint;

public class DataTimeSeries implements DataProcessor {

	protected SensePlatform sensePlatform_;
	protected String sensorName_;
	protected String sensorDescription_;
	protected long historySize_;
	protected ArrayList<JSONObject> history_ = new ArrayList<JSONObject>();

	public DataTimeSeries (SensePlatform platform, String sensorName, String description, long historySize) {
		sensePlatform_ = platform;
		sensorName_ = sensorName;
		sensorDescription_ = description;
		historySize_ = historySize;
		subscribe();
	}
	
	private void subscribe() {
		sensePlatform_.getService().getSenseService().subscribeDataProcessor(sensorName_, this);
	}
	@Override
	public void startNewSample() {}

	@Override
	public boolean isSampleComplete() {
		return false;
	}

	@Override
	public void onNewData(SensorDataPoint dataPoint) {
		//check the input type
				if (dataPoint.sensorName.compareTo(sensorName_) != 0) return;
				if (sensorDescription_ != null && dataPoint.sensorDescription.compareTo(sensorDescription_) != 0) return;
				
				//convert to JSONObject
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
						
						Log.v("DataTimeSeries", "Ai, unknown data type");
						//ignore this data point...
						throw new Exception("Unexpected data type");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				synchronized(history_) {
				history_.add(json);
				
				while (history_.size() > historySize_) {
					history_.remove(0);
				}
				}
	}
	
	public JSONObject getLastDataPoint() {
		synchronized(history_) {
		if (history_.isEmpty())
			return null;
		return history_.get(history_.size()-1);
		}
	}
	
	public ArrayList<JSONObject> getHistory() {
		return new ArrayList<JSONObject>(history_);
	}
}
