# Architecture Overview

Sense Android Library consists of several components as follows.

## 1. SenseService

This is the main SenseService class to which application can bind. 

Activities or other components that bind to SenseService can not access the SenseService directly, but must do so through an nl.sense_os.service.SenseServiceStub instance, that can be gathered from the nl.sense_os.service.SenseService.SenseBinder instance referred in onServiceConnected callback of ServiceConnection defined when binding the service (see Binding). Through this SenseServiceStub instance, the application can then retrieve some functionality:
* Register user
* Login/Logout
* Toggle (start/stop) main sensing
* Toggle (start/stop) individual sensor
* Get/Set settings preferences

SenseService is implemented in nl.sense_os.service.SenseService.

More details on SenseService [here](documentation/sense_service.md).


## 2. SensePlatform

The SensePlatform class provides an abstraction to simplify the interaction with the SenseService, user management and sensor data access. It's a proxy class which by instantiating binds (and starts if needed) the SenseService. You can then use the high level methods of this class, and/or get the service object to work directly with the SenseService.

SensePlatform is implemented in nl.sense_os.platform.SensePlatform.

More details on SensePlatform [here](documentation/sense_platform.md).


## 3. SenseApplication

This represents an application that uses SensePlatform. All applications that use SensePlatform should be based on this class. It will create a new SensePlatform object when created, and if needed make sure the service is started when connected.

SenseApplication is implemented in nl.sense_os.platform.SenseApplication.

## 4. Data Storage Engine

The Data Storage Engine is a stand-alone module for local and remote sensor data storage. It allows the storage of sensor data with a predefined structure from predefined sensors and sources, which are described in the [sensor profiles](https://github.com/senseobservationsystems/aim/blob/master/docs/sensor_profiles.md).

More details on the Data Storage Engine [here](documentation/storage.md)

## 5. Subscription Manager

This class will manage and keep track of available DataProducers and DataConsumers. 
Basically, a DataProducer could be anything that produces data, but currently it’s only used for sensors. A DataConsumer could be anything that wishes to receive updates from a DataProducer every time there is a new data.

A DataConsumer will be added to DataProducer’s subscriber list when the consumer subscribes to a named producer, and also when a new producer registers with that same name.

Every DataProducer instance, like sensors, should be registered to SubscriptionManager.

SubscriptionManager is implemented in nl.sense_os.service.subscription.SubscriptionManager.

More details on SubscriptionManager [here](documentation/subscription.md)
