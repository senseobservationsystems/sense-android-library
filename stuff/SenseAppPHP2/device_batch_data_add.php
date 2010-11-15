<?php
include_once("db_connect.php");
include_once("sendToDeviceServiceManager.php");
include_once("deviceID_check.php");
include_once("error_codes.php");

$tbl_name = "sensor_data"; // Table name

// get raw json string
$jsonString = $_POST['data'];

// To protect MySQL injection (more detail about MySQL injection)
//$jsonString = mysql_real_escape_string($jsonString);
$jsonString = stripslashes($jsonString);
//$jsonString = stripslashes($jsonString);
$jsonString = urldecode($jsonString);
$jsonObj = json_decode($jsonString, True);

// get array with data
$data = $jsonObj["data"];

for ($i = 0; $i < sizeOf($data); ++$i) {
    $entry = $data[$i];
    $sensorName = mysql_real_escape_string($entry['name']);
    $sensorValue = $entry['val']; // value will be escaped later, after JSON re-encoding
    $sensorDataType = mysql_real_escape_string($entry['type']);
    $time = mysql_real_escape_string($entry['time']);
    $sensorDeviceType = mysql_real_escape_string($entry['device']);

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
    $sensorValue = mysql_escape_string($sensorValue);

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
                $message  = 'Invalid query: ' . mysql_error() . '\nWhole query: ' . $sql;
                $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$message);
                die(json_encode($response));
            }
        }

        // Check if tag exists
        $sql = "SELECT `id` FROM `tags` WHERE `tagged_id`='$sensorTypeID' AND `parent_id`='$deviceId'";
        $result = mysql_query($sql);
        $count = mysql_num_rows($result);
        
        // create new tag for this sensor if it doesn't exist already
        if ($count < 1) {

            // get type of device (to make special tags for MyriaNed nodes)
            $sql = "SELECT `type` FROM `devices` ";
            $sql .= "WHERE `id` = '$deviceId' ";
            $sql .= "LIMIT 1";
            $result = mysql_query($sql);
            $row = mysql_fetch_assoc($result);
            $deviceType = $row['type'];
            
            // get tag of parent (i.e. the device)
            $sql = "SELECT `tag` FROM `tags` ";
            $sql .= "WHERE `tagged_id` = '$deviceId' AND `type` = 'devices' ";
            $sql .= "LIMIT 1";
            $result = mysql_query($sql);
            $row = mysql_fetch_assoc($result);
    
            // use parent tag as base for new tag
            $tag = $row['tag'];
            
            // include node id for myrianed nodes, otherwise just use sensor name
            if ($deviceType == 'myrianode') {
                $tag .= "#$nodeId. $sensorName";
            } else {
                $tag .= "$sensorName";
            }
            
            // include sensor device type if available, otherwise finish tag with a '/'
            if (isset($sensorDeviceType)) {
                $tag .= " ($sensorDeviceType)/";
            } else {
                $tag .= "/";
            }
            $sql        = "INSERT INTO `tags` (`id`,`tag`,`tagged_id`,`parent_id`,`type`,`date`) ";
            $sql       .= "VALUES (NULL,'$tag','$sensorTypeID','$deviceId','sensor_type',NOW())";
            $result	    = mysql_query($sql);
            if (!$result) {
                $message  = 'Invalid query: ' . mysql_error() . "\n";
                $message .= 'Whole query: ' . $sql;
                $response = array("status" => "error", "faultcode" => $fault_internal, "msg" => $message);
                die(json_encode($response));
            }
        }

        // Insert into DB
        $sql = "INSERT INTO `$tbl_name` (`id`,`device_id`,`sensor_type`,`sensor_value`,`date`) ";
        $sql .= "VALUES (NULL,'$deviceId','$sensorTypeID','$sensorValue','$time')";
        $result	= mysql_query($sql);

        if ($result) {
            // send to device service manager
            sendToDeviceServiceManager($deviceId.".".$sensorTypeID, $sensorDataType, $sensorName, $sensorValue);
        } else {
            $message  = 'Invalid query: ' . mysql_error() . "\n";
            $message .= 'Whole query: ' . $sql;
            $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$message);
            die(json_encode($response));
        }
    } else {
        $response = array("status" => "error", "faultcode" => $fault_parameter, "msg" => "name:$sensorName, value:$value");
        die(json_encode($response));
    }
}

$msg = "stored " . sizeOf($data) . " values";
$response = array("status"=>"ok", "msg" => $msg);
echo json_encode($response);


?>
