package nl.sense_os.datastorageengine.test;

import android.test.AndroidTestCase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import nl.sense_os.datastorageengine.DataDeletionRequest;
import nl.sense_os.datastorageengine.DataPoint;
import nl.sense_os.datastorageengine.DatabaseHandlerException;
import nl.sense_os.datastorageengine.QueryOptions;
import nl.sense_os.datastorageengine.SensorException;
import nl.sense_os.datastorageengine.SensorProfileException;
import nl.sense_os.datastorageengine.DatabaseHandler;
import nl.sense_os.datastorageengine.realm.RealmDataPoint;
import nl.sense_os.datastorageengine.realm.RealmSensor;
import nl.sense_os.datastorageengine.Sensor;
import nl.sense_os.datastorageengine.SensorOptions;

public class TestDatabaseHandler extends AndroidTestCase {

    protected Realm realm;
    private RealmConfiguration testConfig;
    private DatabaseHandler databaseHandler;
    private String userId = "userId";
    String newUserId = "newUserId";
    private Sensor sensor;
    private Date dateType;
    private SensorOptions sensorOptions;
    private DatabaseHandler newDatabaseHandler;

    @Override
    protected void setUp () throws Exception {

        testConfig = new RealmConfiguration.Builder(getContext()).build();
        Realm.deleteRealm(testConfig);
        realm = Realm.getInstance(testConfig);

        databaseHandler = new DatabaseHandler(getContext(), userId);
        newDatabaseHandler = new DatabaseHandler(getContext(), newUserId);
    }

    @Override
    protected void tearDown () throws Exception {
        realm.close();
    }

    /****Unit tests of  Sensor Class****/

    public void testDifferentUserIdWithSameSensorAndSourceName() throws JSONException, DatabaseHandlerException, SensorException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        Sensor newSensor = newDatabaseHandler.createSensor(sourceName,sensorName,sensorOptions);

    }

    public void testInsertDataPointSucceededWithIntegerValue() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        int value = 0;
        long date = dateType.getTime();
        int numberOfDataPoints = 0;

        sensor.insertOrUpdateDataPoint(value,date);
        numberOfDataPoints++;

        RealmResults<RealmDataPoint> resultList= realm.where(RealmDataPoint.class)
                                                      .equalTo("date", date)
                                                      .findAll();
        int listSize = resultList.size();
        assertEquals("Incorrect number of the Realm DataPoint object", numberOfDataPoints, listSize);

        DataPoint resultDataPoint = RealmDataPoint.toDataPoint(resultList.first());
        assertEquals("Incorrect value of the Realm DataPoint object", value, resultDataPoint.getValueAsInteger());
    }

    public void testInsertDataPointSucceededWithFloatValue() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        float value = 2.30f;
        long date = dateType.getTime();
        int numberOfDataPoints = 0;

        sensor.insertOrUpdateDataPoint(value,date);
        numberOfDataPoints++;

        RealmResults<RealmDataPoint> resultList= realm.where(RealmDataPoint.class)
                .equalTo("date", date)
                .findAll();
        int listSize = resultList.size();
        assertEquals("Incorrect number of the Realm DataPoint object", numberOfDataPoints, listSize);

        DataPoint resultDataPoint = RealmDataPoint.toDataPoint(resultList.first());
        assertEquals("Incorrect value of the Realm DataPoint object", value, resultDataPoint.getValueAsFloat());
    }

    public void testInsertDataPointSucceededWithStringValue() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "sleep_time";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        String value = "Hi, Datapoint";
        long date = dateType.getTime();
        int numberOfDataPoints = 0;

        sensor.insertOrUpdateDataPoint(value,date);
        numberOfDataPoints++;

        RealmResults<RealmDataPoint> resultList= realm.where(RealmDataPoint.class)
                .equalTo("date", date)
                .findAll();
        int listSize = resultList.size();
        assertEquals("Incorrect number of the Realm DataPoint object", numberOfDataPoints, listSize);

        DataPoint resultDataPoint = RealmDataPoint.toDataPoint(resultList.first());
        assertEquals("Incorrect value of the Realm DataPoint object", value, resultDataPoint.getValueAsString());
    }

    public void testInsertDataPointSucceededWithBooleanValue() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "screen activity";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        boolean value = false;
        long date = dateType.getTime();
        int numberOfDataPoints = 0;

        sensor.insertOrUpdateDataPoint(value,date);
        numberOfDataPoints++;

        RealmResults<RealmDataPoint> resultList= realm.where(RealmDataPoint.class)
                .equalTo("date", date)
                .findAll();
        int listSize = resultList.size();
        assertEquals("Incorrect number of the Realm DataPoint object", numberOfDataPoints, listSize);

        DataPoint resultDataPoint = RealmDataPoint.toDataPoint(resultList.first());
        assertEquals("Incorrect value of the Realm DataPoint object", value, resultDataPoint.getValueAsBoolean());
    }

    public void testInsertDataPointSucceededWithJsonValue() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "accelerometer";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        JSONObject value = new JSONObject();
        value.put("x", 9.1);
        value.put("y",8.9);
        value.put("z", 7.2);
        long date = dateType.getTime();
        int numberOfDataPoints = 0;

        sensor.insertOrUpdateDataPoint(value,date);
        numberOfDataPoints++;

        RealmResults<RealmDataPoint> resultList= realm.where(RealmDataPoint.class)
                .equalTo("date", date)
                .findAll();
        int listSize = resultList.size();
        assertEquals("Incorrect number of the Realm DataPoint object", numberOfDataPoints, listSize);

        DataPoint resultDataPoint = RealmDataPoint.toDataPoint(resultList.first());
        assertEquals("Incorrect value of the Realm DataPoint object", value.toString(), resultDataPoint.getValueAsJSONObject().toString());
    }

    // Insert a datapoint, and make an update of value. Realm is expected only to update the same object with new value.
    public void testInsertDatePointSucceededWithDuplicateDataPoint() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        int value = 0;
        long date = dateType.getTime();
        int numberOfDataPoints = 0;

        sensor.insertOrUpdateDataPoint(value,date);
        numberOfDataPoints++;

        value = 1;
        sensor.insertOrUpdateDataPoint(value,date);

        RealmResults<RealmDataPoint> resultList= realm.where(RealmDataPoint.class)
                                                      .equalTo("date", date)
                                                      .findAll();

        int listSize = resultList.size();
        assertEquals("Incorrect number of the Realm DataPoint object", numberOfDataPoints, listSize);

        DataPoint resultDataPoint = RealmDataPoint.toDataPoint(resultList.first());
        assertEquals("Incorrect value of the Realm DataPoint object", value, resultDataPoint.getValueAsInteger());
    }

    // Insert two datapoints with same date but with different sensorId
    public void testInsertDatePointSucceededWithTwoSensorsHavingSameDate() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        int value = 0;
        long date = dateType.getTime();
        int numberOfDataPoints = 0;

        sensor.insertOrUpdateDataPoint(value,date);
        numberOfDataPoints++;
        String newSourceName = "sony";
        String newSensorName = "light";
        sensor = databaseHandler.createSensor(newSourceName,newSensorName,sensorOptions);

        int value1 = 1;
        sensor.insertOrUpdateDataPoint(value1,date);
        numberOfDataPoints++;

        RealmResults<RealmDataPoint> resultList= realm.where(RealmDataPoint.class)
                .equalTo("date", date)
                .findAll();

        int listSize = resultList.size();
        assertEquals("Incorrect number of the Realm DataPoint object", numberOfDataPoints, listSize);

        DataPoint resultDataPoint = RealmDataPoint.toDataPoint(resultList.first());
        assertEquals("Incorrect sensorId of the Realm DataPoint object", value, resultDataPoint.getValueAsInteger());
        resultDataPoint = RealmDataPoint.toDataPoint(resultList.last());
        assertEquals("Incorrect sensorId of the Realm DataPoint object", value1, resultDataPoint.getValueAsInteger());
    }

    // Get the datapoints with asc sort order
    public void testGetDataPointsSucceededWithASC() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        int value = 0;
        long date1 = dateType.getTime();
        int numberOfDataPoints = 0;

        sensor.insertOrUpdateDataPoint(value,date1);
        numberOfDataPoints++;

        long date2 = date1 + 1000;

        sensor.insertOrUpdateDataPoint(value,date2);
        numberOfDataPoints++;

        long date3 = date2 + 1000;

        sensor.insertOrUpdateDataPoint(value,date3);
        numberOfDataPoints++;

        int limit = numberOfDataPoints - 1;
        List<DataPoint> dataPointList = sensor.getDataPoints(new QueryOptions(date1, date3+1, null, limit, QueryOptions.SORT_ORDER.ASC));

        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", limit, listSize);
        assertEquals("Incorrect data point by using ASC sorting", date1, dataPointList.get(0).getTime());
        assertEquals("Incorrect data point by using ASC sorting", date2, dataPointList.get(1).getTime());
    }

    public void testGetDataPointsSucceededWithOutOfBoundLimit() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        int value = 0;
        long date1 = dateType.getTime();
        int numberOfDataPoints = 0;

        sensor.insertOrUpdateDataPoint(value,date1);
        numberOfDataPoints++;

        long date2 = date1 + 1000;

        sensor.insertOrUpdateDataPoint(value,date2);
        numberOfDataPoints++;

        long date3 = date2 + 1000;

        sensor.insertOrUpdateDataPoint(value,date3);
        numberOfDataPoints++;

        int limit = numberOfDataPoints + 1;
        List<DataPoint> dataPointList = sensor.getDataPoints(new QueryOptions(date1, date3+1, null, limit, QueryOptions.SORT_ORDER.ASC));

        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", numberOfDataPoints, listSize);
        assertEquals("Incorrect data point by using ASC sorting", date1, dataPointList.get(0).getTime());
        assertEquals("Incorrect data point by using ASC sorting", date2, dataPointList.get(1).getTime());
    }

    public void testGetDataPointsSucceededWithNullStartTime() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        int value = 0;
        long time = dateType.getTime();
        int numberOfDataPoints = 10;

        long date[] = new long[numberOfDataPoints];

        for(int i=0; i<numberOfDataPoints; i++){
            date[i] = time + 1000 + i;
            sensor.insertOrUpdateDataPoint(value,date[i]);
        }

        int limit = 5;
        List<DataPoint> dataPointList = sensor.getDataPoints(new QueryOptions(null, date[9]+1, null, limit, QueryOptions.SORT_ORDER.ASC));

        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", limit, listSize);
        for(int i=0; i<limit; i++){
            assertEquals("Incorrect data point by using ASC sorting", date[i], dataPointList.get(i).getTime());
        }
    }

    public void testGetDataPointsSucceededWithNullEndTime() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        int value = 0;
        long time = dateType.getTime();
        int numberOfDataPoints = 10;

        long date[] = new long[numberOfDataPoints];

        for(int i=0; i<numberOfDataPoints; i++){
            date[i] = time + 1000 + i;
            sensor.insertOrUpdateDataPoint(value,date[i]);
        }

        int limit = 5;
        List<DataPoint> dataPointList = sensor.getDataPoints(new QueryOptions(date[0], null, null, limit, QueryOptions.SORT_ORDER.DESC));

        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", limit, listSize);
        for(int i=9; i>limit-1; i--){
            assertEquals("Incorrect data point by using DESC sorting", date[i], dataPointList.get(numberOfDataPoints-1-i).getTime());
        }
    }

    public void testGetDataPointsSucceededWithNullLimit() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        int value = 0;
        long time = dateType.getTime();
        int numberOfDataPoints = 10;

        long date[] = new long[numberOfDataPoints];

        for(int i=0; i<numberOfDataPoints; i++){
            date[i] = time + 1000 + i;
            sensor.insertOrUpdateDataPoint(value,date[i]);
        }

        List<DataPoint> dataPointList = sensor.getDataPoints(new QueryOptions(date[0], date[9]+1, null, null, QueryOptions.SORT_ORDER.ASC));

        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", numberOfDataPoints, listSize);
        for(int i=0; i<numberOfDataPoints; i++){
            assertEquals("Incorrect data point by using ASC sorting", date[i], dataPointList.get(i).getTime());
        }
    }

    public void testGetDataPointsSucceededWithNullParams() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        int value = 0;
        long time = dateType.getTime();
        int numberOfDataPoints = 10;

        long date[] = new long[numberOfDataPoints];

        for(int i=0; i<numberOfDataPoints; i++){
            date[i] = time + 1000 + i;
            sensor.insertOrUpdateDataPoint(value,date[i]);
        }

        List<DataPoint> dataPointList = sensor.getDataPoints(new QueryOptions(null, null, null, null, QueryOptions.SORT_ORDER.ASC));

        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", numberOfDataPoints, listSize);
        for(int i=0; i<numberOfDataPoints; i++){
            assertEquals("Incorrect data point by using ASC sorting", date[i], dataPointList.get(i).getTime());
        }
    }


    public void testGetDataPointsWithInvalidLimit() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        int value = 0;
        long date1 = dateType.getTime();

        sensor.insertOrUpdateDataPoint(value,date1);

        long date2 = date1 + 1000;

        sensor.insertOrUpdateDataPoint(value,date2);

        long date3 = date2 + 1000;

        sensor.insertOrUpdateDataPoint(value,date3);

        int limit = -1;
        try {
            sensor.getDataPoints(new QueryOptions(date1, date3 + 1, null, limit, QueryOptions.SORT_ORDER.ASC));
        }catch(DatabaseHandlerException e){
            assertEquals("Wrong DatabaseHandlerException messge", "Invalid input of limit value", e.getMessage());
        }
    }

    // Get the datapoints with desc sort order
    public void testGetDataPointsSucceededWithDESC() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        int value = 0;
        long date1 = dateType.getTime();
        int numberOfDataPoints = 0;

        sensor.insertOrUpdateDataPoint(value,date1);
        numberOfDataPoints++;

        long date2 = date1 + 1000;

        sensor.insertOrUpdateDataPoint(value,date2);
        numberOfDataPoints++;

        long date3 = date2 + 1000;

        sensor.insertOrUpdateDataPoint(value,date3);
        numberOfDataPoints++;

        int limit = numberOfDataPoints - 1;
        List<DataPoint> dataPointList = sensor.getDataPoints(new QueryOptions(date1, date3+1, null, limit, QueryOptions.SORT_ORDER.DESC));

        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", limit, listSize);
        assertEquals("Incorrect data point by using ASC sorting", date3, dataPointList.get(0).getTime());
        assertEquals("Incorrect data point by using ASC sorting", date2, dataPointList.get(1).getTime());
    }

    // Test the case that end date is before start date
    public void testGetDataPointsFailedWithReverseDates() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        int value = 0;
        long date1 = dateType.getTime();
        int numberOfDataPoints = 0;

        sensor.insertOrUpdateDataPoint(value,date1);
        numberOfDataPoints++;

        long date2 = date1 + 1000;

        sensor.insertOrUpdateDataPoint(value,date2);
        numberOfDataPoints++;

        long date3 = date2 + 1000;

        sensor.insertOrUpdateDataPoint(value,date3);
        numberOfDataPoints++;

        int limit = numberOfDataPoints - 1;
        try {
            List<DataPoint> dataPointList = sensor.getDataPoints(new QueryOptions(date3, date1, null, limit, QueryOptions.SORT_ORDER.ASC));
            int listSize = dataPointList.size();
            assertEquals("Incorrect number of data points", 0, listSize);
        }catch(DatabaseHandlerException e){
            assertEquals("Wrong DatabaseHandlerException", "startTime is the same as or later than the endTime", e.getMessage());
        }
    }

    // Delete DataPoints with Specified startTime and endTime
    public void testDeleteDataPointsSucceededWithTwoSpecifiedDates() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        int value = 0;
        long time = dateType.getTime();
        int numberOfDataPoints = 10;

        long date[] = new long[numberOfDataPoints];

        for(int i=0; i<numberOfDataPoints; i++){
            date[i] = time + 1000 + i;
            sensor.insertOrUpdateDataPoint(value,date[i]);
        }

        List<DataPoint> dataPointList = sensor.getDataPoints(new QueryOptions(null, null, null, numberOfDataPoints, QueryOptions.SORT_ORDER.ASC));
        realm.beginTransaction();
        for(DataPoint dp: dataPointList){
            dp.setExistsInRemote(true);
            realm.copyToRealmOrUpdate( RealmDataPoint.fromDataPoint(dp));
        }
        realm.commitTransaction();

        int numberToDelete = 5;
        sensor.deleteDataPoints(date[0], date[numberToDelete]);
        dataPointList = sensor.getDataPoints(new QueryOptions(null, null, null, numberOfDataPoints, QueryOptions.SORT_ORDER.ASC));
        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", numberOfDataPoints - numberToDelete, listSize);
    }

    // Delete DataPoints with null startTime
    public void testDeleteDataPointsSucceededWithNullStartTime() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        int value = 0;
        long time = dateType.getTime();
        int numberOfDataPoints = 10;

        long date[] = new long[numberOfDataPoints];

        for(int i=0; i<numberOfDataPoints; i++){
            date[i] = time + 1000 + i;
            sensor.insertOrUpdateDataPoint(value,date[i]);
        }

        List<DataPoint> dataPointList = sensor.getDataPoints(new QueryOptions(null, null, null, numberOfDataPoints, QueryOptions.SORT_ORDER.ASC));
        realm.beginTransaction();
        for(DataPoint dp: dataPointList){
            dp.setExistsInRemote(true);
            realm.copyToRealmOrUpdate( RealmDataPoint.fromDataPoint(dp));
        }
        realm.commitTransaction();

        int numberToDelete = 5;
        sensor.deleteDataPoints(null, date[numberToDelete]);
        dataPointList = sensor.getDataPoints(new QueryOptions(null, null, null, numberOfDataPoints, QueryOptions.SORT_ORDER.ASC));
        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", numberOfDataPoints - numberToDelete, listSize);
        assertEquals("Wrong Date of the DataPoint", date[numberToDelete], dataPointList.get(0).getTime());
    }

    // Delete DataPoints with null endTime
    public void testDeleteDataPointsSucceededWithNullEndTime() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        int value = 0;
        long time = dateType.getTime();
        int numberOfDataPoints = 10;

        long date[] = new long[numberOfDataPoints];

        for(int i=0; i<numberOfDataPoints; i++){
            date[i] = time + 1000 + i;
            sensor.insertOrUpdateDataPoint(value,date[i]);
        }

        List<DataPoint> dataPointList = sensor.getDataPoints(new QueryOptions(null, null, null, numberOfDataPoints, QueryOptions.SORT_ORDER.ASC));
        realm.beginTransaction();
        for(DataPoint dp: dataPointList){
            dp.setExistsInRemote(true);
            realm.copyToRealmOrUpdate( RealmDataPoint.fromDataPoint(dp));
        }
        realm.commitTransaction();

        int numberToDelete = 5;
        sensor.deleteDataPoints(date[numberToDelete], null);
        dataPointList = sensor.getDataPoints(new QueryOptions(null, null, null, numberOfDataPoints, QueryOptions.SORT_ORDER.DESC));
        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", numberOfDataPoints - numberToDelete, listSize);
        assertEquals("Wrong Date of the DataPoint", date[numberToDelete-1], dataPointList.get(0).getTime());
    }

    // Delete DataPoints with null startTime and endTime
    public void testDeleteDataPointsSucceededWithNullDates() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        dateType = new Date();
        int value = 0;
        long time = dateType.getTime();
        int numberOfDataPoints = 10;

        long date[] = new long[numberOfDataPoints];

        for(int i=0; i<numberOfDataPoints; i++){
            date[i] = time + 1000 + i;
            sensor.insertOrUpdateDataPoint(value,date[i]);
        }

        List<DataPoint> dataPointList = sensor.getDataPoints(new QueryOptions(null, null, null, numberOfDataPoints, QueryOptions.SORT_ORDER.ASC));
        realm.beginTransaction();
        for(DataPoint dp: dataPointList){
            dp.setExistsInRemote(true);
            realm.copyToRealmOrUpdate( RealmDataPoint.fromDataPoint(dp));
        }
        realm.commitTransaction();

        sensor.deleteDataPoints(null, null);
        dataPointList = sensor.getDataPoints(new QueryOptions(null, null, null, numberOfDataPoints, QueryOptions.SORT_ORDER.DESC));
        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", 0, listSize);
    }

    /****Unit tests of  DatabaseHandler Class****/

    public void testCreateSensorSucceeded() throws JSONException,DatabaseHandlerException, SensorException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);
        int sensorNumber = 0;

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);
        sensorNumber++;

        RealmResults<RealmSensor> resultList= realm.where(RealmSensor.class)
                .equalTo("userId", userId)
                .findAll();

        int listSize = resultList.size();
        assertEquals("Incorrect number of the Realm Sensor object", sensorNumber, listSize);

        Sensor resultSensor = RealmSensor.toSensor(getContext(), resultList.first());
        assertEquals("Incorrect name of the Realm Sensor object", sensorName, resultSensor.getName());
        assertEquals("Incorrect source of the Realm Sensor object", sourceName, resultSensor.getSource());
        assertEquals("Incorrect sensorOptions meta of the Realm Sensor object", sensorOptions.getMeta().toString(), resultSensor.getOptions().getMeta().toString());
        assertEquals("Incorrect sensorOptions isUploadEnabled of the Realm Sensor object", sensorOptions.isUploadEnabled(), resultSensor.getOptions().isUploadEnabled());
        assertEquals("Incorrect sensorOptions isDownloadEnabled of the Realm Sensor object", sensorOptions.isDownloadEnabled(), resultSensor.getOptions().isDownloadEnabled());
        assertEquals("Incorrect sensorOptions isPersistLocally of the Realm Sensor object", sensorOptions.isPersistLocally(), resultSensor.getOptions().isPersistLocally());
        assertEquals("Incorrect userId of the Realm Sensor object", userId, resultSensor.getUserId());
    }

    public void testCreateSensorFailedWithDuplicateCreation() throws JSONException, DatabaseHandlerException, SensorException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject meta = new JSONObject();
        meta.put("sensor_description","sensor_description");
        meta.put("display_name","display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(meta,csUploadEnabled,csDownloadEnabled,persistLocally);
        int sensorNumber = 0;

        sensor = databaseHandler.createSensor(sourceName,sensorName, sensorOptions);
        sensorNumber++;
        try {
            sensor = databaseHandler.createSensor(sourceName,sensorName, sensorOptions);
            sensorNumber++;
        }catch(DatabaseHandlerException e){
            assertEquals("Wrong DatabaseHandlerException", "Cannot create sensor. A sensor with name \"" + sensorName + "\" and source \"" + sourceName + "\" already exists.", e.getMessage());
            assertEquals("Wrong sensor number", sensorNumber, 1);
        }
    }

    public void testSetOptionsSucceeded() throws JSONException, DatabaseHandlerException, SensorException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);
        int sensorNumber = 0;

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);
        sensorNumber++;

        sensorMeta.put("sensor_description", "sensor_description1");
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);
        sensor.setOptions(sensorOptions);

        RealmResults<RealmSensor> resultList= realm.where(RealmSensor.class)
                .equalTo("name", sensorName)
                .findAll();

        int listSize = resultList.size();
        assertEquals("Incorrect number of the Realm Sensor object", sensorNumber, listSize);
        Sensor resultSensor = RealmSensor.toSensor(getContext(), resultList.first());
        assertEquals("Incorrect options of the Realm Sensor object", sensorMeta.toString(), resultSensor.getOptions().getMeta().toString());
    }

    public void testSetSyncedSucceeded() throws JSONException, DatabaseHandlerException, SensorException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);
        int sensorNumber = 0;

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);
        sensorNumber++;

        boolean downloaded = true;
        sensor.setRemoteDataPointsDownloaded(downloaded);
        assertEquals("Incorrect synced status of the Realm Sensor object", downloaded, sensor.isRemoteDataPointsDownloaded());

        RealmResults<RealmSensor> resultList= realm.where(RealmSensor.class)
                .equalTo("name", sensorName)
                .findAll();

        int listSize = resultList.size();
        assertEquals("Incorrect number of the Realm Sensor object", sensorNumber, listSize);
        Sensor resultSensor = RealmSensor.toSensor(getContext(), resultList.first());
        assertEquals("Incorrect userId of the Realm Sensor object", downloaded, resultSensor.isRemoteDataPointsDownloaded());
    }


    public void testGetSensorSucceeded() throws JSONException, DatabaseHandlerException, SensorException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        Sensor resultSensor = databaseHandler.getSensor(sourceName,sensorName);

        assertEquals("This is not the expected sensorId", sensor.getId(), resultSensor.getId());
        assertEquals("This is not the expected sensor name", sensor.getName(), resultSensor.getName());
        assertEquals("This is not the expected sensor meta", sensorOptions.getMeta().toString(),resultSensor.getOptions().getMeta().toString());
        assertEquals("This is not the expected sensor status of csUploadEnabled", sensorOptions.isUploadEnabled(), resultSensor.getOptions().isUploadEnabled());
        assertEquals("This is not the expected sensor status of csDownloadEnabled", sensorOptions.isDownloadEnabled(), resultSensor.getOptions().isDownloadEnabled());
        assertEquals("This is not the expected sensor status of persistLocally", sensorOptions.isPersistLocally(), resultSensor.getOptions().isPersistLocally());
        assertEquals("This is not the expected sensor UserId", sensor.getUserId(), resultSensor.getUserId());
        assertEquals("This is not the expected sensor status of synced", sensor.isRemoteDataPointsDownloaded(), resultSensor.isRemoteDataPointsDownloaded());
    }

    public void testGetSensorFailedWithInvalidSensor() throws JSONException, DatabaseHandlerException, SensorException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);

        String fakeName = "light_sensor";
        try {
            databaseHandler.getSensor(sourceName,fakeName);
        }catch(DatabaseHandlerException e){
            assertEquals("Wrong DatabaseHandlerException Message","Sensor not found. Sensor with name " + fakeName + " does not exist." , e.getMessage());
        }
    }


    public void testGetSensorsSucceeded() throws JSONException, DatabaseHandlerException, SensorException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);
        int sensorNumber = 0;

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);
        sensorNumber++;

        String sensorName1 = "visits";

        sensor = databaseHandler.createSensor(sourceName,sensorName1,sensorOptions);
        sensorNumber++;

        List<Sensor> resultSensor = databaseHandler.getSensors(sourceName);

        int listSize = resultSensor.size();
        assertEquals("Incorrect number of the sensor object", sensorNumber, listSize);
        assertEquals("Incorrect name of the Realm sensor object", sensorName, resultSensor.get(0).getName());
        assertEquals("Incorrect name of the Realm sensor object", sensorName1, resultSensor.get(1).getName());
    }

    public void testGetSensorsWithZeroResult() throws JSONException, DatabaseHandlerException, SensorException {
        List<Sensor> resultSensor = databaseHandler.getSensors("sense-android");

        int listSize = resultSensor.size();
        assertEquals("Incorrect number of the sensor object", 0, listSize);
    }

    public void testGetSourcesSucceeded() throws JSONException, DatabaseHandlerException, SensorException {
        String sourceName = "sense-android";
        String sensorName = "noise_sensor";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        sensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);
        int sourceNumber = 0;

        sensor = databaseHandler.createSensor(sourceName,sensorName,sensorOptions);
        sourceNumber++;

        String sourceName1 = "sony";
        sensor = databaseHandler.createSensor(sourceName1,sensorName,sensorOptions);
        sourceNumber++;

        List<String> returnSources = databaseHandler.getSources();

        int listSize = returnSources.size();
        assertEquals("Incorrect number of the source object", sourceNumber, listSize);
        assertNotSame(returnSources.get(0), returnSources.get(1));
    }

    public void testGetSourcesWithZeroResult() throws JSONException, DatabaseHandlerException{
        int sourceNumber = 0;

        List<String> returnSources = databaseHandler.getSources();
        int listSize = returnSources.size();
        assertEquals("Incorrect number of the source object", sourceNumber, listSize);
    }

    public void testCreateDataDeletionRequestSucceeded() throws DatabaseHandlerException{
        int numberOfRequest = 0;
        databaseHandler.createDataDeletionRequest("light","sony",new Date().getTime(),new Date().getTime());
        numberOfRequest++;
        databaseHandler.createDataDeletionRequest("gyroscope", "htc",new Date().getTime(),new Date().getTime());
        numberOfRequest++;

        RealmResults<DataDeletionRequest> resultList= realm.where(DataDeletionRequest.class)
                .equalTo("userId", databaseHandler.getUserId())
                .findAll();
        assertEquals("Incorrect number of the deletion requests", numberOfRequest, resultList.size());
    }

    public void testCreateDataDeletionRequestSucceededWithNullDate() throws DatabaseHandlerException{
        int numberOfRequest = 0;
        databaseHandler.createDataDeletionRequest("light","sony", null, new Date().getTime());
        numberOfRequest++;
        databaseHandler.createDataDeletionRequest("gyroscope", "htc",new Date().getTime(), null);
        numberOfRequest++;
        databaseHandler.createDataDeletionRequest("accelerometer", "iphone", null, null);
        numberOfRequest++;

        RealmResults<DataDeletionRequest> resultList = realm.where(DataDeletionRequest.class)
                .equalTo("userId", databaseHandler.getUserId())
                .findAll();
        assertEquals("Incorrect number of the deletion requests", numberOfRequest, resultList.size());
    }

    public void testGetDataDeletionRequestSucceeded() throws DatabaseHandlerException{
        int numberOfRequest = 0;
        databaseHandler.createDataDeletionRequest("light","sony", null, new Date().getTime());
        numberOfRequest++;
        databaseHandler.createDataDeletionRequest("gyroscope", "htc",new Date().getTime(), null);
        numberOfRequest++;
        databaseHandler.createDataDeletionRequest("accelerometer", "iphone", null, null);
        numberOfRequest++;

        List<DataDeletionRequest> resultList = databaseHandler.getDataDeletionRequests();
        assertEquals("Incorrect number of the deletion requests", numberOfRequest, resultList.size());
    }

    public void testDeleteDataDeletionRequestSucceeded() throws DatabaseHandlerException{
        int numberOfRequest = 0;
        databaseHandler.createDataDeletionRequest("light","sony", null, new Date().getTime());
        numberOfRequest++;
        databaseHandler.createDataDeletionRequest("gyroscope", "htc",new Date().getTime(), null);
        numberOfRequest++;
        databaseHandler.createDataDeletionRequest("accelerometer", "iphone", null, null);
        numberOfRequest++;

        List<DataDeletionRequest> resultList = databaseHandler.getDataDeletionRequests();
        for(DataDeletionRequest request: resultList){
            databaseHandler.deleteDataDeletionRequest(request.getUuid());
            numberOfRequest--;
            List<DataDeletionRequest> newResultList = databaseHandler.getDataDeletionRequests();
            assertEquals("Incorrect number of the deletion requests", numberOfRequest, newResultList.size());
        }
    }
}
