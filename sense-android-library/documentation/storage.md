# Data Storage

Sensor data can be stored in and obtained from local database as well as CommonSense. The data will be stored in local database first, and transmitted to CommonSense periodically (see MsgHandler).

Initially the data is stored in memory,  In case the memory becomes too full, the data is offloaded into a persistent database in the flash memory.  This process is hidden to the end user, so you do not have to worry about which data is where. But it’s still possible to manually persist the current data in inMemory database by calling update on LocalStorage instance with “persist” query parameter set to “true”.

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

Particular for query functionality, LocalStorage instance could also behave as a proxy to query data from commonSense (see nl.sense_os.service.storage.LocalStorage.query).

Internally, LocalStorage will use two instance of SQLite to actually store the data, one in memory and one more in a file. The data in file will be persistent even when application killed, while the data in memory will be vanish in such case. The data will internally buffered in memory first, and will be moved into file database at certain condition :
in memory database become overflow when inserting data
call update function with “persist=true” query parameter

the data in memory database will be deleted when data moved to in file database.

Sense data will be stored locally as encrypted SQLite database with such field at minimal:
* ID
* SENSOR_NAME
* DISPLAY_NAME
* SENSOR_DESCRIPTION
* DATA_TYPE,
* VALUE
* TIMESTAMP
* DataPoint.DEVICE_UUID
* TRANSMIT_STATE

Query with LOCAL_VALUES_URI will get the data from both inMemory and persisted, while query from REMOTE_VALUES_URI will get the data from commonSense.

### Retention

Old data in persistent database will be deleted after exceed retention time (and not sended yet in case using commonSense). Retention times is gathered from preferences, or 24 hour by default.


## 2. Remote Storage



