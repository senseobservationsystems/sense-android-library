# Sensors

## SenseSensor

Almost all of sensor in Sense Library implement SenseSensor interface. This interface provide several function that common for all sensor:
* start and stop sensing
* get and set sample rate

SenseSensor interface is defined in nl.sense_os.service.shared.SenseSensor

## BaseSensor

BaseSensor is base implementation of SenseSensor (nl.sense_os.service.shared.SenseSensor). It inherit from nl.sense_os.service.subscription.BaseDataProducer therefore it also implement nl.sense_os.service.subscription.DataProducer. It provide basic implementation to set and get sample rate, and leaving the start and stop sensing mechanism to specific sensor it inherits.

BaseSensor is implemented in nl.sense_os.service.subscription.BaseSensor

## PeriodicPollingSensor

This is an interface that all of periodic-based sensor should implement. It provide isActive method for PeriodicPollAlarmReceiver to check if the sensor is still active, and doSample method which PeriodicPollAlarmReceiver will call to do the actual sampling at scheduled time.

PeriodicPollingSensor interface is defined in nl.sense_os.service.share.PeriodicPollingSensor

## PeriodicPollAlarmReceiver

This is a generic BroadcastReceiver for periodic based sensor. Every periodic-based sensor should have an instance of this class and start it when start sensing. This instance will register itself to scheduler to be run at defined rate based from interval value  of the specific sensor (see nl.sense_os.service.subcription.BaseSensor.sampleDelay).

It will call nl.sense_os.service.subscription.BaseSensor.doSample method of the sensor instance when it scheduled to be run.

PeriodicPollAlarmReceiver is implemented in nl.sense_os.service.shared.PeriodicPollAlarmReceiver

## Controller

## Sensor groups

Sensors in Sense Library is grouped based on it’s usecase as follow :
* Ambience -> Noise, Loudness, Light, Magnetic, Pressure, Humidity, CameraLight, Temperature
* DeviceProx -> Bluetooth, NFC, Wifi
* ExternalSensors -> ZephyrBioHarness, ZephyrHxM, OBD2
* Location -> Location, TimeZone, TraveledDistance
* Motion -> Motion (accelerometer, linier accelerometer, gyroscope), MotionBurst, MotionEnergy, Epilepsy, FallDetector
* PhoneState

##Sensor Data Structure

Every data from sensor should be encapsulated as nl.sense_os.service.shared.SensorDataPoint instance. It contain several fields correspond to data point fields at CommonSense: 
* sensor name
* sensor description (correspond to sensor_type field in CommonSense)
* timestamp
* data type
* value

Sensor datapoint could be one of following type:
* Integer
* Float
* Bool
* Double
* String
* ArrayList
* JSON
* JSONString
* File
* SensorEvent

Here is an example of how to create a new datapoint :

    SensorDataPoint dataPoint = new SensorDataPoint(value);
    dataPoint.sensorName = sensorName;
    dataPoint.sensorDescription = sensor.getName();
    dataPoint.timeStamp = SNTP.getInstance().getTime();

This sensor datapoint also need to send to nl.sense_os.service.MsgHandler so it could be stored in LocalStorage and later in CommonSense. Here is an example of how to send a sensor datapoint to be stored by MsgHandler.

    Intent sensorData = new Intent(context.getString(R.string.action_sense_new_data));
    sensorData.putExtra(DataPoint.SENSOR_NAME, SensorNames.NOISE);
    sensorData.putExtra(DataPoint.VALUE, BigDecimal.valueOf(dB).setScale(2, 0).floatValue());
    sensorData.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.FLOAT);
    sensorData.putExtra(DataPoint.TIMESTAMP, startTimestamp);	sensorData.setPackage(context.getPackageName());
    context.startService(sensorData);