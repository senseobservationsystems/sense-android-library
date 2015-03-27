# Message Handler

MsgHandler is a service that responsible for handling the data that has been collected by different sensors. There are two task that it could done :
* Receive sensor data from sensors and store it in [LocalStorage](docs/storage.md)
* Periodically transmit all sensor data in LocalStorage to CommonSense

MsgHandler is implemented in nl.sense_os.service.MsgHandler.

## Store datapoint from Sensor

To send a sensor data to MsgHandler, sensors need to send an Intent with *action_sense_new_data* that contain the details of the datapoint (see Sensor DataPoint)

Here is an example of sending a sensor datapoint to Message Handler.

    Intent sensorData = new Intent(context.getString(R.string.action_sense_new_data));
    sensorData.putExtra(DataPoint.SENSOR_NAME, SensorNames.NOISE);
    sensorData.putExtra(DataPoint.VALUE, BigDecimal.valueOf(dB).setScale(2, 0).floatValue());
    sensorData.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
    sensorData.putExtra(DataPoint.TIMESTAMP, startTimestamp);
    sensorData.setPackage(context.getPackageName());
    context.startService(sensorData);


## Transmit sensor data to CommonSense

Sense Service using nl.sense_os.service.ctrl.Controller instance to schedule sending of data in LocalStorage to CommonSense periodically. This Controller will create an instance of nl.sense_os.service.DataTransmitter that will registering itself to scheduler to run at particular interval based on nl.sense_os.service.constants.SensePrefs.Main.SyncRate settings in preferences. This DataTransmitter then will send an *action_sense_send_data* intent to nl.sense_os.service.MsgHandler when scheduled to run, to actually send the sensor data to CommonSense and empty it’s buffer.

This is available option for SyncRate settings:
* nl.sense_os.service.SensePrefs.Main.SyncRate.RARELY (15 minute)
* nl.sense_os.service.SensePrefs.Main.SyncRate.ECO_MODE (30 minute)
* nl.sense_os.service.SensePrefs.Main.SyncRate.NORMAL (5 minute)
* nl.sense_os.service.SensePrefs.Main.SyncRate.OFTEN (1 minute)
* nl.sense_os.service.SensePrefs.Main.SyncRate.REAL_TIME (depend on sample rate)


