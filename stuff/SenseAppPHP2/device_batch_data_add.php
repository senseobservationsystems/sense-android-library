<?php
include_once("db_connect.php");
include_once("sendToDeviceServiceManager.php");
include_once("deviceID_check.php");

$tbl_name = "sensor_data"; // Table name
$fault_sql_error = 3;
$fault_missing_data = 2;

// get raw json string
$jsonString = $_REQUEST['data'];

// To protect MySQL injection (more detail about MySQL injection)
//$jsonString = mysql_real_escape_string($jsonString);
$jsonString = stripslashes($jsonString);
//$jsonString = stripslashes($jsonString);

$jsonObj = json_decode($jsonString, True);

// get array with data
$data = $jsonObj["data"];

for ($i = 0; $i < sizeOf($data); ++$i) {
    $entry = $data[$i];
    $sensorName = $entry['name'];
    $sensorValue = $entry['val'];
    $sensorDataType = $entry['type'];
    $time = $entry['time'];
    $sensorDeviceType = $entry['device'];

    // guess data type if it is not given
    if (!isset($entry['type'])) {
        if (is_numeric($sensorValue)) {
            $sensorDataType = 'float';
        } else if (is_string($sensorValue)) {
            $sensorDataType = 'string';
        } else if (is_bool($sensorValue)) {
            $sensorDataType = 'bool';
        } else {
            $sensorDataType = 'json';
        }
    }

    // re-encode JSON sensor data
    if ($sensorDataType == "json") {
        $sensorValue = json_encode($sensorValue);
    }

    // change boolean values to String because "False" doesn't print
    if ($sensorDataType === "bool") {
        if ($sensorValue) {
            $sensorValue = "true";
        } else {
            $sensorValue = "false";
        }
    }

    if (isset($sensorName) && isset($sensorValue)) {

        // Check if the sensor exists
        $sql = "SELECT * FROM `sensor_type` WHERE `name`='$sensorName' and `device_type`='$sensorDeviceType'";
        $result = mysql_query($sql);
        $count = mysql_num_rows($result);

        // Get sensorType
        if($count == 1) {
            $row 		    = mysql_fetch_assoc($result);
            $sensorTypeID 	= $row['id'];
        } else {
            // Create new sensor type
            $sql = "INSERT INTO `sensor_type` (`id`,`name`,`data_type`,`device_type`) VALUES (NULL,'$sensorName','$sensorDataType','$sensorDeviceType')";
            $result	= mysql_query($sql);
            if ($result) {
                // Get sensorType
                $sql		= "SELECT * FROM `sensor_type` WHERE `name`='$sensorName' and `device_type`='$sensorDeviceType'";
                $result		= mysql_query($sql);
                $row 		= mysql_fetch_assoc($result);
                $sensorTypeID = $row['id'];
            } else {
                $message  = 'Invalid query: ' . mysql_error() . "\n";
                $message .= 'Whole query: ' . $query;
                $response = array("status"=>"error", "faultcode"=>$fault_sql_error, "msg"=>$message);
                die(json_encode($response));
            }
        }

        // Check if tag exists
        $sql = "SELECT `id` FROM `tags` WHERE `tagged_id`='$sensorTypeID' AND `parent_id`='$deviceId'";
        $result = mysql_query($sql);
        $count = mysql_num_rows($result);
        if ($count < 1) {

            $sql        = "SELECT `type` FROM `devices` WHERE `id`='$deviceId' LIMIT 1";
            $result		= mysql_query($sql);
            $row 		= mysql_fetch_assoc($result);
            $deviceType = $row['type'];

            // insert new tag with user id, device type and sensor type            
            if ($deviceType == 'smartPhone') {
                $tag    = "/$userId/$deviceType #$deviceId/$sensorName/";
            } else if ($deviceType == 'myrianode') {
                $nodeId = $_REQUEST['uuid'];
                $tag = "/$userId/MyriaNed node $nodeId/#$nodeId. $sensorName/";
            } else {
                // legacy, for older device types
                $tag    = "/$userId/$deviceType/$sensorName/";
            }
            $sql        = "INSERT INTO `tags` (`id`,`tag`,`tagged_id`,`parent_id`,`type`,`date`) ";
            $sql       .= "VALUES (NULL,'$tag','$sensorTypeID','$deviceId','sensor_type',NOW())";
            $result	    = mysql_query($sql);
            if (!$result) {
                $message  = 'Invalid query: ' . mysql_error() . "\n";
                $message .= 'Whole query: ' . $query;
                $response = array("status" => "error", "faultcode" => $fault_sql_error, "msg" => $message);
                die(json_encode($response));
            }
        }

        // Insert into DB
        $sql = "INSERT INTO `$tbl_name` (`id`,`device_id`,`sensor_type`,`sensor_value`,`date`) ";
        $sql .= "VALUES (NULL,'$deviceId','$sensorTypeID','$sensorValue','$time')";
        $result	= mysql_query($sql);

        if ($result) {
            // send to device service manager
            sendToDeviceServiceManager($deviceId, $sensorDataType, $sensorName, $sensorValue);
        } else {
            $message  = 'Invalid query: ' . mysql_error() . "\n";
            $message .= 'Whole query: ' . $query;
            $response = array("status"=>"error", "faultcode"=>$fault_sql_error, "msg"=>$message);
            die(json_encode($response));
        }
    } else {
        $response = array("status" => "error", "faultcode" => $fault_missing_data, "msg" => "name:$sensorName, value:$value");
        die(json_encode($response));
    }
}

$msg = "stored " . sizeOf($data) . " values";
$response = array("status"=>"ok", "msg" => $msg);
echo json_encode($response);


?>
