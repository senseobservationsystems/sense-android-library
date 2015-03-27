# Architecture Overview

Sense Android Library consist of several component as follow.

## 1. SenseService

This is main sense service class from which application can binded to. 

Activities or other component that binded to sense service can not access the SenseService directly, but through nl.sense_os.service.SenseServiceStub instance, that can be gathered from the nl.sense_os.service.SenseService.SenseBinder instance refered in onServiceConnected callback of ServiceConnection defined when binding the service (see Binding). Through this SenseServiceStub instance, application could then retrieve some functionality:
* Register user
* Login/Logout
* Toggle (start/stop) main sensing
* Toggle (start/stop) individual sensor
* Get/Set settings preferences

SenseService is implemented in nl.sense_os.service.SenseService.

More details of SenseService [here](docs/sense_service.md).


## 2. SensePlatform

This provide a high level abstraction to Sense Service. It will create and bind to a new Sense Service when created.

SenseService is implemented in nl.sense_os.platform.SensePlatform.

More details on SensePlatform [here](docs/sense_platform.md).


## 3. SenseApplication

This represents an application that using Sense Platform. All applications that using Sense should based on this class. It will create a new SensePlatform object when created, and if needed make sure the service is started when connected.

SenseApplication is implemented in nl.sense_os.platform.SenseApplication.

## 4. MsgHandler

This is a service that responsible for handling the data that has been collected by different sensors. There are two task that it could be done :
Receive sensor data from sensors and append it to buffer
Periodically transmit all sensor data in buffer to CommonSense

MsgHandler is implemented in nl.sense_os.service.MsgHandler.

More details on MsgHandler [here](docs/msg_handler.md)

## 5. Local / Remote Storage

Sensor data can be stored in and obtained from local database as well as CommonSense. The data will be stored in local database first, and transmitted to CommonSense periodically. To store the data, sensor need to pass a message to MsgHandler by sending an Intent with action_sense_new_data containing the details of datapoint. This new data will be stored and buffered in Local database before transmitted to CommonSense at scheduled time.

LocalStorage is implemented in nl.sense_os.service.storage.LocalStorage.

RemoteStorage is implemented in nl.sense_os.service.storage.RemoteStorage.

More details of Data Storage [here](docs/storage.md)

## 6. Subscription Manager

This class will manage and keeping track of available DataProducers and DataConsumers. 
Basically, DataProducer could be anything that producing data, but currently it’s only use for Sensor. While DataConsumers could be anything that willing to receive update from DataProducer everytime there is a new data.

Consumer will be added to producer’s subscriber list when a consumer subscribe to a named producer, and also when a new producer register with same name

Every DataProducer instance like sensor should be registered to SubscriptionManager.

SubscriptionManager is implemented in nl.sense_os.service.subscription.SubscriptionManager.

More details of Data Subscription [here](docs/subscription.md)
