# Message Handler {#msg_handler}

This is a service that is responsible for handling the data that has been collected by different sensors. There are two tasks that it fulfills:
* Receive sensor data from sensors and append it to buffer
* Periodically transmit all sensor data in buffer to CommonSense

MsgHandler is implemented in nl.sense_os.service.MsgHandler.

# Store Data from Sensor {#store_data}

To send a sensor datapoint to MsgHandler, sensors need to send an Intent with *action_sense_new_data* that contains the details of the datapoint (see Sensor DataPoint)

Here is an example of sending a sensor datapoint to Message Handler.

~~~java
Intent sensorData = new Intent(context.getString(R.string.action_sense_new_data));
sensorData.putExtra(DataPoint.SENSOR_NAME, SensorNames.NOISE);
sensorData.putExtra(DataPoint.VALUE, BigDecimal.valueOf(dB).setScale(2, 0).floatValue());
sensorData.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
sensorData.putExtra(DataPoint.TIMESTAMP, startTimestamp);
sensorData.setPackage(context.getPackageName());
context.startService(sensorData);
~~~

# Transmit Sensor Data to CommonSense {#transmit_data}

SenseService uses an nl.sense_os.service.ctrl.Controller instance to schedule sending of data in LocalStorage to CommonSense periodically. This Controller will create an instance of nl.sense_os.service.scheduler.DataTransmitter that will register itself to scheduler to run at a particular interval based on the nl.sense_os.service.constants.SensePrefs.Main.SyncRate setting in preferences. This DataTransmitter then will send an *action_sense_send_data* intent to nl.sense_os.service.MsgHandler when scheduled to run, to actually send the sensor data to CommonSense and empty its buffer.

These options are available for SyncRate settings:
* nl.sense_os.service.SensePrefs.Main.SyncRate.ECO_MODE (30 minute)
* nl.sense_os.service.SensePrefs.Main.SyncRate.RARELY (15 minute)
* nl.sense_os.service.SensePrefs.Main.SyncRate.NORMAL (5 minute)
* nl.sense_os.service.SensePrefs.Main.SyncRate.OFTEN (1 minute)
* nl.sense_os.service.SensePrefs.Main.SyncRate.REAL_TIME (depend on sample rate)


