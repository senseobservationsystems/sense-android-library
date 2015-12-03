# Data Subscription {#data_subscription}

# Subscription Manager {#subscription_manager}

Sense Library provides SubscriptionManager to keep all of the available producers and subscribers as well as relations between them. Every sensor in sense library should be registered to this SubscriptionManager instance.

This class, like most of other class in Sense Library, is implemented with singleton pattern. It's implemented in nl.sense_os.service.subscription.SubscriptionManager.

A [DataProducer](##DataProducer) can register to [SubscriptionManager](##SubscribtionManager) instance with a given name. One name in SubscriptionManager could be registered to one or more different DataProducer instances. Registering an existing instance of DataProducer will result in doing nothing.

A [DataConsumer](##DataConsumer) can subscribe to one or more [DataProducer](##DataProducer) registered under a certain name. If there is no DataProducer for this name yet, SubscriptionManager will still store it as a subscriber and subscribe it when a new DataProducer is registered with that name.

# Data Producer {#data_producer}

DataProducer is an interface that specifies some functionality for objects that produce data, specifically [SensorDataPoint](##SensorDataPoint) objects, so that one or more [DataConsumer](##DataConsumer) can subscribe to it. Every sensor in Sense Library is an implementation of DataProducer. 

Sense library provides a base implementation of this interface in nl.sense_os.service.subscription.BaseDataProducer abstract class, and almost all of sensors in Sense library are based on this class. BaseDataProducer will store all of its subcribers in an ArrayList. This class also provides a mechanisme to notify all of its subscribers when there are new samples, and also send the SensorDataPoint to some or all of its subscribers when the sampling is finished. BaseDataProducer object will check the status of its DataConsumers to see if they have received all the data they need before sending the data.

DataProducer can register to SubscriptionManager so a DataConsumer can subscribe to it. Here is an example of how to register a producer to SubscriptionManager

~~~java
mSubscrMgr = SubscriptionManager.getInstance();
testSensor = TestSensor.getInstance(ctx);
mSubscrMgr.registerProducer(“TestSensor”, testSensor);
~~~

BaseDataProducer objects like sensors should also notify and send the datapoint everytime there is a new sample. Here is an example of how to notify and send sensor data to subscribers when there is a new sample.

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

# Data Consumer {#data_consumer}

DataConsumer is an interface that specifies some functionality for objects to subscribe to one or more [DataProducer](##DataProducer). By subscribing to a DataProducer, a DataConsumer object will get a notification every time there is a new datapoint, as well as get the actual data. Any class that implements this interface also needs to implement a function that enables the DataProducer to check if the DataConsumer object has all of the data it needs before sending the actual data.

FallDetector and EpilepsySensor are examples of DataConsumer.

DataConsumer can subscribe to a DataProducer directly or through SubscriptionManager. Here is an example on how to subscribe a DataConsumer to a data producer directly

~~~
testConsumerSensor = TestConsumerSensor.getInstance();
dataProducer.addSubscriber(testConsumerSensor);
~~~

and here is an example of how to subscribe a DataConsumer to DataProducer in previous example (see DataProducer) using SubscriptionManager.

~~~java
testConsumerSensor = TestConsumerSensor.getInstance();

mSubscrMgr = SubscriptionManager.getInstance();
mSubscrMgr.subscribeConsumer("TestSensor", testConsumerSensor);
~~~
