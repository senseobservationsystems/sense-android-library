# Data Subscription

## Subscription manager

Sense Library provide SubscriptionManager to keep all of the available producer as well as its subscriber. This class, like most of other class in Sense Library, is implemented with singleton pattern. Every sensor in sense library should registered to this subscriptionManager instance.

A DataProducer could register to subscriptionManager identified with a given name. One name in SubscriptionManager could consist of one or more different instance DataProducer. Registering an existing instance of DataProducer will do nothing.

A DataConsumer could subscribe to one or more DataProducer registered as certain name. If there is no DataProducer for this name yet, SubscriptionManager will still store it as subscriber and subscribe it when a new DataProducer is registered with those name.

## DataProducer

DataProducer is an interface that specify some functionality for object that producing data, specifically SensorDataPoint object, so that one or more DataConsumer could subscribe to it. Every sensor in Sense Library is an implementation of DataProducer. 

Sense library provide a base implementation of this interface in BaseDataProducer abstract class, and almost all of sensors in Sense library are based from this class. BaseDataProducer will store all of its subcriber in an ArrayList. This class also provide a mechanisme to notify all of it subscriber when there are new samples, and also send the SensorDataPoint to some or all of its subscribers when the sampling finished. BaseDataProducer object will check the status of it DataConsumer if they have get all the data they need before sending the data.

DataProducer could register to SubscriptionManager so a DataConsumer could subscribe to it. Here is an example of how to register a producer to SubscriptionManager

    mSubscrMgr = SubscriptionManager.getInstance();
    testSensor = TestSensor.getInstance(ctx);
    mSubscrMgr.registerProducer(“TestSensor”, testSensor);

BaseDataProducer object like sensor should also notify and send the datapoint everytime there is a new sample. Here is an example of how to notify and send sensor data to subscriber when there is a new sample.

    notifySubscribers();
    SensorDataPoint dataPoint = new SensorDataPoint(value);
    dataPoint.sensorName = "Sensor name";
    dataPoint.sensorDescription = "Sensor description";
    dataPoint.timeStamp = startTimestamp;
    sendToSubscribers(dataPoint);


## DataConsumer

DataConsumer is an interface that specify some functionality for object to subscribe to one or more DataProducer. By subscribing to a DataProducer, a DataConsumer object will get notification everytime there is a new data, as well as get the actual data. Class that implement this interface also need to implement a function that enabling the DataProducer to check if the DataConsumer object has all of the data it need before sending the actual data.

FallDetector and EpilepsySensor is an example of DataConsumer.

DataConsumer could subscribe to a DataProducer directly or through Subscription Manager. Here is an example on how to subscribe a DataConsumer to a data producer directly

    testConsumerSensor = TestConsumerSensor.getInstance();
    dataProducer.addSubscriber(testConsumerSensor);

and here is an example of how to subscribe a DataConsumer to DataProducer in previous example (see DataProducer) using SubscriptionManager

    testConsumerSensor = TestConsumerSensor.getInstance();

    mSubscrMgr = SubscriptionManager.getInstance();
    mSubscrMgr.subscribeConsumer("TestSensor", testConsumerSensor);
