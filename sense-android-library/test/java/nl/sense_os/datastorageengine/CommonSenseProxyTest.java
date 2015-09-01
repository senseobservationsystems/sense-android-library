package nl.sense_os.datastorageengine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.util.Map;

import nl.sense_os.service.BuildConfig;
import nl.sense_os.service.provider.SNTP;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link CommonSenseProxyTest} tests whether {@link CommonSenseProxy} covers the full CommonSense API
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class CommonSenseProxyTest {

    Map<String, String> newUser;
    CommonSenseProxy proxy = new CommonSenseProxy(false, CSUtils.APP_KEY);

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Before
    public  void setUp () throws IOException{
        newUser = CSUtils.createCSAccount();

    }

    @Test
    public void testLoginUserWithValidUsernameAndValidPassword() throws IOException, RuntimeException{
        String session_id = proxy.loginUser(newUser.get("username"),newUser.get("password"));
        assertNotNull("session_id returned from server is null", session_id);
        assertFalse("session_id returned from server is empty",session_id.isEmpty());
    }
    @Test
    public void testLoginUserWithValidUsernameAndWrongPassword() throws IOException, RuntimeException{
        thrown.expect(IOException.class);
        thrown.expectMessage("could not get InputStream");
        proxy.loginUser(newUser.get("username"), "123456789");
    }
    @Test
    public void testLoginUserWithValidUsernameAndNullPassword() throws IOException, RuntimeException{
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("invalid input of username or password");
        proxy.loginUser(newUser.get("username"),null);
    }
    @Test
    public void testLoginUserWithValidUsernameAndEmptyPassword() throws IOException, RuntimeException{
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("invalid input of username or password");
        proxy.loginUser(newUser.get("username"),"");
    }
    @Test
    public void testLoginUserWithNullUsernameAndValidPassword() throws IOException, RuntimeException{
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("invalid input of username or password");
        proxy.loginUser(null,newUser.get("password"));
    }
    @Test
    public void testLoginUserWithEmptyUsernameAndValidPassword() throws IOException, RuntimeException{
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("invalid input of username or password");
        proxy.loginUser("",newUser.get("password"));
    }
    @Test
    public void testLogoutCurrentUserWithValidSessionID() throws IOException, IllegalArgumentException{
        // log in first in order to log out
        String session_id = proxy.loginUser(newUser.get("username"), newUser.get("password"));
        boolean result = proxy.logoutCurrentUser(session_id);
        assertTrue("current user cannot be successfully logged out", result);
    }
    @Test
    public void testLogoutCurrentUserWithNullSessionID() throws IOException, IllegalArgumentException{
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("invalid input of session ID");
        proxy.loginUser(newUser.get("username"),newUser.get("password"));
        boolean result = proxy.logoutCurrentUser(null);
    }
    @Test
    public void testLogoutCurrentUserWithEmptySessionID() throws IOException, IllegalArgumentException{
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("invalid input of session ID");
        // log in first in order to log out
        proxy.loginUser(newUser.get("username"),newUser.get("password"));
        boolean result = proxy.logoutCurrentUser("");
    }
    @Test
    public void testLogoutCurrentUserWithWrongSessionID() throws IOException, IllegalArgumentException{
        // log in first in order to log out
        proxy.loginUser(newUser.get("username"),newUser.get("password"));
        boolean result = proxy.logoutCurrentUser("987654321");
        assertFalse("logout with wrong session id should fail",result);
    }
    @Test
    public void testLogoutCurrentUserTwice() throws IOException, IllegalArgumentException{
        // log in first in order to log out
        String session_id = proxy.loginUser(newUser.get("username"),newUser.get("password"));
        boolean result = proxy.logoutCurrentUser(session_id);
        result = proxy.logoutCurrentUser(session_id);
        assertFalse("logout twice should fail",result);
    }
    @Test
    public void testCreateSensorAndGetSensorWithValidParams() throws IOException, RuntimeException {
        // log in first in order to create sensor
        String session_id = proxy.loginUser(newUser.get("username"), newUser.get("password"));
        int sensorNumber = 0;
        String name = "test";
        String displayName = "test";
        String deviceType = "deviceType";
        String dataType = "json";
        String sessionID = session_id;
        String value = "{\"interval:\":0,\"data\":[2.23, 19.45, 20.2]}";
        String dataStructure = createDataStructure(value);

        // first check the sensor JSONObject has been created,
        JSONObject sensor = proxy.createSensor(name, displayName, deviceType, dataType, dataStructure, sessionID);
        assertNotNull("Failed to create a sensor", sensor);
        sensorNumber++;
        String sensorId;
        try {
            // then check if the sensor id has been returned from the server
            sensorId = sensor.get("sensor_id").toString();
        }catch(JSONException js){
            throw new RuntimeException("Failed to get the sensor id from sensor object");
        }
        assertEquals("The sensor id returned from the server is empty",false, sensorId.isEmpty());

        // check the sensor with valid session id
        JSONArray sensorList = proxy.getAllSensors(sessionID);
        assertEquals("Failed to get correct number of sensor", sensorNumber, sensorList.length());
    }
    @Test
    public void testCreateSensorMoreThanOne() throws IOException, RuntimeException {
        // log in first in order to create sensor
        String session_id = proxy.loginUser(newUser.get("username"), newUser.get("password"));
        int sensorNumber = 0;
        String name = "test";
        String displayName = "test";
        String deviceType = "deviceType";
        String dataType = "json";
        String sessionID = session_id;
        String value = "{\"interval:\":0,\"data\":[2.23, 19.45, 20.2]}";
        String dataStructure = createDataStructure(value);

        // first check the sensor JSONObject has been created,
        JSONObject sensor = proxy.createSensor(name, displayName, deviceType, dataType, dataStructure, sessionID);
        assertNotNull("Failed to create a sensor", sensor);
        sensorNumber++;

        String sensorId;
        try {
            // then check if the sensor id has been returned from the server
            sensorId = sensor.get("sensor_id").toString();
        }catch(JSONException js){
            throw new RuntimeException("Failed to get the sensor id from sensor object");
        }
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        // check the sensor with valid session id
        JSONArray sensorList = proxy.getAllSensors(sessionID);
        assertEquals("Failed to get correct number of sensor", sensorNumber, sensorList.length());

        name = "test1";
        displayName = "test1";
        // first check the sensor JSONObject has been created,
        sensor = proxy.createSensor(name, displayName, deviceType, dataType, dataStructure, sessionID);
        assertNotNull("Failed to create a sensor", sensor);
        sensorNumber++;

        try {
            // then check if the sensor id has been returned from the server
            sensorId = sensor.get("sensor_id").toString();
        }catch(JSONException js){
            throw new RuntimeException("Failed to get the sensor id from sensor object");
        }
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        // check the sensor with valid session id
        sensorList = proxy.getAllSensors(sessionID);
        assertEquals("Failed to get correct number of sensor", sensorNumber, sensorList.length());
    }
    @Test
    public void testCreateSensorWithEmptyName() throws IOException, RuntimeException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(startsWith("invalid input of name"));
        /*This should be the same as testing invalid dataType, deviceType and Session ID*/
        // log in first in order to create sensor
        String session_id = proxy.loginUser(newUser.get("username"), newUser.get("password"));
        String name = "";
        String displayName = "test";
        String deviceType = "deviceType";
        String dataType = "json";
        String sessionID = session_id;
        String value = "{\"interval\":0,\"data\":[2.23, 19.45, 20.2]}";
        String dataStructure = createDataStructure(value);

        // first check the sensor JSONObject has been created,
        JSONObject sensor = proxy.createSensor(name, displayName, deviceType, dataType, dataStructure, sessionID);
    }
    @Test
    public void testCreateSensorWithNullDisplayName() throws IOException, RuntimeException {
        // log in first in order to create sensor
        String session_id = proxy.loginUser(newUser.get("username"), newUser.get("password"));
        String name = "test";
        String deviceType = "deviceType";
        String dataType = "json";
        String sessionID = session_id;
        String value = "{\"interval\":0,\"data\":[2.23, 19.45, 20.2]}";
        String dataStructure = createDataStructure(value);

        // first check the sensor JSONObject has been created,
        JSONObject sensor = proxy.createSensor(name, null, deviceType, dataType, dataStructure, sessionID);
        assertNotNull("Failed to create a sensor", sensor);
    }
    @Test
    public void testCreateSensorWithNullDataStructure() throws IOException, RuntimeException {
        // log in first in order to create sensor
        String session_id = proxy.loginUser(newUser.get("username"), newUser.get("password"));
        String name = "test";
        String deviceType = "deviceType";
        String dataType = "json";
        String sessionID = session_id;

        // first check the sensor JSONObject has been created,
        JSONObject sensor = proxy.createSensor(name, null, deviceType, dataType, null, sessionID);
        assertNotNull("Failed to create a sensor", sensor);
    }
    @Test
    public void testGetAllSensorsWithNullSessionId() throws IOException, IllegalArgumentException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("getAllSensors: invalid input of sessionID");
        // log in first in order to create sensor
        proxy.loginUser(newUser.get("username"), newUser.get("password"));
        // check the sensor with valid session id
        JSONArray sensorList = proxy.getAllSensors(null);
    }
    @Test
    public void testGetAllSensorsWithEmptySessionId() throws IOException, IllegalArgumentException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("getAllSensors: invalid input of sessionID");
        // log in first in order to create sensor
        proxy.loginUser(newUser.get("username"), newUser.get("password"));
        // check the sensor with valid session id
        JSONArray sensorList = proxy.getAllSensors("");
    }
    @Test
    public void testAddSensorToDeviceWithValidParams() throws RuntimeException, IOException {
        // log in first in order to create sensor
        String session_id = proxy.loginUser(newUser.get("username"), newUser.get("password"));
        int deviceNumber = 0;
        String name = "test";
        String displayName = "test";
        String deviceType = "deviceType";
        String dataType = "json";
        String sessionID = session_id;
        String value = "{\"interval\":0,\"data\":[2.23, 19.45, 20.2]}";
        String dataStructure = createDataStructure(value);

        // first check the sensor JSONObject has been created,
        JSONObject sensor = proxy.createSensor(name, displayName, deviceType, dataType, dataStructure, sessionID);
        assertNotNull("Failed to create a sensor", sensor);

        String sensorId;
        try {
            // then check if the sensor id has been returned from the server
            sensorId = sensor.get("sensor_id").toString();
        }catch(JSONException js){
            throw new RuntimeException("Failed to get the sensor id from sensor object");
        }
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        // add the sensor to a device
        boolean result = proxy.addSensorToDevice(sensorId, "deviceType", "uuid", sessionID);
        assertEquals("Failed to add the sensor to a device", true,result);
        deviceNumber++;
        // check the actual device number with the expected device number
        JSONArray deviceList = proxy.getAllDevices(sessionID);
        assertEquals("The returned device number is wrong", deviceNumber, deviceList.length());
    }
    @Test
    public void testAddSensorToDeviceWithNullUuid() throws RuntimeException, IOException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(startsWith("invalid input of csSensorID"));
        // log in first in order to create sensor
        String session_id = proxy.loginUser(newUser.get("username"), newUser.get("password"));
        int deviceNumber = 0;
        String name = "test";
        String displayName = "test";
        String deviceType = "deviceType";
        String dataType = "json";
        String sessionID = session_id;
        String value = "{\"interval\":0,\"data\":[2.23, 19.45, 20.2]}";
        String dataStructure = createDataStructure(value);

        // first check the sensor JSONObject has been created,
        JSONObject sensor = proxy.createSensor(name, displayName, deviceType, dataType, dataStructure, sessionID);
        assertNotNull("Failed to create a sensor", sensor);

        String sensorId;
        try {
            // then check if the sensor id has been returned from the server
            sensorId = sensor.get("sensor_id").toString();
        }catch(JSONException js){
            throw new RuntimeException("Failed to get the sensor id from sensor object");
        }
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        // add the sensor to a device
        boolean result = proxy.addSensorToDevice(sensorId, "deviceType", null, sessionID);
        assertEquals("Failed to add the sensor to a device", true, result);
        deviceNumber++;
        // check the actual device number with the expected device number
        JSONArray deviceList = proxy.getAllDevices(sessionID);
        assertEquals("The returned device number is wrong", deviceNumber, deviceList.length());
    }
    @Test
    public void testGetAllDevicesWithSingleDevice() throws IllegalArgumentException, IOException {
        // log in first in order to create sensor
        String session_id = proxy.loginUser(newUser.get("username"), newUser.get("password"));
        int deviceNumber = 0;
        String name = "test";
        String displayName = "test";
        String deviceType = "deviceType";
        String dataType = "json";
        String sessionID = session_id;
        String value = "{\"interval\":0,\"data\":[2.23, 19.45, 20.2]}";
        String dataStructure = createDataStructure(value);

        // first check the sensor JSONObject has been created,
        JSONObject sensor = proxy.createSensor(name, displayName, deviceType, dataType, dataStructure, sessionID);
        assertNotNull("Failed to create a sensor", sensor);

        String sensorId;
        try {
            // then check if the sensor id has been returned from the server
            sensorId = sensor.get("sensor_id").toString();
        }catch(JSONException js){
            throw new RuntimeException("Failed to get the sensor id from sensor object");
        }
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        // add the sensor to a device
        boolean result = proxy.addSensorToDevice(sensorId, "deviceType", "uuid", sessionID);
        assertEquals("Failed to add the sensor to a device", true,result);
        deviceNumber++;

        JSONArray deviceList = proxy.getAllDevices(sessionID);
        assertNotNull("Failed to get the list of devices", deviceList);
        assertEquals("Incorrect device number", deviceNumber,deviceList.length());

    }
    @Test
    public void testGetAllDevicesWithTwoDevices() throws IllegalArgumentException, IOException {
        // log in first in order to create sensor
        String session_id = proxy.loginUser(newUser.get("username"), newUser.get("password"));
        int deviceNumber = 0;
        String name = "test";
        String displayName = "test";
        String deviceType = "deviceType";
        String dataType = "json";
        String sessionID = session_id;
        String value = "{\"interval\":0,\"data\":[2.23, 19.45, 20.2]}";
        String dataStructure = createDataStructure(value);

        // first check the sensor JSONObject has been created,
        JSONObject sensor = proxy.createSensor(name, displayName, deviceType, dataType, dataStructure, sessionID);
        assertNotNull("Failed to create a sensor", sensor);

        String sensorId;
        try {
            // then check if the sensor id has been returned from the server
            sensorId = sensor.get("sensor_id").toString();
        }catch(JSONException js){
            throw new RuntimeException("Failed to get the sensor id from sensor object");
        }
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        // add the sensor to a device
        boolean result = proxy.addSensorToDevice(sensorId, "deviceType", "uuid", sessionID);
        deviceNumber++;
        assertEquals("Failed to add the sensor to a device", true,result);

        JSONArray deviceList = proxy.getAllDevices(sessionID);
        assertNotNull("Failed to get the list of devices", deviceList);
        assertEquals("Incorrect device number", deviceNumber, deviceList.length());

        name = "test1";
        displayName = "test1";
        deviceType = "deviceType1";
        value = "{\"interval:\":0,\"data\":[2.23, 19.45, 20.0]}";
        dataStructure = createDataStructure(value);

        // first check the sensor JSONObject has been created,
        sensor = proxy.createSensor(name, displayName, deviceType, dataType, dataStructure, sessionID);
        assertNotNull("Failed to create a sensor", sensor);

        try {
            // then check if the sensor id has been returned from the server
            sensorId = sensor.get("sensor_id").toString();
        }catch(JSONException js){
            throw new RuntimeException("Failed to get the sensor id from sensor object");
        }
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        // add the sensor to a device
        result = proxy.addSensorToDevice(sensorId, "deviceType1", "uuid1", sessionID);
        deviceNumber++;
        assertEquals("Failed to add the sensor to a device", true,result);

        deviceList = proxy.getAllDevices(sessionID);
        assertNotNull("Failed to get the list of devices", deviceList);
        assertEquals("Incorrect device number", deviceNumber, deviceList.length());

    }
    @Test
    public void testPostDataWithInvalidDataFormat() throws RuntimeException, IOException{
        // log in first in order to create sensor
        String session_id = proxy.loginUser(newUser.get("username"), newUser.get("password"));
        String name = "test";
        String displayName = "test";
        String deviceType = "deviceType";
        String dataType = "json";
        String sessionID = session_id;
        String value = "{\"interval\":0,\"data\":[2.23, 19.45, 20.2]}";
        String dataStructure = createDataStructure(value);

        // first check the sensor JSONObject has been created,
        JSONObject sensor = proxy.createSensor(name, displayName, deviceType, dataType, dataStructure, sessionID);
        assertNotNull("Failed to create a sensor", sensor);

        String sensorId;
        try {
            // then check if the sensor id has been returned from the server
            sensorId = sensor.get("sensor_id").toString();
        }catch(JSONException js){
            throw new RuntimeException("Failed to get the sensor id from sensor object");
        }
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        JSONArray postData = new JSONArray();
        JSONObject data = new JSONObject();
        try {
            //This is the invalid data format
            data.put("sensor", sensorId);
            data.put("data", "1234");
        }catch(JSONException js){
            throw new RuntimeException("Failed to put sensor info to a JSON object");
        }
        postData.put(data);

        boolean result = proxy.postData(postData,sessionID);
        assertEquals("postData should fail with invalid DataFormat",false, result);

    }
    @Test
    public void testPostDataAndGetDataWithValidParams()throws RuntimeException, IOException{
        // log in first in order to create sensor
        String session_id = proxy.loginUser(newUser.get("username"), newUser.get("password"));
        String name = "test";
        String displayName = "test";
        String deviceType = "deviceType";
        String dataType = "json";
        String sessionID = session_id;
        String value = "{\"interval\":0,\"data\":[2.23, 19.45, 20.2]}";
        String dataStructure = createDataStructure(value);

        // first check the sensor JSONObject has been created,
        JSONObject sensor = proxy.createSensor(name, displayName, deviceType, dataType, dataStructure, sessionID);
        assertNotNull("Failed to create a sensor", sensor);

        String sensorId;
        try {
            // then check if the sensor id has been returned from the server
            sensorId = sensor.get("sensor_id").toString();
        }catch(JSONException js){
            throw new RuntimeException("Failed to get the sensor id from sensor object");
        }
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        JSONArray postData = createDataPoint(sensorId);
        SNTP sntp = new SNTP();
        double fromDate =  sntp.getTime()/1000;
        boolean result = proxy.postData(postData,sessionID);
        assertEquals("Failed to post data to the server",true, result);

        JSONArray getData = proxy.getData(sensorId, fromDate, sessionID);
        assertEquals("Failed to get data from the server", true, (getData.length()!=0));

    }
    @Test
    public void testPostDataAndGetDataWithInvalidParams()throws RuntimeException, IOException{
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("invalid input of date or or sensorID or sessionID");
        // log in first in order to create sensor
        String session_id = proxy.loginUser(newUser.get("username"), newUser.get("password"));
        String name = "test";
        String displayName = "test";
        String deviceType = "deviceType";
        String dataType = "json";
        String sessionID = session_id;
        String value = "{\"interval\":0,\"data\":[2.23, 19.45, 20.2]}";
        String dataStructure = createDataStructure(value);

        // first check the sensor JSONObject has been created,
        JSONObject sensor = proxy.createSensor(name, displayName, deviceType, dataType, dataStructure, sessionID);
        assertNotNull("Failed to create a sensor", sensor);

        String sensorId;
        try {
            // then check if the sensor id has been returned from the server
            sensorId = sensor.get("sensor_id").toString();
        }catch(JSONException js){
            throw new RuntimeException("Failed to get the sensor id from sensor object");
        }
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        JSONArray postData = createDataPoint(sensorId);
        SNTP sntp = new SNTP();
        float fromDate =  sntp.getTime()/1000;
        boolean result = proxy.postData(postData,sessionID);
        assertEquals("Failed to post data to the server",true, result);

        //get data with null session id
        JSONArray getData = proxy.getData(sensorId, fromDate, null);
        assertEquals("No data should be returned from the server with null session id", true, (getData.length()==0));

    }
    @Test
    public void testGetDataWithInvalidDate()throws IllegalArgumentException, IOException{
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(startsWith("start date cannot be after current date"));
        // log in first in order to create sensor
        String session_id = proxy.loginUser(newUser.get("username"), newUser.get("password"));
        int deviceNumber = 0;
        String name = "test";
        String displayName = "test";
        String deviceType = "deviceType";
        String dataType = "json";
        String sessionID = session_id;
        String value = "{\"interval\":0,\"data\":[2.23, 19.45, 20.2]}";
        String dataStructure = createDataStructure(value);

        // first check the sensor JSONObject has been created,
        JSONObject sensor = proxy.createSensor(name, displayName, deviceType, dataType, dataStructure, sessionID);
        assertNotNull("Failed to create a sensor", sensor);

        String sensorId;
        try {
            // then check if the sensor id has been returned from the server
            sensorId = sensor.get("sensor_id").toString();
        }catch(JSONException js){
            throw new RuntimeException("Failed to get the sensor id from sensor object");
        }
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        JSONArray postData = createDataPoint(sensorId);
        boolean result = proxy.postData(postData,sessionID);
        assertEquals("Failed to post data to the server",true, result);
        SNTP sntp = new SNTP();
        double fromDate = (sntp.getTime() + 1000 * 60 * 60 * 24 * 7)/1000;
        JSONArray getData = proxy.getData(sensorId, fromDate, sessionID);

        assertEquals("Failed to get data from the server", true, (getData.length()!=0));

    }

    private JSONArray createDataPoint(String sensorId) throws RuntimeException {
        JSONObject value = new JSONObject();
        JSONObject dataPoint = new JSONObject();
        JSONArray dataPoints = new JSONArray();
        JSONObject data = new JSONObject();
        JSONArray dataArray = new JSONArray();
        dataArray.put(data);
        try {
            value.put("value1", 1);
            value.put("value2", 2);
            value.put("value3", 3);


            dataPoint.put("value", value);
            SNTP sntp = new SNTP();
            double time = sntp.getTime() / 1000;
            dataPoint.put("date", time);

            dataPoints.put(dataPoint);

            data.put("sensor_id", sensorId);
            data.put("data", dataPoints);

        }catch(JSONException js){
            throw new RuntimeException("Failed to create Data Point");
        }
        return dataArray;
    }

    private String createDataStructure(String value) throws RuntimeException {
        JSONObject dataStructJSon ;
        try {
            dataStructJSon = new JSONObject(value);
            JSONArray fieldNames = dataStructJSon.names();
            for (int x = 0; x < fieldNames.length(); x++) {
                String fieldName = fieldNames.getString(x);
                int start = dataStructJSon.get(fieldName).getClass().getName().lastIndexOf(".");
                dataStructJSon.put(fieldName, dataStructJSon.get(fieldName).getClass()
                        .getName().substring(start + 1));

            }
        }catch(JSONException js){
            throw new RuntimeException("Failed to create Data Structure");
        }
        return dataStructJSon.toString().replaceAll("\"", "\\\"");
    }

    @After
    public void tearDown() throws IOException{

        CSUtils.deleteAccount(newUser.get("username"), newUser.get("password"),newUser.get("id"));
    }
}
