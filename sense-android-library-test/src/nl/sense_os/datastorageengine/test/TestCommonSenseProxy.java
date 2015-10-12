package nl.sense_os.datastorageengine.test;

import android.test.AndroidTestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.util.Map;

import nl.sense_os.datastorageengine.CommonSenseProxy;
import nl.sense_os.service.provider.SNTP;

public class TestCommonSenseProxy extends AndroidTestCase {

    Map<String, String> newUser;
    String session_id;
    CSUtils csUtils = new CSUtils(false);
    CommonSenseProxy proxy = new CommonSenseProxy(false, csUtils.APP_KEY);

    public  void setUp () throws IOException {
        newUser = csUtils.createCSAccount();
        session_id = csUtils.loginUser(newUser.get("username"), newUser.get("password"));
    }

    public void tearDown() throws IOException {
        csUtils.deleteAccount(newUser.get("username"), newUser.get("password"),newUser.get("id"));
    }

    public void testCreateSensorAndGetSensorWithValidParams() throws IOException, RuntimeException {

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

    public void testCreateSensorMoreThanOne() throws IOException, RuntimeException {

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

    /*This should be the same as testing invalid dataType, deviceType and Session ID*/
    public void testCreateSensorWithEmptyName() throws IOException, RuntimeException {
        try {
            String name = "";
            String displayName = "test";
            String deviceType = "deviceType";
            String dataType = "json";
            String sessionID = session_id;
            String value = "{\"interval\":0,\"data\":[2.23, 19.45, 20.2]}";
            String dataStructure = createDataStructure(value);

            // first check the sensor JSONObject has been created,
            JSONObject sensor = proxy.createSensor(name, displayName, deviceType, dataType, dataStructure, sessionID);

            fail( "Missing exception" );
        } catch( IllegalArgumentException e ) {
            assertTrue( "Should throw the right exception message",
                    e.getMessage().startsWith("invalid input of name") );
        } catch (Exception e) {
            fail( "Wrong type of exception");
        }
    }

    public void testCreateSensorWithNullDisplayName() throws IOException, RuntimeException {

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

    public void testCreateSensorWithNullDataStructure() throws IOException, RuntimeException {

        String name = "test";
        String deviceType = "deviceType";
        String dataType = "json";
        String sessionID = session_id;

        // first check the sensor JSONObject has been created,
        JSONObject sensor = proxy.createSensor(name, null, deviceType, dataType, null, sessionID);
        assertNotNull("Failed to create a sensor", sensor);
    }

    public void testGetAllSensorsWithNullSessionId() throws IOException, IllegalArgumentException {
        try {
            // check the sensor with valid session id
            proxy.getAllSensors(null);

            fail("Missing exception");
        } catch( IllegalArgumentException e ) {
            assertEquals("Should throw the right exception message",
                    "getAllSensors: invalid input of sessionID", e.getMessage());
        } catch (Exception e) {
            fail( "Wrong type of exception");
        }
    }

    public void testGetAllSensorsWithEmptySessionId() throws IOException, IllegalArgumentException {
        try {
            // check the sensor with valid session id
            proxy.getAllSensors("");

            fail("Missing exception");
        } catch( IllegalArgumentException e ) {
            assertEquals( "Should throw the right exception message",
                    "getAllSensors: invalid input of sessionID", e.getMessage() );
        } catch (Exception e) {
            fail( "Wrong type of exception");
        }
    }

    public void testAddSensorToDeviceWithValidParams() throws RuntimeException, IOException {
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

    public void testAddSensorToDeviceWithNullUuid() throws RuntimeException, IOException {
        try {
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

            fail("Missing exception");
        } catch( IllegalArgumentException e ) {
            assertTrue("Should throw the right exception message",
                    e.getMessage().startsWith("invalid input of csSensorID") );
        } catch (Exception e) {
            fail( "Wrong type of exception");
        }
    }

    public void testGetAllDevicesWithSingleDevice() throws IllegalArgumentException, IOException {
        int deviceNumber = 0;
        String name = "test";
        String displayName = "test";
        String deviceType = "deviceType";
        String uuid = "uuid";
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

        // before adding sensor, check the number of device
        JSONArray deviceList = proxy.getAllDevices(sessionID);
        assertEquals("No devices should be created", deviceNumber, deviceList.length());
        // add the sensor to a device
        boolean result = proxy.addSensorToDevice(sensorId, "deviceType", uuid, sessionID);
        assertEquals("Failed to add the sensor to a device", true,result);
        deviceNumber++;

        // after adding sensor, check the number of device
        deviceList = proxy.getAllDevices(sessionID);
        assertNotNull("Failed to get the list of devices", deviceList);
        assertEquals("Incorrect device number", deviceNumber,deviceList.length());
        String returnedDeviceType;
        String returnedUUID;
        try {
            returnedDeviceType = deviceList.getJSONObject(0).getString("type");
            returnedUUID = deviceList.getJSONObject(0).getString("uuid");
        }catch(JSONException js){
            throw new RuntimeException("failed to get a sensor from the device list");
        }
        assertEquals("Incorrect device Type",deviceType,returnedDeviceType);
        assertEquals("Incorrect UUID", uuid, returnedUUID);
    }

    public void testGetAllDevicesWithTwoDevices() throws IllegalArgumentException, IOException {
        int deviceNumber = 0;
        String name = "test";
        String displayName = "test";
        String deviceType = "deviceType";
        String uuid = "uuid";
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

        // before adding sensor, check the number of device
        JSONArray deviceList = proxy.getAllDevices(sessionID);
        assertEquals("No devices should be created", deviceNumber, deviceList.length());

        // add the sensor to a device
        boolean result = proxy.addSensorToDevice(sensorId, deviceType, uuid, sessionID);
        deviceNumber++;
        assertEquals("Failed to add the sensor to a device", true,result);

        deviceList = proxy.getAllDevices(sessionID);
        assertNotNull("Failed to get the list of devices", deviceList);
        assertEquals("Incorrect device number", deviceNumber, deviceList.length());

        String returnedDeviceType;
        String returnedUUID;
        try {
            returnedDeviceType = deviceList.getJSONObject(0).getString("type");
            returnedUUID = deviceList.getJSONObject(0).getString("uuid");
        }catch(JSONException js){
            throw new RuntimeException("failed to get a sensor from the device list");
        }
        assertEquals("Incorrect device Type",deviceType,returnedDeviceType);
        assertEquals("Incorrect UUID","uuid",returnedUUID);

        String name1 = "test1";
        String displayName1 = "test1";
        String deviceType1 = "deviceType1";
        String uuid1 = "uuid1";
        value = "{\"interval:\":0,\"data\":[2.23, 19.45, 20.0]}";
        dataStructure = createDataStructure(value);

        // first check the sensor JSONObject has been created,
        sensor = proxy.createSensor(name1, displayName1, deviceType1, dataType, dataStructure, sessionID);
        assertNotNull("Failed to create a sensor", sensor);

        try {
            // then check if the sensor id has been returned from the server
            sensorId = sensor.get("sensor_id").toString();
        }catch(JSONException js){
            throw new RuntimeException("Failed to get the sensor id from sensor object");
        }
        assertEquals("The new sensorid returned from the server is empty",false, sensorId.isEmpty());

        deviceList = proxy.getAllDevices(sessionID);
        assertEquals("No devices should be created", deviceNumber, deviceList.length());

        // add the sensor to a device
        result = proxy.addSensorToDevice(sensorId, deviceType1, uuid1, sessionID);
        deviceNumber++;
        assertEquals("Failed to add the sensor to a device", true,result);

        deviceList = proxy.getAllDevices(sessionID);
        assertNotNull("Failed to get the list of devices", deviceList);
        assertEquals("Incorrect device number", deviceNumber, deviceList.length());
        // be aware that addSensorToDevice, the newly created device is stored at the head of the list
        try {
            // note that the returned list with devices is unordered
            if (deviceList.getJSONObject(0).getString("uuid").equals(uuid)) {
                // first entry contains uuid and deviceType
                assertEquals("Incorrect device Type", deviceType,  deviceList.getJSONObject(0).getString("type"));

                // second entry should contain uuid1 and deviceType1
                assertEquals("Incorrect UUID",        uuid1,       deviceList.getJSONObject(1).getString("uuid"));
                assertEquals("Incorrect device Type", deviceType1, deviceList.getJSONObject(1).getString("type"));
            }
            else if (deviceList.getJSONObject(0).getString("uuid").equals(uuid1)) {
                // first entry should contain uuid1 and deviceType1
                assertEquals("Incorrect device Type", deviceType1, deviceList.getJSONObject(0).getString("type"));

                // second entry contains uuid and deviceType
                assertEquals("Incorrect UUID",        uuid1,       deviceList.getJSONObject(1).getString("uuid"));
                assertEquals("Incorrect device Type", deviceType,  deviceList.getJSONObject(1).getString("type"));
            }
            else {
                fail("Wrong uuid in list with devices");
            }
        }catch(JSONException js){
            throw new RuntimeException("failed to get a sensor from the device list");
        }
    }

    public void testPostDataWithInvalidDataFormat() throws RuntimeException, IOException{
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

        boolean result = proxy.postData(postData, sessionID);
        assertEquals("postData should fail with invalid DataFormat",false, result);

    }

    public void testPostDataAndGetDataWithValidParams()throws RuntimeException, IOException{
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

    public void testPostDataAndGetDataWithInvalidParams()throws RuntimeException, IOException{
        try {
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
            } catch (JSONException js) {
                throw new RuntimeException("Failed to get the sensor id from sensor object");
            }
            assertEquals("The new sensorid returned from the server is empty", false, sensorId.isEmpty());

            JSONArray postData = createDataPoint(sensorId);
            SNTP sntp = new SNTP();
            float fromDate = sntp.getTime() / 1000;
            boolean result = proxy.postData(postData, sessionID);
            assertEquals("Failed to post data to the server", true, result);

            //get data with null session id
            JSONArray getData = proxy.getData(sensorId, fromDate, null);

            fail("Missing exception");
        } catch( IllegalArgumentException e ) {
            assertEquals("Should throw the right exception message",
                    "invalid input of date or or sensorID or sessionID", e.getMessage());
        } catch (Exception e) {
            fail( "Wrong type of exception");
        }
    }

    public void testGetDataWithInvalidDate()throws IllegalArgumentException, IOException{
        try {
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

            fail("Missing exception");
        } catch( IllegalArgumentException e ) {
            assertTrue("Should throw the right exception message",
                    e.getMessage().startsWith("start date cannot be after current date"));
        } catch (Exception e) {
            fail( "Wrong type of exception");
        }
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
}
