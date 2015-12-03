# Data Storage Engine {#data_storage}

The Data Storage Engine is used for storing the sensor data of the sensors. The SenseService is in charge of setting the right configurations for the `DataStorageEngine` instance. Upon login the necessary configuration settings are read from the shared preferences and are provided to the `DataStorageEngine` via the `setConfig` function. After this the `DataStorageEngine` will try to initialize: download the sensor profiles and user specific sensors.

The SenseService has an ErrorCallback registered to the `DataStorageEngine` to receive errors occurring in the background. If there are authorization problems in the back-end a re-login is performed in the SenseService which will trigger the on login process described above.

## Storing sensor data
Data from the sensors is stored in the `DataStorageEngine` using a specific `DataConsumer`, the `DSEDataConsumer`, that subscribes to all the sensors that exist in the sensor profiles. Sensor data storage and retrieval is done via a source and sensor specific `nl.sense_os.datastorageengine.Sensor` instance.

### Manual storage
Sensor data can be stored manually by calling the function `Sensor.insertOrUpdateDataPoint(Object value, long time)` after the appropriate sensor has been retrieved with `DataStorageEngine.getSensor(String source, String sensorName)`. This function automatically creates the sensor with the default `SensorOptions` if it does not exist already. Sensor data is only accepted if the provided `value` matches the sensor profile.

## Retrieving stored data {#retrieving_sensor_data}
Stored sensor data can be queried for a specific sensor and source using the method `Sensor.getDataPoints(QueryOptions queryOptions)`. This method takes a QueryOptions object for which the following properties can be set:
* startTime: start time in UTC milliseconds (inclusive). If null, no condition for startTime.
* endTime: end time in UTC milliseconds (not inclusive). If null, no condition for endTime.
* existsInRemote: Boolean value for whether the datapoint exists in CS. If null, no condition.
* limit: maximum number of data points. No limit if null.
* sortOrder: sorting order. it could either be DESC or ASC..
* interval: such as MINUTE, HOUR, DAY, WEEK

## Default Sensor Options {#default_sensor_options}
Default SensorOptions are used when a sensor is created for the first time. This can be when initializing the `DataStorageEngine` or when accessing a sensor. Default SensorOptions can be defined for each sensor in the sensor profiles by defining it statically in the string resource file located at `res/values/default_sensor_options.xml`. In this document the SensorOptions can be defined by adding a JSON object to the JSON array with the following fields as defined in `DefaultSensorOptions`:

```json
{
    "sensor_name": <String> The sensor name as defined in the sensor profiles,
    "persist_locally": <boolean> Whether to persist the data locally,
    "upload_enabled": <boolean> Whether to upload the data to the back-end,
    "download_enabled": <boolean> Whether to download the data from the back-end,
    "meta": <String> Default meta data to use for the sensor
}
```

Example for the accelerometer
```json
{
    "sensor_name": "accelerometer",
    "persist_locally": false,
    "upload_enabled": true,
    "download_enabled": false
}
```

Default SensorOptions can also be set pragmatically for a sensor via de function `DefaultSensorOptions.setDefaultSensorOptions`. N.B. This should be done before the sensor is created in the database in order to take these options into account.


## Retention Rate {#retention_rate}

Old data in persistent database will be deleted after it exceeds the retention time (and has been sent to the back-end, if applicable). The Retention Time is gathered from preferences (see nl.sense_os.service.constants.SensePrefs.Main.Advanced.RETENTION_HOURS), and has a default value of 31 days (specified in DataSyncer.PERSIST_PERIOD).
