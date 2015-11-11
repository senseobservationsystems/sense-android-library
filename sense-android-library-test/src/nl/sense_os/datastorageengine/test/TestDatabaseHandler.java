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
import nl.sense_os.datastorageengine.realm.RealmDataDeletionRequest;
import nl.sense_os.datastorageengine.DataPoint;
import nl.sense_os.datastorageengine.DatabaseHandlerException;
import nl.sense_os.datastorageengine.QueryOptions;
import nl.sense_os.datastorageengine.SensorException;
import nl.sense_os.datastorageengine.SensorProfileException;
import nl.sense_os.datastorageengine.DatabaseHandler;
import nl.sense_os.datastorageengine.SensorProfiles;
import nl.sense_os.datastorageengine.realm.RealmDataPoint;
import nl.sense_os.datastorageengine.realm.RealmSensor;
import nl.sense_os.datastorageengine.Sensor;
import nl.sense_os.datastorageengine.SensorOptions;
import nl.sense_os.util.json.SchemaException;
import nl.sense_os.util.json.ValidationException;

public class TestDatabaseHandler extends AndroidTestCase {

    protected Realm mRealm;
    private RealmConfiguration mTestConfig;
    private DatabaseHandler mDatabaseHandler;
    private String mUserId = "mUserId";
    private String mNewUserId = "mNewUserId";
    private String mSourceName = "sense-android";
    private Sensor mSensor;
    private SensorOptions mSensorOptions;
    private DatabaseHandler mNewDatabaseHandler; // TODO: remove this mNewDatabaseHandler
    private byte[] mEncryptionKey = null; // TODO: test with encryption key

    @Override
    protected void setUp () throws Exception {

        mTestConfig = new RealmConfiguration.Builder(getContext()).build();
        Realm.deleteRealm(mTestConfig);
        mRealm = Realm.getInstance(mTestConfig);

        mDatabaseHandler = new DatabaseHandler(getContext(), mUserId);
        mNewDatabaseHandler = new DatabaseHandler(getContext(), mNewUserId);

        // Create a few sensor profiles by hand, so we don't have to fetch them from the server via SensorDataProxy
        SensorProfiles profiles = new SensorProfiles(getContext(), mEncryptionKey);

        profiles.create("noise",
                new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"description\": \"The Ambient noise in decibel\",\"type\": \"number\"}"));

        profiles.create("accelerometer",
                new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"type\": \"object\",\"properties\": {\"x-axis\": {\"description\": \"The acceleration force applied on the x-axis in m\\/s2\", \"type\": \"number\"}, \"y-axis\": { \"description\": \"The acceleration force applied on the y-axis in m\\/s2\", \"type\": \"number\"}, \"z-axis\": {\"description\": \"The acceleration force applied on the z-axis in m\\/s2\", \"type\": \"number\" } }, \"required\": [\"x-axis\",\"y-axis\",\"z-axis\"]}"));

        profiles.create("light",
                new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"description\": \"The illuminance in lux\",\"type\": \"number\"}"));
    }

    @Override
    protected void tearDown () throws Exception {
        mRealm.close();
    }

    /****Unit tests of  Sensor Class****/

    public void testDifferentUserIdWithSameSensorAndSourceName() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        Sensor newSensor = mNewDatabaseHandler.createSensor(sourceName, sensorName, mSensorOptions);

    }

    public void testInsertDataPointFailedWithInvalidValue() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException, ValidationException {
        try {
            Sensor noise = mDatabaseHandler.createSensor("sense-android","noise", new SensorOptions());

            noise.insertOrUpdateDataPoint("hello world", new Date().getTime());
            fail("Should throw an exception");
        }
        catch (ValidationException err) {
            assertEquals("Invalid type. number expected.", err.getMessage());
        }

        try {
            Sensor accelerometer = mDatabaseHandler.createSensor("sense-android","accelerometer", new SensorOptions());

            JSONObject value = new JSONObject("{\"x-axis\":2,\"y-axis\":3.4}");
            accelerometer.insertOrUpdateDataPoint(value, new Date().getTime());
            fail("Should throw an exception");
        }
        catch (ValidationException err) {
            assertEquals("Required property 'z-axis' missing.", err.getMessage());
        }

    }

    public void testInsertDataPointSucceededWithJsonObject() throws JSONException, DatabaseHandlerException, SensorException, SchemaException, ValidationException, SensorProfileException {
        String sourceName = "sense-android";
        String sensorName = "accelerometer";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        long date = new Date().getTime();
        JSONObject value = new JSONObject();
        value.put("x-axis", 9.1);
        value.put("y-axis", 8.9);
        value.put("z-axis", 7.2);
        int numberOfDataPoints = 0;

        mSensor.insertOrUpdateDataPoint(value, date);
        numberOfDataPoints++;

        RealmResults<RealmDataPoint> resultList= mRealm.where(RealmDataPoint.class)
                .equalTo("date", date)
                .findAll();
        int listSize = resultList.size();
        assertEquals("Incorrect number of the Realm DataPoint object", numberOfDataPoints, listSize);

        DataPoint resultDataPoint = RealmDataPoint.toDataPoint(resultList.first());
        assertEquals("Incorrect value of the Realm DataPoint object", value.toString(), resultDataPoint.getValueAsJSONObject().toString());
    }

    // Insert a datapoint, and make an update of value. Realm is expected only to update the same object with new value.
    public void testInsertDatePointSucceededWithDuplicateDataPoint() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException, ValidationException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        int value = 0;
        long date = new Date().getTime();
        int numberOfDataPoints = 0;

        mSensor.insertOrUpdateDataPoint(value, date);
        numberOfDataPoints++;

        value = 1;
        mSensor.insertOrUpdateDataPoint(value, date);

        RealmResults<RealmDataPoint> resultList= mRealm.where(RealmDataPoint.class)
                                                      .equalTo("date", date)
                                                      .findAll();

        int listSize = resultList.size();
        assertEquals("Incorrect number of the Realm DataPoint object", numberOfDataPoints, listSize);

        DataPoint resultDataPoint = RealmDataPoint.toDataPoint(resultList.first());
        assertEquals("Incorrect value of the Realm DataPoint object", value, resultDataPoint.getValueAsInteger());
    }

    // Insert two datapoints with same date but with different sensorId
    public void testInsertDatePointSucceededWithTwoSensorsHavingSameDate() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException, ValidationException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        int value = 0;
        long date = new Date().getTime();
        int numberOfDataPoints = 0;

        mSensor.insertOrUpdateDataPoint(value, date);
        numberOfDataPoints++;
        String newSourceName = "sony";
        String newSensorName = "light";
        mSensor = mDatabaseHandler.createSensor(newSourceName,newSensorName,mSensorOptions);

        int value1 = 1;
        mSensor.insertOrUpdateDataPoint(value1, date);
        numberOfDataPoints++;

        RealmResults<RealmDataPoint> resultList= mRealm.where(RealmDataPoint.class)
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
    public void testGetDataPointsSucceededWithASC() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException, ValidationException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        int value = 0;
        long date1 = new Date().getTime();
        int numberOfDataPoints = 0;

        mSensor.insertOrUpdateDataPoint(value, date1);
        numberOfDataPoints++;

        long date2 = date1 + 1000;

        mSensor.insertOrUpdateDataPoint(value, date2);
        numberOfDataPoints++;

        long date3 = date2 + 1000;

        mSensor.insertOrUpdateDataPoint(value, date3);
        numberOfDataPoints++;

        int limit = numberOfDataPoints - 1;
        List<DataPoint> dataPointList = mSensor.getDataPoints(new QueryOptions(date1, date3+1, null, limit, QueryOptions.SORT_ORDER.ASC));

        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", limit, listSize);
        assertEquals("Incorrect data point by using ASC sorting", date1, dataPointList.get(0).getTime());
        assertEquals("Incorrect data point by using ASC sorting", date2, dataPointList.get(1).getTime());
    }

    public void testGetDataPointsSucceededWithOutOfBoundLimit() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException, ValidationException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        int value = 0;
        long date1 = new Date().getTime();
        int numberOfDataPoints = 0;

        mSensor.insertOrUpdateDataPoint(value, date1);
        numberOfDataPoints++;

        long date2 = date1 + 1000;

        mSensor.insertOrUpdateDataPoint(value, date2);
        numberOfDataPoints++;

        long date3 = date2 + 1000;

        mSensor.insertOrUpdateDataPoint(value, date3);
        numberOfDataPoints++;

        int limit = numberOfDataPoints + 1;
        List<DataPoint> dataPointList = mSensor.getDataPoints(new QueryOptions(date1, date3+1, null, limit, QueryOptions.SORT_ORDER.ASC));

        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", numberOfDataPoints, listSize);
        assertEquals("Incorrect data point by using ASC sorting", date1, dataPointList.get(0).getTime());
        assertEquals("Incorrect data point by using ASC sorting", date2, dataPointList.get(1).getTime());
    }

    public void testGetDataPointsSucceededWithNullStartTime() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException, ValidationException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        int value = 0;
        long time = new Date().getTime();
        int numberOfDataPoints = 10;

        long date[] = new long[numberOfDataPoints];

        for(int i=0; i<numberOfDataPoints; i++){
            date[i] = time + 1000 + i;
            mSensor.insertOrUpdateDataPoint(value,date[i]);
        }

        int limit = 5;
        List<DataPoint> dataPointList = mSensor.getDataPoints(new QueryOptions(null, date[9]+1, null, limit, QueryOptions.SORT_ORDER.ASC));

        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", limit, listSize);
        for(int i=0; i<limit; i++){
            assertEquals("Incorrect data point by using ASC sorting", date[i], dataPointList.get(i).getTime());
        }
    }

    public void testGetDataPointsSucceededWithNullEndTime() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException, ValidationException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        int value = 0;
        long time = new Date().getTime();
        int numberOfDataPoints = 10;

        long date[] = new long[numberOfDataPoints];

        for(int i=0; i<numberOfDataPoints; i++){
            date[i] = time + 1000 + i;
            mSensor.insertOrUpdateDataPoint(value,date[i]);
        }

        int limit = 5;
        List<DataPoint> dataPointList = mSensor.getDataPoints(new QueryOptions(date[0], null, null, limit, QueryOptions.SORT_ORDER.DESC));

        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", limit, listSize);
        for(int i=9; i>limit-1; i--){
            assertEquals("Incorrect data point by using DESC sorting", date[i], dataPointList.get(numberOfDataPoints-1-i).getTime());
        }
    }

    public void testGetDataPointsSucceededWithNullLimit() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException, ValidationException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        int value = 0;
        long time = new Date().getTime();
        int numberOfDataPoints = 10;

        long date[] = new long[numberOfDataPoints];

        for(int i=0; i<numberOfDataPoints; i++){
            date[i] = time + 1000 + i;
            mSensor.insertOrUpdateDataPoint(value,date[i]);
        }

        List<DataPoint> dataPointList = mSensor.getDataPoints(new QueryOptions(date[0], date[9]+1, null, null, QueryOptions.SORT_ORDER.ASC));

        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", numberOfDataPoints, listSize);
        for(int i=0; i<numberOfDataPoints; i++){
            assertEquals("Incorrect data point by using ASC sorting", date[i], dataPointList.get(i).getTime());
        }
    }

    public void testGetDataPointsSucceededWithNullParams() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException, ValidationException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        int value = 0;
        long time = new Date().getTime();
        int numberOfDataPoints = 10;

        long date[] = new long[numberOfDataPoints];

        for(int i=0; i<numberOfDataPoints; i++){
            date[i] = time + 1000 + i;
            mSensor.insertOrUpdateDataPoint(value,date[i]);
        }

        List<DataPoint> dataPointList = mSensor.getDataPoints(new QueryOptions(null, null, null, null, QueryOptions.SORT_ORDER.ASC));

        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", numberOfDataPoints, listSize);
        for(int i=0; i<numberOfDataPoints; i++){
            assertEquals("Incorrect data point by using ASC sorting", date[i], dataPointList.get(i).getTime());
        }
    }


    public void testGetDataPointsWithInvalidLimit() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException, ValidationException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        int value = 0;
        long date1 = new Date().getTime();

        mSensor.insertOrUpdateDataPoint(value, date1);

        long date2 = date1 + 1000;

        mSensor.insertOrUpdateDataPoint(value, date2);

        long date3 = date2 + 1000;

        mSensor.insertOrUpdateDataPoint(value, date3);

        int limit = -1;
        try {
            mSensor.getDataPoints(new QueryOptions(date1, date3 + 1, null, limit, QueryOptions.SORT_ORDER.ASC));
        }catch(DatabaseHandlerException e){
            assertEquals("Wrong DatabaseHandlerException messge", "Invalid input of limit value", e.getMessage());
        }
    }

    // Get the datapoints with desc sort order
    public void testGetDataPointsSucceededWithDESC() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException, ValidationException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        int value = 0;
        long date1 = new Date().getTime();
        int numberOfDataPoints = 0;

        mSensor.insertOrUpdateDataPoint(value, date1);
        numberOfDataPoints++;

        long date2 = date1 + 1000;

        mSensor.insertOrUpdateDataPoint(value, date2);
        numberOfDataPoints++;

        long date3 = date2 + 1000;

        mSensor.insertOrUpdateDataPoint(value, date3);
        numberOfDataPoints++;

        int limit = numberOfDataPoints - 1;
        List<DataPoint> dataPointList = mSensor.getDataPoints(new QueryOptions(date1, date3+1, null, limit, QueryOptions.SORT_ORDER.DESC));

        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", limit, listSize);
        assertEquals("Incorrect data point by using ASC sorting", date3, dataPointList.get(0).getTime());
        assertEquals("Incorrect data point by using ASC sorting", date2, dataPointList.get(1).getTime());
    }

    // Test the case that end date is before start date
    public void testGetDataPointsFailedWithReverseDates() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException, ValidationException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        int value = 0;
        long date1 = new Date().getTime();
        int numberOfDataPoints = 0;

        mSensor.insertOrUpdateDataPoint(value, date1);
        numberOfDataPoints++;

        long date2 = date1 + 1000;

        mSensor.insertOrUpdateDataPoint(value, date2);
        numberOfDataPoints++;

        long date3 = date2 + 1000;

        mSensor.insertOrUpdateDataPoint(value, date3);
        numberOfDataPoints++;

        int limit = numberOfDataPoints - 1;
        try {
            List<DataPoint> dataPointList = mSensor.getDataPoints(new QueryOptions(date3, date1, null, limit, QueryOptions.SORT_ORDER.ASC));
            int listSize = dataPointList.size();
            assertEquals("Incorrect number of data points", 0, listSize);
        }catch(DatabaseHandlerException e){
            assertEquals("Wrong DatabaseHandlerException", "startTime is the same as or later than the endTime", e.getMessage());
        }
    }

    // Delete DataPoints with Specified startTime and endTime
    public void testDeleteDataPointsSucceededWithTwoSpecifiedDates() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException, ValidationException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        int value = 0;
        long time = new Date().getTime();
        int numberOfDataPoints = 10;

        long date[] = new long[numberOfDataPoints];

        for(int i=0; i<numberOfDataPoints; i++){
            date[i] = time + 1000 + i;
            mSensor.insertOrUpdateDataPoint(value,date[i]);
        }

        List<DataPoint> dataPointList = mSensor.getDataPoints(new QueryOptions(null, null, null, numberOfDataPoints, QueryOptions.SORT_ORDER.ASC));
        mRealm.beginTransaction();
        for(DataPoint dp: dataPointList){
            dp.setExistsInRemote(true);
            mRealm.copyToRealmOrUpdate(RealmDataPoint.fromDataPoint(dp));
        }
        mRealm.commitTransaction();

        int numberToDelete = 5;
        mSensor.deleteDataPoints(date[0], date[numberToDelete]);
        dataPointList = mSensor.getDataPoints(new QueryOptions(null, null, null, numberOfDataPoints, QueryOptions.SORT_ORDER.ASC));
        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", numberOfDataPoints - numberToDelete, listSize);
    }

    // Delete DataPoints with null startTime
    public void testDeleteDataPointsSucceededWithNullStartTime() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException, ValidationException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        int value = 0;
        long time = new Date().getTime();
        int numberOfDataPoints = 10;

        long date[] = new long[numberOfDataPoints];

        for(int i=0; i<numberOfDataPoints; i++){
            date[i] = time + 1000 + i;
            mSensor.insertOrUpdateDataPoint(value,date[i]);
        }

        List<DataPoint> dataPointList = mSensor.getDataPoints(new QueryOptions(null, null, null, numberOfDataPoints, QueryOptions.SORT_ORDER.ASC));
        mRealm.beginTransaction();
        for(DataPoint dp: dataPointList){
            dp.setExistsInRemote(true);
            mRealm.copyToRealmOrUpdate(RealmDataPoint.fromDataPoint(dp));
        }
        mRealm.commitTransaction();

        int numberToDelete = 5;
        mSensor.deleteDataPoints(null, date[numberToDelete]);
        dataPointList = mSensor.getDataPoints(new QueryOptions(null, null, null, numberOfDataPoints, QueryOptions.SORT_ORDER.ASC));
        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", numberOfDataPoints - numberToDelete, listSize);
        assertEquals("Wrong Date of the DataPoint", date[numberToDelete], dataPointList.get(0).getTime());
    }

    // Delete DataPoints with null endTime
    public void testDeleteDataPointsSucceededWithNullEndTime() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException, ValidationException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        int value = 0;
        long time = new Date().getTime();
        int numberOfDataPoints = 10;

        long date[] = new long[numberOfDataPoints];

        for(int i=0; i<numberOfDataPoints; i++){
            date[i] = time + 1000 + i;
            mSensor.insertOrUpdateDataPoint(value,date[i]);
        }

        List<DataPoint> dataPointList = mSensor.getDataPoints(new QueryOptions(null, null, null, numberOfDataPoints, QueryOptions.SORT_ORDER.ASC));
        mRealm.beginTransaction();
        for(DataPoint dp: dataPointList){
            dp.setExistsInRemote(true);
            mRealm.copyToRealmOrUpdate(RealmDataPoint.fromDataPoint(dp));
        }
        mRealm.commitTransaction();

        int numberToDelete = 5;
        mSensor.deleteDataPoints(date[numberToDelete], null);
        dataPointList = mSensor.getDataPoints(new QueryOptions(null, null, null, numberOfDataPoints, QueryOptions.SORT_ORDER.DESC));
        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", numberOfDataPoints - numberToDelete, listSize);
        assertEquals("Wrong Date of the DataPoint", date[numberToDelete-1], dataPointList.get(0).getTime());
    }

    // Delete DataPoints with null startTime and endTime
    public void testDeleteDataPointsSucceededWithNullDates() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException, ValidationException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        int value = 0;
        long time = new Date().getTime();
        int numberOfDataPoints = 10;

        long date[] = new long[numberOfDataPoints];

        for(int i=0; i<numberOfDataPoints; i++){
            date[i] = time + 1000 + i;
            mSensor.insertOrUpdateDataPoint(value,date[i]);
        }

        List<DataPoint> dataPointList = mSensor.getDataPoints(new QueryOptions(null, null, null, numberOfDataPoints, QueryOptions.SORT_ORDER.ASC));
        mRealm.beginTransaction();
        for(DataPoint dp: dataPointList){
            dp.setExistsInRemote(true);
            mRealm.copyToRealmOrUpdate(RealmDataPoint.fromDataPoint(dp));
        }
        mRealm.commitTransaction();

        mSensor.deleteDataPoints(null, null);
        dataPointList = mSensor.getDataPoints(new QueryOptions(null, null, null, numberOfDataPoints, QueryOptions.SORT_ORDER.DESC));
        int listSize = dataPointList.size();
        assertEquals("Incorrect number of data points", 0, listSize);
    }

    /****Unit tests of  DatabaseHandler Class****/

    public void testCreateSensorSucceeded() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);
        int sensorNumber = 0;

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);
        sensorNumber++;

        RealmResults<RealmSensor> resultList= mRealm.where(RealmSensor.class)
                .equalTo("userId", mUserId)
                .findAll();

        int listSize = resultList.size();
        assertEquals("Incorrect number of the Realm Sensor object", sensorNumber, listSize);

        Sensor resultSensor = RealmSensor.toSensor(getContext(), mEncryptionKey, resultList.first());
        assertEquals("Incorrect name of the Realm Sensor object", sensorName, resultSensor.getName());
        assertEquals("Incorrect source of the Realm Sensor object", sourceName, resultSensor.getSource());
        assertEquals("Incorrect sensorOptions meta of the Realm Sensor object", mSensorOptions.getMeta().toString(), resultSensor.getOptions().getMeta().toString());
        assertEquals("Incorrect sensorOptions isUploadEnabled of the Realm Sensor object", mSensorOptions.isUploadEnabled(), resultSensor.getOptions().isUploadEnabled());
        assertEquals("Incorrect sensorOptions isDownloadEnabled of the Realm Sensor object", mSensorOptions.isDownloadEnabled(), resultSensor.getOptions().isDownloadEnabled());
        assertEquals("Incorrect sensorOptions isPersistLocally of the Realm Sensor object", mSensorOptions.isPersistLocally(), resultSensor.getOptions().isPersistLocally());
        assertEquals("Incorrect userId of the Realm Sensor object", mUserId, resultSensor.getUserId());
    }

    public void testCreateSensorFailedWithDuplicateCreation() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject meta = new JSONObject();
        meta.put("sensor_description","sensor_description");
        meta.put("display_name","display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(meta,csUploadEnabled,csDownloadEnabled,persistLocally);
        int sensorNumber = 0;

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName, mSensorOptions);
        sensorNumber++;
        try {
            mSensor = mDatabaseHandler.createSensor(sourceName,sensorName, mSensorOptions);
            sensorNumber++;
        }catch(DatabaseHandlerException e){
            assertEquals("Wrong DatabaseHandlerException", "Cannot create sensor. A sensor with name \"" + sensorName + "\" and source \"" + sourceName + "\" already exists.", e.getMessage());
            assertEquals("Wrong sensor number", sensorNumber, 1);
        }
    }

    public void testSetOptionsSucceeded() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);
        int sensorNumber = 0;

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);
        sensorNumber++;

        sensorMeta.put("sensor_description", "sensor_description1");
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);
        mSensor.setOptions(mSensorOptions);

        RealmResults<RealmSensor> resultList= mRealm.where(RealmSensor.class)
                .equalTo("name", sensorName)
                .findAll();

        int listSize = resultList.size();
        assertEquals("Incorrect number of the Realm Sensor object", sensorNumber, listSize);
        Sensor resultSensor = RealmSensor.toSensor(getContext(), mEncryptionKey, resultList.first());
        assertEquals("Incorrect options of the Realm Sensor object", sensorMeta.toString(), resultSensor.getOptions().getMeta().toString());
    }

    public void testSetSyncedSucceeded() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);
        int sensorNumber = 0;

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);
        sensorNumber++;

        boolean downloaded = true;
        mSensor.setRemoteDataPointsDownloaded(downloaded);
        assertEquals("Incorrect synced status of the Realm Sensor object", downloaded, mSensor.isRemoteDataPointsDownloaded());

        RealmResults<RealmSensor> resultList= mRealm.where(RealmSensor.class)
                .equalTo("name", sensorName)
                .findAll();

        int listSize = resultList.size();
        assertEquals("Incorrect number of the Realm Sensor object", sensorNumber, listSize);
        Sensor resultSensor = RealmSensor.toSensor(getContext(), mEncryptionKey, resultList.first());
        assertEquals("Incorrect userId of the Realm Sensor object", downloaded, resultSensor.isRemoteDataPointsDownloaded());
    }


    public void testGetSensorSucceeded() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        Sensor resultSensor = mDatabaseHandler.getSensor(sourceName,sensorName);

        assertEquals("This is not the expected sensorId", mSensor.getId(), resultSensor.getId());
        assertEquals("This is not the expected mSensor name", mSensor.getName(), resultSensor.getName());
        assertEquals("This is not the expected mSensor meta", mSensorOptions.getMeta().toString(),resultSensor.getOptions().getMeta().toString());
        assertEquals("This is not the expected mSensor status of csUploadEnabled", mSensorOptions.isUploadEnabled(), resultSensor.getOptions().isUploadEnabled());
        assertEquals("This is not the expected mSensor status of csDownloadEnabled", mSensorOptions.isDownloadEnabled(), resultSensor.getOptions().isDownloadEnabled());
        assertEquals("This is not the expected mSensor status of persistLocally", mSensorOptions.isPersistLocally(), resultSensor.getOptions().isPersistLocally());
        assertEquals("This is not the expected mSensor UserId", mSensor.getUserId(), resultSensor.getUserId());
        assertEquals("This is not the expected mSensor status of synced", mSensor.isRemoteDataPointsDownloaded(), resultSensor.isRemoteDataPointsDownloaded());
    }

    public void testGetSensorFailedWithInvalidSensor() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);

        String fakeName = "light_sensor";
        try {
            mDatabaseHandler.getSensor(sourceName, fakeName);
        }catch(DatabaseHandlerException e){
            assertEquals("Wrong DatabaseHandlerException Message","Sensor not found. Sensor with name " + fakeName + " does not exist." , e.getMessage());
        }
    }


    public void testGetSensorsSucceeded() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);
        int sensorNumber = 0;

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);
        sensorNumber++;

        String sensorName1 = "light";

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName1,mSensorOptions);
        sensorNumber++;

        List<Sensor> resultSensor = mDatabaseHandler.getSensors(sourceName);

        int listSize = resultSensor.size();
        assertEquals("Incorrect number of the mSensor object", sensorNumber, listSize);
        assertEquals("Incorrect name of the Realm mSensor object", sensorName, resultSensor.get(0).getName());
        assertEquals("Incorrect name of the Realm mSensor object", sensorName1, resultSensor.get(1).getName());
    }

    public void testGetSensorsWithZeroResult() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException {
        List<Sensor> resultSensor = mDatabaseHandler.getSensors("sense-android");

        int listSize = resultSensor.size();
        assertEquals("Incorrect number of the mSensor object", 0, listSize);
    }

    public void testGetSourcesSucceeded() throws JSONException, DatabaseHandlerException, SensorException, SensorProfileException, SchemaException {
        String sourceName = "sense-android";
        String sensorName = "noise";
        JSONObject sensorMeta = new JSONObject();
        sensorMeta.put("sensor_description", "sensor_description");
        sensorMeta.put("display_name", "display_name");
        boolean csUploadEnabled = true;
        boolean csDownloadEnabled = true;
        boolean persistLocally = true;
        mSensorOptions = new SensorOptions(sensorMeta,csUploadEnabled,csDownloadEnabled,persistLocally);
        int sourceNumber = 0;

        mSensor = mDatabaseHandler.createSensor(sourceName,sensorName,mSensorOptions);
        sourceNumber++;

        String sourceName1 = "sony";
        mSensor = mDatabaseHandler.createSensor(sourceName1,sensorName,mSensorOptions);
        sourceNumber++;

        List<String> returnSources = mDatabaseHandler.getSources();

        int listSize = returnSources.size();
        assertEquals("Incorrect number of the source object", sourceNumber, listSize);
        assertNotSame(returnSources.get(0), returnSources.get(1));
    }

    public void testGetSourcesWithZeroResult() throws JSONException, DatabaseHandlerException{
        int sourceNumber = 0;

        List<String> returnSources = mDatabaseHandler.getSources();
        int listSize = returnSources.size();
        assertEquals("Incorrect number of the source object", sourceNumber, listSize);
    }

    public void testCreateDataDeletionRequestSucceeded() throws DatabaseHandlerException{
        int numberOfRequest = 0;
        mDatabaseHandler.createDataDeletionRequest(mSourceName, "light", new Date().getTime(), new Date().getTime());
        numberOfRequest++;
        mDatabaseHandler.createDataDeletionRequest(mSourceName, "gyroscope",new Date().getTime(),new Date().getTime());
        numberOfRequest++;

        RealmResults<RealmDataDeletionRequest> resultList= mRealm.where(RealmDataDeletionRequest.class)
                .equalTo("userId", mDatabaseHandler.getUserId())
                .findAll();
        assertEquals("Incorrect number of the deletion requests", numberOfRequest, resultList.size());
    }

    public void testCreateDataDeletionRequestSucceededWithNullDate() throws DatabaseHandlerException{
        int numberOfRequest = 0;
        mDatabaseHandler.createDataDeletionRequest(mSourceName, "light", null, new Date().getTime());
        numberOfRequest++;
        mDatabaseHandler.createDataDeletionRequest(mSourceName, "gyroscope", new Date().getTime(), null);
        numberOfRequest++;
        mDatabaseHandler.createDataDeletionRequest(mSourceName, "accelerometer", null, null);
        numberOfRequest++;

        RealmResults<RealmDataDeletionRequest> resultList = mRealm.where(RealmDataDeletionRequest.class)
                .equalTo("userId", mDatabaseHandler.getUserId())
                .findAll();
        assertEquals("Incorrect number of the deletion requests", numberOfRequest, resultList.size());
    }

    public void testGetDataDeletionRequestSucceeded() throws DatabaseHandlerException{
        int numberOfRequest = 0;
        mDatabaseHandler.createDataDeletionRequest(mSourceName, "light",null, new Date().getTime());
        numberOfRequest++;
        mDatabaseHandler.createDataDeletionRequest(mSourceName, "gyroscope", new Date().getTime(), null);
        numberOfRequest++;
        mDatabaseHandler.createDataDeletionRequest(mSourceName, "accelerometer",null, null);
        numberOfRequest++;

        List<DataDeletionRequest> resultList = mDatabaseHandler.getDataDeletionRequests();
        assertEquals("Incorrect number of the deletion requests", numberOfRequest, resultList.size());
    }

    public void testDeleteDataDeletionRequestSucceeded() throws DatabaseHandlerException{
        int numberOfRequest = 0;
        mDatabaseHandler.createDataDeletionRequest(mSourceName, "light", null, new Date().getTime());
        numberOfRequest++;
        mDatabaseHandler.createDataDeletionRequest(mSourceName, "gyroscope", new Date().getTime(), null);
        numberOfRequest++;
        mDatabaseHandler.createDataDeletionRequest(mSourceName, "accelerometer", null, null);
        numberOfRequest++;

        List<DataDeletionRequest> resultList = mDatabaseHandler.getDataDeletionRequests();
        for(DataDeletionRequest request: resultList){
            mDatabaseHandler.deleteDataDeletionRequest(request.getId());
            numberOfRequest--;
            List<DataDeletionRequest> newResultList = mDatabaseHandler.getDataDeletionRequests();
            assertEquals("Incorrect number of the deletion requests", numberOfRequest, newResultList.size());
        }
    }
}
