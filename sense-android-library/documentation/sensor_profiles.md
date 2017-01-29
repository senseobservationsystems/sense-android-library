# Sensor Profiles

# App sensors {#app_sensors}
The App sensors have data that is used in the apps (Brightr or Goalie).

## mental_resilience {#mental_resilience}

*renamed from mental_resilience_sensor*
~~~json
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

## time_zone {#time_zone}

~~~json
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

# Sensing Library sensors {#sensing_library_sensors}
These sensors contain data which is stored in the back-end to improve the current cortex modules.

## accelerometer {#accelerometer}

~~~json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "x-axis": {
      "description": "The acceleration force applied on the x-axis in m/s2",
      "type": "number"
    },
    "y-axis": {
      "description": "The acceleration force applied on the y-axis in m/s2",
      "type": "number"
    },
    "z-axis": {
      "description": "The acceleration force applied on the z-axis in m/s2",
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

## battery {#battery}

*renamed from battery_sensor*
~~~json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "status": {
      "description": "The status of the battery, e.g. charging, discharging, full",
      "type": "string"
    },
    "level": {
      "description": "The battery level in percentage",
      "type": "number"
    }
  },
  "required": [
    "level"
  ]
}
~~~
## call {#call}

*renamed from call_state*
~~~json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "state": {
      "description": "The state of the phone",
      "enum": ["idle", "dialing", "ringing", "calling"]
    },
    "incomingNumber": {
      "description": "The phone number of the in-comming call",
      "type": "string"
    },
    "outgoingNumber": {
      "description": "The phone number of the out-going call",
      "type": "string"
    }
  },
  "required": [
    "state"
  ]
}
~~~
## light {#light}

~~~json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "description": "The illuminance in lux",
  "type": "number"
}
~~~

*removed inner object with key lux*

## noise {#noise}

*renamed from noise_sensor*
~~~json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "description": "The Ambient noise in decibel",
  "type": "number",
}
~~~

## position {#position}

~~~json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "longitude": {
      "description": "The longitude in degrees",
      "type": "number"
    },
    "latitude": {
      "description": "The latitude in degrees",
      "type": "number"
    },
    "altitude": {
      "description": "altitude in meters above the WGS 84 reference ellipsoid.",
      "type": "number"
    },
    "accuracy": {
      "description": "accuracy in meters",
      "type": "number"
    },
    "speed": {
      "description": "The speed in meters/second over ground.",
      "type": "number"
    },
    "bearing": {
      "description": "The average bearing in degrees",
      "type": "number"
    },
    "provider": {
      "description": "The location provider, e.g. GPS, NETWORK or FUSED",
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

## proximity {#proximity}

~~~json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "description": "The proximity to an object in cm",
  "type": "number",
}
~~~

## screen {#screen}

*renamed from screen_activity*
~~~json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "description": "The status of the screen, e.g. on or off",
  "enum": [ "on", "off"]
}
~~~
*removed inner object with the key-name: screen*

## wifi_scan {#wifi_scan}

~~~json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "ssid": {
      "description": "The name of the detected wifi device",
      "type": "string"
    },
    "bssid": {
      "description": "The mac address of the detected wifi device",
      "type": "string"
    },
    "frequency": {
      "description": "The signal frequency of the detected wifi device",
      "type": "integer"
    },
    "rssi": {
      "description": "The signal strength of the detected wifi device",
      "type": "integer"
    },
    "capabilities": {
      "description": "The capabilities of the detected wifi device, e.g. encryption, WPS",
      "type": "string"
    }
  },
  "required": [
    "bssid"  
  ]
}
~~~


# Debug sensors {#debug_sensors}
Debug sensors are used for debuggin the platform libraries.

## app_info {#app_info}

~~~json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "sense_library_version": {
      "description": "The version of the sense library",
      "type": "string"
    },
    "app_build": {
      "description": "Application build (version code, e.g. 1413357349)",
      "type": "string"
    },
    "app_name": {
      "description": "Application name (e.g. Goalie)",
      "type": "string"
    },
    "app_version": {
      "description": "Application version (version name, e.g. 3.0.0)",
      "type": "string"
    },
    "locale": {
      "description": "The device region settings (e.g. en_GB)",
      "type": "string"
    },
    "os": {
      "description": "OS (e.g Android or iOS)",
      "type": "string"
    },
    "os_version": {
      "description": "OS Version (e.g. 4.2.2)",
      "type": "string"
    },
    "device_model": {
      "description": "Device model (e.g. Galaxy Nexus)",
      "type": "string"
    },
  "required": [
    "sense_library_version",
    "app_name",
    "app_build"
  ]
}
~~~
*The property key sense_platform_version (iOS only) is changed to sense_library_version*

## cortex_log {#cortex_log}

~~~json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "tag": {
      "description": "Log tag",
      "type": "string"
    },
    "type": {
      "description": "The type of log information e.g. WARNING",
      "enum": ["VERBOSE", "DEBUG", "INFO", "WARNING" , "ERROR"]
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

## time_active {#time_active}

~~~json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "description": "The time physically active in seconds",
  "type": "number",
}
~~~


## sleep {#sleep}

*renamed from sleep_time*
~~~json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "end_date": {
      "description" : "The end time of the sleep period in epoch seconds",
      "type": "number"
    },
    "metadata": {
      "type": "object",
      "properties": {
        "core version": {
          "description" : "The version of the cortex core",
          "type": "string"
        },
        "module version": {
          "description" : "The version of the sleep_time module",
          "type": "string"
        },
        "status": {
          "description" : "The current status of the sleep_time module for debugging module, e.g. awake: too much noise",
          "type": "string"
        }
      }
    },
    "hours": {
      "description" : "The number of actual sleep hours",
      "type": "number"
    },
    "start_date": {
      "description" : "The start time of the sleep period in epoch seconds",
      "type": "number"
    }
  },
  "required": [
    "end_time",
    "hours",
    "start_time"
  ]
}
~~~
*changed the sleepTime key to hours, end_date and start_date to end_time and start_time resp.*<br>
*changed the type of the property history_based from integer to boolean*

## sleep_estimate {#sleep_estimate}

*renamed from sleep_time_estimate*
~~~json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "end_time": {
      "description" : "The end time of the sleep period in epoch seconds",
      "type": "number"
    },
    "history_based": {
      "description" : "Whether the sleep value is based on the history or on the actual sleep period",
      "type": "boolean"
    },
    "metadata": {
      "type": "object",
      "properties": {
        "core version": {
          "description" : "The version of the cortex core",
          "type": "string"
        },
        "module version": {
          "description" : "The version of the sleep_time module",
          "type": "string"
        },
        "status": {
          "description" : "The current status of the sleep_time module for debugging module, e.g. awake: too much noise",
          "type": "string"
        }
      }
    },
    "hours": {
      "description" : "The number of actual sleep hours",
      "type": "number"
    },
    "start_time": {
      "description" : "The start time of the sleep period in epoch seconds",
      "type": "number"
    }
  },
  "required": [
    "end_time",
    "hours",
    "start_time"
  ]
}
~~~
*changed the property sleepTime to hours, end_date and start_date to end_time and start_time resp.*<br>
*changed the type of the property history_based from integer to boolean*

# Deprecated sensors {#deprecated_sensors}
Sensors that output data that will not be stored in the back-end.

Debug ouput of the coaches is stored in sensors but is not needed:

ANDROID / iOS
* SLEEP_DURATION / sleep_duration_coach
* MEAL_TIME / meal_time_coach
* MENTAL_RESILIENCE
* VISIT_LOCATION
* PHYSICAL_ACTIVITY / exercise_coach
* AGORAPHOBIA

Sense library sensors which do not need data in the back-end:
* linear acceleration
* linear acceleration (burst-mode)
* accelerometer (burst-mode)
* motion energy
* motion features
* connection type
* geofence_sensor_VISIT_LOCATION