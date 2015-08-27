package nl.sense_os.datastorageengine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Config;
import static org.hamcrest.core.StringStartsWith.startsWith;
import java.io.IOException;
import java.util.Map;

import nl.sense_os.service.BuildConfig;

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
    public void testLoginUserWithValidUsernameAndValidPassword() throws IOException, JSONException{
        String session_id = proxy.loginUser(newUser.get("username"),newUser.get("password"));
        assertNotNull("session_id returned from server is null", session_id);
        assertFalse("session_id returned from server is empty",session_id.isEmpty());
    }
    @Test
    public void testLoginUserWithValidUsernameAndWrongPassword() throws IOException, JSONException{
        thrown.expect(IOException.class);
        thrown.expectMessage("could not get InputStream");
        proxy.loginUser(newUser.get("username"), "123456789");
    }
    @Test
    public void testLoginUserWithValidUsernameAndNullPassword() throws IOException, JSONException{
        thrown.expect(IOException.class);
        thrown.expectMessage("invalid input of username or password");
        proxy.loginUser(newUser.get("username"),null);
    }
    @Test
    public void testLoginUserWithValidUsernameAndEmptyPassword() throws IOException, JSONException{
        thrown.expect(IOException.class);
        thrown.expectMessage("invalid input of username or password");
        proxy.loginUser(newUser.get("username"),"");
    }
    @Test
    public void testLoginUserWithNullUsernameAndValidPassword() throws IOException, JSONException{
        thrown.expect(IOException.class);
        thrown.expectMessage("invalid input of username or password");
        proxy.loginUser(null,newUser.get("password"));
    }
    @Test
    public void testLoginUserWithEmptyUsernameAndValidPassword() throws IOException, JSONException{
        thrown.expect(IOException.class);
        thrown.expectMessage("invalid input of username or password");
    }
    @Test
    public void testLogoutCurrentUserWithValidSessionID() throws IOException, JSONException{
        // log in first in order to log out
        String session_id = proxy.loginUser(newUser.get("username"), newUser.get("password"));
        boolean result = proxy.logoutCurrentUser(session_id);
        assertTrue("current user cannot be successfully logged out", result);
    }
    @Test
    public void testLogoutCurrentUserWithNullSessionID() throws IOException, JSONException{
        thrown.expect(IOException.class);
        thrown.expectMessage("invalid input of session ID");
        proxy.loginUser(newUser.get("username"),newUser.get("password"));
        boolean result = proxy.logoutCurrentUser(null);
    }
    @Test
    public void testLogoutCurrentUserWithEmptySessionID() throws IOException, JSONException{
        thrown.expect(IOException.class);
        thrown.expectMessage("invalid input of session ID");
        // log in first in order to log out
        proxy.loginUser(newUser.get("username"),newUser.get("password"));
        boolean result = proxy.logoutCurrentUser("");
    }
    @Test
    public void testLogoutCurrentUserWithWrongSessionID() throws IOException, JSONException{
        thrown.expect(IOException.class);
        thrown.expectMessage("logout with session id failed");
        // log in first in order to log out
        proxy.loginUser(newUser.get("username"),newUser.get("password"));
        boolean result = proxy.logoutCurrentUser("987654321");
    }
    @Test
    public void testLogoutCurrentUserTwice() throws IOException, JSONException{
        thrown.expect(IOException.class);
        thrown.expectMessage("logout with session id failed");
        // log in first in order to log out
        String session_id = proxy.loginUser(newUser.get("username"),newUser.get("password"));
        boolean result = proxy.logoutCurrentUser(session_id);
        result = proxy.logoutCurrentUser(session_id);
    }
    @Test
    public void testCreateSensorAndGetSensorWithValidParams() throws IOException, JSONException {
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

        // then check if the sensor id has been returned from the server
        String sensorId = sensor.get("sensor_id").toString();
        assertEquals("The sensor id returned from the server is empty",false, sensorId.isEmpty());

        // check the sensor with valid session id
        JSONArray sensorList = proxy.getAllSensors(sessionID);
        assertEquals("Failed to get correct number of sensor", sensorNumber, sensorList.length());
    }
    @Test
    public void testCreateSensorMoreThanOne() throws IOException, JSONException {
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

        // then check if the sensorid has been returned from the server
        String sensorId = sensor.get("sensor_id").toString();
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

        // then check if the sensorid has been returned from the server
        sensorId = sensor.get("sensor_id").toString();
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        // check the sensor with valid session id
        sensorList = proxy.getAllSensors(sessionID);
        assertEquals("Failed to get correct number of sensor", sensorNumber, sensorList.length());
    }
    @Test
    public void testCreateSensorWithEmptyName() throws IOException, JSONException {
        thrown.expect(IOException.class);
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
    public void testCreateSensorWithNullDisplayName() throws IOException, JSONException {
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
    public void testCreateSensorWithNullDataStructure() throws IOException, JSONException {
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
    public void testGetAllSensorsWithNullSessionId() throws IOException, JSONException {
        thrown.expect(IOException.class);
        thrown.expectMessage("getAllSensors: invalid input of sessionID");
        // log in first in order to create sensor
        proxy.loginUser(newUser.get("username"), newUser.get("password"));

        // check the sensor with valid session id
        JSONArray sensorList = proxy.getAllSensors(null);
    }
    @Test
    public void testGetAllSensorsWithEmptySessionId() throws IOException, JSONException {
        thrown.expect(IOException.class);
        thrown.expectMessage("getAllSensors: invalid input of sessionID");
        // log in first in order to create sensor
        proxy.loginUser(newUser.get("username"), newUser.get("password"));

        // check the sensor with valid session id
        JSONArray sensorList = proxy.getAllSensors("");
    }
    @Test
    public void testAddSensorWithValidParams() throws IOException, JSONException {
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

        // then check if the sensorid has been returned from the server
        String sensorId = sensor.get("sensor_id").toString();
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        // add the sensor to a device
        boolean result = proxy.addSensor(sensorId,"deviceType","uuid",sessionID);
        assertEquals("Failed to add the sensor to a device", true,result);
    }
    @Test
    public void testAddSensorWithNullUuid() throws IOException, JSONException {
        thrown.expect(IOException.class);
        thrown.expectMessage(startsWith("invalid input of csSensorID"));
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

        // then check if the sensorid has been returned from the server
        String sensorId = sensor.get("sensor_id").toString();
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        // add the sensor to a device
        boolean result = proxy.addSensor(sensorId,"deviceType",null,sessionID);
        assertEquals("Failed to add the sensor to a device", true,result);
    }
    @Test
    public void testGetAllDevicesWithSingleDevice() throws IOException, JSONException {
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

        // then check if the sensorid has been returned from the server
        String sensorId = sensor.get("sensor_id").toString();
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        // add the sensor to a device
        boolean result = proxy.addSensor(sensorId,"deviceType","uuid",sessionID);
        assertEquals("Failed to add the sensor to a device", true,result);
        deviceNumber++;

        JSONArray deviceList = proxy.getAllDevices(sessionID);
        assertNotNull("Failed to get the list of devices", deviceList);
        assertEquals("Incorrect device number", deviceNumber,deviceList.length());

    }
    @Test
    public void testGetAllDevicesWithTwoDevices() throws IOException, JSONException {
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

        // then check if the sensorid has been returned from the server
        String sensorId = sensor.get("sensor_id").toString();
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        // add the sensor to a device
        boolean result = proxy.addSensor(sensorId,"deviceType","uuid",sessionID);
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

        // then check if the sensorid has been returned from the server
        sensorId = sensor.get("sensor_id").toString();
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        // add the sensor to a device
        result = proxy.addSensor(sensorId,"deviceType1","uuid1",sessionID);
        deviceNumber++;
        assertEquals("Failed to add the sensor to a device", true,result);

        deviceList = proxy.getAllDevices(sessionID);
        assertNotNull("Failed to get the list of devices", deviceList);
        assertEquals("Incorrect device number", deviceNumber, deviceList.length());

    }
    @Test
    public void testPostDataWithInvalidDataFormat() throws IOException, JSONException{
        thrown.expect(IOException.class);
        thrown.expectMessage(startsWith("Incorrect response of postData from CommonSense"));
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

        // then check if the sensorid has been returned from the server
        String sensorId = sensor.get("sensor_id").toString();
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        JSONArray postData = new JSONArray();
        JSONObject data = new JSONObject();
        //This is the invalid data format
        data.put("sensor", sensorId);
        data.put("data","1234");
        postData.put(data);

        boolean result = proxy.postData(postData,sessionID);
        assertEquals("Failed to post data to the server",true, result);

    }
    @Test
    public void testPostDataAndGetDataWithValidParams()throws IOException, JSONException{
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

        // then check if the sensorid has been returned from the server
        String sensorId = sensor.get("sensor_id").toString();
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        JSONArray postData = createDataPoint(sensorId);

        double fromDate = System.currentTimeMillis()/1000;
        boolean result = proxy.postData(postData,sessionID);
        assertEquals("Failed to post data to the server",true, result);

        JSONArray getData = proxy.getData(sensorId, fromDate, sessionID);
        assertEquals("Failed to get data from the server", true, (getData.length()!=0));

    }
    @Test
    public void testPostDataAndGetDataWithInvalidParams()throws IOException, JSONException{
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

        // then check if the sensorid has been returned from the server
        String sensorId = sensor.get("sensor_id").toString();
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        JSONArray postData = createDataPoint(sensorId);
        float fromDate = System.currentTimeMillis()/1000;
        boolean result = proxy.postData(postData,sessionID);
        assertEquals("Failed to post data to the server",true, result);

        JSONArray getData = proxy.getData(sensorId, fromDate, null);
        assertEquals("Failed to get data from the server", true, (getData.length()!=0));

    }
    @Test
    public void testGetDataWithInvalidDate()throws IOException, JSONException{
        thrown.expect(IOException.class);
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

        // then check if the sensorid has been returned from the server
        String sensorId = sensor.get("sensor_id").toString();
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        JSONArray postData = createDataPoint(sensorId);
        boolean result = proxy.postData(postData,sessionID);
        assertEquals("Failed to post data to the server",true, result);

        double fromDate = (System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7)/1000;
        JSONArray getData = proxy.getData(sensorId, fromDate, sessionID);

        assertEquals("Failed to get data from the server", true, (getData.length()!=0));

    }

    private JSONArray createDataPoint(String sensorId) throws JSONException {
        JSONObject value = new JSONObject();
        value.put("value1",1);
        value.put("value2",2);
        value.put("value3",3);

        JSONObject dataPoint = new JSONObject();
        dataPoint.put("value",value);
        double time = System.currentTimeMillis()/1000;
        dataPoint.put("date",time);

        JSONArray dataPoints = new JSONArray();
        dataPoints.put(dataPoint);

        JSONObject data = new JSONObject();
        data.put("sensor_id", sensorId);
        data.put("data", dataPoints);

        JSONArray dataArray = new JSONArray();
        dataArray.put(data);

        return dataArray;
    }

    private String createDataStructure(String value) throws JSONException{
        JSONObject dataStructJSon = new JSONObject(value);
        JSONArray fieldNames = dataStructJSon.names();
        for (int x = 0; x < fieldNames.length(); x++) {
            String fieldName = fieldNames.getString(x);
            int start = dataStructJSon.get(fieldName).getClass().getName().lastIndexOf(".");
            dataStructJSon.put(fieldName, dataStructJSon.get(fieldName).getClass()
                    .getName().substring(start + 1));

        }
        return dataStructJSon.toString().replaceAll("\"", "\\\"");
    }

    @After
    public void tearDown() throws IOException{

        CSUtils.deleteAccount(newUser.get("username"), newUser.get("password"),newUser.get("id"));
    }
}
