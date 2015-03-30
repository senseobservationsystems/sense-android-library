# Data Storage {#data_storage}

Sensor data can be stored in and obtained from a local database as well as CommonSense. The data will be stored in the local database first, and be transmitted to CommonSense periodically (see [MsgHandler](documentation/msg_handler.md)). To store the data, sensors need to pass a message to MsgHandler [MsgHandler](documentation/msg_handler.md) by sending an Intent with action_sense_new_data containing the details of the datapoint. This new data will be stored and buffered in the local database before being transmitted to CommonSense at a scheduled time.

~~~
Intent sensorData = new Intent(getString(R.string.action_sense_new_data));
sensorData.putExtra(DataPoint.SENSOR_NAME, &quot;sensor name&quot;);
sensorData.putExtra(DataPoint.VALUE, &quot;foo&quot;);
sensorData.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
sensorData.putExtra(DataPoint.TIMESTAMP, System.currentTimeMillis());
startService(sensorData);
~~~

## Sensor registration {#sensor_registration}
Before a sensor can be uploaded to CommonSense it needs to be created. The SensorRegistrator class implements a trivial sensor registration method called `checkSensor` which checks in the back-end whether a sensor with a given description already exists. If the back-end successfully returns and the sensor does not exist then it will be created and the method will return True.

SensorRegistrator is implemented in nl.sense_os.service.commonsense.SensorRegistrator.

LocalStorage is implemented in nl.sense_os.service.storage.LocalStorage.

RemoteStorage is implemented in nl.sense_os.service.storage.RemoteStorage.

# Local Storage {#local_storage}

The Sense Library provides LocalStorage that will abstract the data storage mechanism. This class is implemented with the singleton pattern, so there will be only one instance that manages the local data storage. This class provides several functionalities regarding data storage, for example:
* insert new data,
* delete existing data,
* update existing data, and
* query for data

Query can be done with two different URI path schemes, as follows:
* [CONTENT_URI_PATH](@ref nl.sense_os.service.constants.SensorData.DataPoint.CONTENT_URI_PATH) scheme, will get the data from both inMemory and persisted
* [CONTENT_REMOTE_URI_PATH](@ref nl.sense_os.service.constants.SensorData.DataPoint.CONTENT_REMOTE_URI_PATH) will get the data from commonSense

In case of [CONTENT_REMOTE_URI_PATH](@ref nl.sense_os.service.constants.SensorData.DataPoint.CONTENT_REMOTE_URI_PATH), LocalStorage becomes a proxy to RemoteStorage. See nl.sense_os.service.storage.LocalStorage.query).

LocalStorage is implemented in nl.sense_os.service.storage.LocalStorage

## SQLite Database {#sqlite_database}

Local data will be stored in an encrypted SQLite database (see nl.sense_os.service.storage.SQLiteStorage and nl.sense_os.service.storage.DbHelper).

The data stored in the database will have the following fields (see nl.sense_os.service.storage.LocalStorage.DEFAULT_PROJECTION):
* ID
* SENSOR_NAME
* DISPLAY_NAME
* SENSOR_DESCRIPTION
* DATA_TYPE,
* VALUE
* TIMESTAMP
* DataPoint.DEVICE_UUID
* TRANSMIT_STATE

## In RAM & In Flash Storage {#RAM_and_Flash_storage}

Internally, LocalStorage will use two instances of SQLite to actually store the data, one in memory (RAM) and one in a file (Flash). The data in file will be persistent even when the application is killed, while the data in memory (RAM) will vanish in such cases. The data will internally be buffered in memory (RAM) first, and will be moved into file database when certain conditions are met:
* in memory database overflows when inserting data
* manually call update function with “persist=true” query parameter

The data in memory (RAM) database will be deleted when it is moved to in file database.

## Retention Rate {#retention_rate}

Old data in persistent database will be deleted after it exceeds the retention time (and has been sent to CommonSense, if applicable). The Retention Time is gathered from preferences (see nl.sense_os.service.constants.SensePrefs.Main.Advanced.RETENTION_HOURS), and has a default value of 24 hours (see nl.sense_os.service.storage.LocalStorage.DEFAULT_RETENTION_HOURS).
