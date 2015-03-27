# Data Storage

Sensor data can be stored in and obtained from local database as well as CommonSense. The data will be stored in local database first, and transmitted to CommonSense periodically (see [MsgHandler](documentation/msg_handler.md)). 

To store the data, sensor need to pass a message to MsgHandler by sending an Intent with action_sense_new_data containing the details of datapoint. This new data will be stored and buffered in Local database before transmitted to CommonSense at scheduled time.

    Intent sensorData = new Intent(getString(R.string.action_sense_new_data));
    sensorData.putExtra(DataPoint.SENSOR_NAME, &quot;sensor name&quot;);
    sensorData.putExtra(DataPoint.VALUE, &quot;foo&quot;);
    sensorData.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
    sensorData.putExtra(DataPoint.TIMESTAMP, System.currentTimeMillis());
    startService(sensorData);


## 1. LocalStorage 

Sense library provide LocalStorage that will abstract the data storage mechanism. This class is implemented with singleton pattern, so there will be only one instance that managing the local data storage. This class provide several functionality regards to data storage, for example :
* insert new data,
* delete existing data,
* update existing data, and
* query for data

Query can be done with two URI path scheme, as follow :
* [CONTENT_URI_PATH](@ref nl.sense_os.service.constants.SensorData.DataPoint.CONTENT_URI_PATH) scheme, will get the data from both inMemory and persisted
* [CONTENT_REMOTE_URI_PATH](@ref nl.sense_os.service.constants.SensorData.DataPoint.CONTENT_REMOTE_URI_PATH) will get the data from commonSense

In case of [CONTENT_REMOTE_URI_PATH](@ref nl.sense_os.service.constants.SensorData.DataPoint.CONTENT_REMOTE_URI_PATH), LocalStorage become a proxy to RemoteStorage. See nl.sense_os.service.storage.LocalStorage.query).

### SQLite

Local data will be stored in encrypted SQLite database (see nl.sense_os.service.storage.SQLiteStorage and nl.sense_os.service.storage.DbHelper).

The data stored in database will have the following field (see nl.sense_os.service.storage.LocalStorage.DEFAULT_PROJECTION):
* ID
* SENSOR_NAME
* DISPLAY_NAME
* SENSOR_DESCRIPTION
* DATA_TYPE,
* VALUE
* TIMESTAMP
* DataPoint.DEVICE_UUID
* TRANSMIT_STATE

### In RAM & In Flash Storage

Internally, LocalStorage will use two instance of SQLite to actually store the data, one in memory (RAM) and one more in a file (Flash). The data in file will be persistent even when application killed, while the data in memory (RAM) will be vanish in such case. The data will internally buffered in memory (RAM) first, and will be moved into file database at certain condition :
* in memory database become overflow when inserting data
* manually call update function with “persist=true” query parameter

The data in memory (RAM) database will be deleted when data moved to in file database.

### Retention Rate

Old data in persistent database will be deleted after exceed retention time (and not sended yet in case using commonSense). Retention times is gathered from preferences, or 24 hour by default.


## 2. Remote Storage



