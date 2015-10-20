[TOC]
# Sensor Profiles {#sensor_profiles}

## App sensors
App sensors have data that is used in the apps (Brightr or Goalie).

### mental_resilience_sensor
~~~
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "question": {
      "description": "The question text",
      "type": "string"
    },
    "score": {
      "description": "The mental resilience value (e.g. 30)",
      "type": "integer"
    },
    "question_id": {
      "description": "The identifier of the question (e.g. stylemsg_id)",
      "type": "integer"
    },
    "answer_id": {
      "description": "The identifier of the answer (e.g. 2, for a 10 point scale 0-9)",
      "type": "integer"
    },
    "answer": {
      "description": "The string representation of the answer (e.g. 30%)",
      "type": "string"
    },
    "task_type": {
      "description": "The Task.TYPE representing the domain to which this question belongs to (e.g. MR_TASK_DOMAIN_1)",
      "type": "string"
    }
  },
  "required": [
    "question",
    "score",
    "question_id",
    "answer_id",
    "answer",
    "task_type"
  ]
}
~~~

### time_zone
~~~
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "id": {
      "description": "The time-zone id, e.g. Europe/Paris",
      "type": "string"
    },
    "offset": {
      "description": "The offset from GMT in seconds",
      "type": "integer"
    }
  },
  "required": [
    "offset"
  ]
}
~~~

## Debug sensors
Debug sensors are used for debuggin the platform libraries.

### cortex_log (Android only)
~~~
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "tag": {
      "description": "Log tag",
      "type": "string"
    },
    "type": {
      "description": "One of the loggin tags (VERBOSE, DEBUG, INFO, WARNING , ERROR)",
      "type": "string"
    },
    "text": {
      "description": "The log message",
      "type": "string"
    }
  },
  "required": [
    "tag",
    "type",
    "text"
  ]
}
~~~
### geofence_sensor_visit_location
*renamed from geofence_sensor_VISIT_LOCATION*

~~~
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "accuracy": {
       "description": "The average location accuracy in meters",
      "type": "number"
    },
    "altitude": {
      "description": "The average altitude in meters above the WGS 84 reference ellipsoid.",
      "type": "integer"
    },
    "bearing": {
      "description": "The average bearing in degrees",
      "type": "integer"
    },
    "distance from fence": {
      "description": "The distance from the fence in meters",
      "type": "number"
    },
    "distance from goal": {
      "description": "The distance from the goal location in meters",
      "type": "number"
    },
    "latitude": {
      "description": "The filtered latitude in degrees",
      "type": "number"
    },
    "longitude": {
      "description": "The filtered longitude in degrees",
      "type": "number"
    },
    "out of range": {
      "description": "Whether the current location is in side or outside the fence",
      "type": "integer"
    },
    "provider": {
      "description": "The location provider, e.g. GPS, NETWORK or FUSED",
      "type": "string"
    },
    "speed": {
      "description": "The speed in meters/second over ground.",
      "type": "integer"
    }
  },
  "required": [
    "accuracy",
    "altitude",
    "bearing",
    "distance from fence",
    "distance from goal",
    "latitude",
    "longitude",
    "out of range",
    "provider",
    "speed"
  ]
}
~~~

### geofence_sensor_agoraphobia
*renamed from geofence_sensor_AGORAPHOBIA*
~~~
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
        "accuracy": {
       "description": "The average location accuracy in meters",
      "type": "number"
    },
    "altitude": {
      "description": "The average altitude in meters above the WGS 84 reference ellipsoid.",
      "type": "integer"
    },
    "bearing": {
      "description": "The average bearing in degrees",
      "type": "integer"
    },
    "distance from fence": {
      "description": "The distance from the fence in meters",
      "type": "number"
    },
    "distance from goal": {
      "description": "The distance from the goal location in meters",
      "type": "number"
    },
    "latitude": {
      "description": "The filtered latitude in degrees",
      "type": "number"
    },
    "longitude": {
      "description": "The filtered longitude in degrees",
      "type": "number"
    },
    "out of range": {
      "description": "Whether the current location is in side or outside the fence",
      "type": "integer"
    },
    "provider": {
      "description": "The location provider, e.g. GPS, NETWORK or FUSED",
      "type": "string"
    },
    "speed": {
      "description": "The speed in meters/second over ground.",
      "type": "integer"
    }
  },
  "required": [
    "accuracy",
    "altitude",
    "bearing",
    "distance from fence",
    "distance from goal",
    "latitude",
    "longitude",
    "out of range",
    "provider",
    "speed"
  ]
}
~~~

## Sensing Library sensors
Data from raw sensors is stored in the back-end to replay the data and improve the current cortex modules.

### light

~~~
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "lux": {
      "description": "The illuminance in lx",
      "type": "integer"
    }
  },
  "required": [
    "lux"
  ]
}
~~~

### noise_sensor
~~~
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "description": "The Ambient noise in decibel",
  "type": "number",
}
~~~

### wifi_scan
~~~
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "ssid": {
      "type": "string"
    },
    "bssid": {
      "type": "string"
    },
    "frequency": {
      "type": "integer"
    },
    "rssi": {
      "type": "integer"
    },
    "capabilities": {
      "type": "string"
    }
  },
  "required": [
    "bssid"  
  ]
}
~~~
### position
~~~
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "longitude": {
      "type": "number"
    },
    "latitude": {
      "type": "number"
    },
    "altitude": {
      "type": "number"
    },
    "accuracy": {
      "type": "number"
    },
    "speed": {
      "type": "number"
    },
    "bearing": {
      "type": "number"
    },
    "provider": {
      "type": "string"
    }
  },
  "required": [
    "longitude",
    "latitude",
    "accuracy",
    "provider"
  ]
}
~~~
### accelerometer
~~~
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "x-axis": {
      "type": "number"
    },
    "y-axis": {
      "type": "number"
    },
    "z-axis": {
      "type": "number"
    }
  },
  "required": [
    "x-axis",
    "y-axis",
    "z-axis"
  ]
}
~~~

### battery_sensor
~~~
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "status": {
      "type": "string"
    },
    "level": {
      "type": "integer"
    }
  },
  "required": [
    "status",
    "level"
  ]
}
### screen_activity
~~~
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "screen": {
      "type": "string"
    }
  },
  "required": [
    "screen"
  ]
}
~~~
### proximity
~~~
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "number",
}
~~~

### call_state
~~~
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "state": {
      "type": "string"
    },
    "incomingNumber": {
      "type": "string"
    }
  },
  "required": [
    "state"
  ]
}
~~~

### time_active
~~~
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "number",
}
~~~
## Deprecated sensors
Sensors that output data that will not be stored in the back-end.

Debug ouput of the coaches is stored in sensors but is not needed:

* SLEEP_DURATION
* MEAL_TIME
* MENTAL_RESILIENCE
* VISIT_LOCATION
* PHYSICAL_ACTIVITY
* AGORAPHOBIA

Sensing library Sensors:  
* linear acceleration
* app_info (doesn't work for Android)