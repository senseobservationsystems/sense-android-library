# Sensors {#sensors}

# Sense Sensor {#sense_sensor}

Almost all sensors in the Sense Library implement the SenseSensor interface. This interface provides several functions that are common for all sensors:
* start and stop sensing
* get and set sample rate

The SenseSensor interface is defined in nl.sense_os.service.shared.SenseSensor

# Base Sensor {#base_sensor}

BaseSensor is a base implementation of [SenseSensor](##SenseSensor). It inherits from nl.sense_os.service.subscription.BaseDataProducer and therefore it also implements nl.sense_os.service.subscription.DataProducer. It provides a basic implementation to set and get sample rate, and leaves the start and stop sensing mechanism to the specific sensor it inherits.

BaseSensor is implemented in nl.sense_os.service.subscription.BaseSensor

# Periodic Polling Sensor {#periodic_polling_sensor}

This is an interface that all periodic-based sensors should implement. It provides an **isActive** method for [PeriodicPollAlarmReceiver](##PeriodicPollAlarmReceiver) to check if the sensor is still active, and a **doSample** method which [PeriodicPollAlarmReceiver](##PeriodicPollAlarmReceiver) will call to do the actual sampling at a scheduled time.

The PeriodicPollingSensor interface is defined in nl.sense_os.service.shared.PeriodicPollingSensor

# Periodic Poll Alarm Receiver {#periodic_poll_alarm_receiver}

This is a generic BroadcastReceiver for periodic based sensors. Every periodic-based sensor should have an instance of this class and start it when it starts sensing. This instance will register itself to [Scheduler](##Scheduler) to be run at a specific rate based on the interval value of the specific sensor (see nl.sense_os.service.subscription.BaseSensor.sampleDelay).

It will call the **doSample** method of the sensor instance it is bound to when it is scheduled to be run.

PeriodicPollAlarmReceiver is implemented in nl.sense_os.service.shared.PeriodicPollAlarmReceiver

# Sensor groups {#sense_sensor_groups}

Sensors in Sense Library are grouped based on their usecase as follows:
* Ambience Sensors
  * nl.sense_os.service.ambience.NoiseSensor
  * nl.sense_os.service.ambience.LoudnessSensor
  * nl.sense_os.service.ambience.LightSensor
  * nl.sense_os.service.ambience.MagneticFieldSensor
  * nl.sense_os.service.ambience.PressureSensor
  * nl.sense_os.service.ambience.HumiditySensor
  * nl.sense_os.service.ambience.CameraLightSensor
  * nl.sense_os.service.ambience.TemperatureSensor
* Device Proximity Sensors
  * nl.sense_os.service.deviceprox.BluetoothDeviceProximity
  * nl.sense_os.service.deviceprox.WIFIDeviceProximity
  * nl.sense_os.service.deviceprox.NFCScan
* External Sensors
  * nl.sense_os.service.external_sensors.ZephyrBioHarness
  * nl.sense_os.service.external_sensors.ZephyrHxM
  * nl.sense_os.service.external_sensors.OBD2Sensor
* Location Sensors
  * nl.sense_os.service.location.FusedLocationSensor
  * nl.sense_os.service.location.LocationSensor
  * nl.sense_os.service.location.TimeZoneSensor
* Motion Sensors
  * nl.sense_os.service.motion.EpilepsySensor
  * nl.sense_os.service.motion.FallDetector
  * nl.sense_os.service.motion.MotionBurstSensor
  * nl.sense_os.service.motion.MotionEnergySensor
  * nl.sense_os.service.motion.StandardMotionSensor
* PhoneState Sensors
  * nl.sense_os.service.phonestate.AppsSensor
  * nl.sense_os.service.phonestate.BatterySensor
  * nl.sense_os.service.phonestate.PhoneActivitySensor
  * nl.sense_os.service.phonestate.ProximitySensor
  * nl.sense_os.service.phonestate.SensePhoneState

# Sensor Data Structure {#sensor_data_structure}

Every datapoint from a sensor should be encapsulated as nl.sense_os.service.shared.SensorDataPoint object. It contains several fields corresponding to data point fields at CommonSense: 
* sensor name
* source name
* timestamp
* data type
* value

Sensor datapoints can be one of the following types:
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

Here is an example of how to send a new Noise datapoint for a `SensorData.SensorNames.NOISE` DataProvider:

~~~java
// Notify the subscribers that a new sample is started
notifySubscribers();
// Create a SensorDataPoint with a double value
SensorDataPoint dataPoint = new SensorDataPoint(40.3d);
// Set the sensor name using the SensorNames constants
dataPoint.sensorName = SensorData.SensorNames.NOISE;
// Set the source name using the SensorNames constants
dataPoint.source = SensorData.SourceNames.SENSE_ANDROID;
// Set the epoch milliseconds time stamp using the SNTP time module
dataPoint.timeStamp = SNTP.getInstance().getTime();
// Send it to all DataConsumers that subscribed to SensorData.SensorNames.NOISE
sendToSubscribers(dataPoint);
~~~
