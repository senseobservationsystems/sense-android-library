# Data Storage Engine {#data_storage}

The Data Storage Engine is used for storing the sensor data of the sensors. The SenseService is in charge of setting the right configurations for the `DataStorageEngine` instance. Upon login the necessary configuration settings are read from the shared preferences and are provided to the `DataStorageEngine` via the `setConfig` function. After this the `DataStorageEngine` will try to initialize: download the sensor profiles and user specific sensors.

The SenseService has an ErrorCallback registered to the `DataStorageEngine` to receive errors occurring in the background. If there are authorization problems in the back-end a re-login is performed in the SenseService which will trigger the on login process described above.

## Storing Sensor data
Data from the sensors is stored in the `DataStorageEngine` using a specific `DataConsumer`, the `DSEDataConsumer`, that subscribes to all the sensors that exist in the sensor profiles.

### Manual storage
Sensor data can be stored manually by calling the function `Sensor.insertOrUpdateDataPoint(Object value, long time)` after the appropriate sensor has been retrieved with `DataStorageEngine.getSensor(String source, String sensorName)`.

## Retrieving data {#retrieving_sensor_data}


## Default Sensor Options {#default_sensor_options}

## Retention Rate {#retention_rate}

Old data in persistent database will be deleted after it exceeds the retention time (and has been sent to CommonSense, if applicable). The Retention Time is gathered from preferences (see nl.sense_os.service.constants.SensePrefs.Main.Advanced.RETENTION_HOURS), and has a default value of 24 hours (see nl.sense_os.service.storage.LocalStorage.DEFAULT_RETENTION_HOURS).
