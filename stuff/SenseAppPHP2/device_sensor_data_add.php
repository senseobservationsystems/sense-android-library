<?php
include_once("db_connect.php");
include_once("sendToDeviceServiceManager.php");
include_once("deviceID_check.php");
include_once("error_codes.php");

$tbl_name = "sensor_data"; // Table name

// Get input
$sensorName	    	= $_REQUEST['sensorName'];
$sensorValue		= $_REQUEST['sensorValue'];
$sensorDataType 	= $_REQUEST['sensorDataType'];
$sensorDeviceType   = $_REQUEST['sensorDeviceType'];

if(!isset($_REQUEST['sensorDataType'])) {
    if (is_numeric($sensorValue)) {
        $sensorDataType = 'float';
    } else if (substr($sensorValue,0,1) == "{") {
        $sensorDataType = 'json';
    } else if (is_bool($sensorValue)) {
        $sensorDataType = 'bool';
    } else if (is_string($sensorValue)) {
        $sensorDataType = 'string';
    }
}

if(isset($_REQUEST['sampleTime'])) {
    $time     = $_REQUEST['sampleTime'];
} else {
    $time = microtime(true);
}

if($sensorName && $sensorValue)
{
    // To protect MySQL injection (more detail about MySQL injection)
//     $sensorName         = mysql_real_escape_string($sensorName);
//     $sensorDataType     = mysql_real_escape_string($sensorDataType);
//     $sensorValue        = mysql_real_escape_string($sensorValue);
//     $sensorDeviceType   = mysql_real_escape_string($sensorDeviceType);
// 
//     $sensorName 		= stripslashes($sensorName);
//     $sensorValue 		= stripslashes($sensorValue);
//     $sensorDataType 	= stripslashes($sensorDataType);
//     $sensorDeviceType	= stripslashes($sensorDeviceType);

    // Check if the sensor exists
    $sql	= "SELECT * FROM `sensor_type` WHERE `name`='$sensorName' AND `device_type`='$sensorDeviceType'";
    $result	= mysql_query($sql);
    $count	= mysql_num_rows($result);

    // Get sensorType
    if ($count == 1) {
        $row 		    = mysql_fetch_assoc($result);
        $sensorTypeID 	= $row['id'];
    } else {
        // Create new sensor type
        $sql	= "INSERT INTO sensor_type (`id` ,`name`, `data_type`, `device_type` ) VALUES (NULL ,  '$sensorName', '$sensorDataType','$sensorDeviceType')";
        $result	= mysql_query($sql);
        if ($result) {
            // Get sensorType
            $sql		= "SELECT * FROM `sensor_type` WHERE `name`='$sensorName' AND `device_type`='$sensorDeviceType'";
            $result		= mysql_query($sql);
            $row 		= mysql_fetch_assoc($result);
            $sensorTypeID 	= $row['id'];
        } else {
            $msg  = 'Invalid query: ' . mysql_error() . "\n";
            $msg .= 'Whole query: ' . $sql;
            $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$msg);
            die(json_encode($response));
        }
    }

    // Check if tag exists
    $sql    = "SELECT `id` FROM `tags` WHERE `tagged_id`='$sensorTypeID' AND `parent_id`='$deviceId'";
    $result	= mysql_query($sql);
    $count	= mysql_num_rows($result);
    if ($count < 1) {

        $sql        = "SELECT `type` FROM `devices` WHERE `id` = '$deviceId' LIMIT 1";
        $result		= mysql_query($sql);
        $row 		= mysql_fetch_assoc($result);
        $deviceType = $row['type'];

        // insert new tag with user id, device type and sensor type
        $sql        = "INSERT INTO `tags` (`id`, `tag`, `tagged_id`, `parent_id`, `type`, `date`) ";
        if ($deviceType == 'smartPhone') {
            $tag    = "/$userId/$deviceType #$deviceId/$sensorName/";
        } else if ($deviceType == 'myrianode') {
            $nodeId = $_REQUEST['uuid'];
            if (isset($sensorDeviceType)) {
                $tag = "/$userId/MyriaNed node $nodeId/#$nodeId. $sensorName ($sensorDeviceType)/";
            } else {
                $tag = "/$userId/MyriaNed node $nodeId/#$nodeId. $sensorName/";
            }
        } else {
            // legacy, for older device types
            $tag    = "/$userId/$deviceType/$sensorName/";
        }
        $sql       .= "VALUES (NULL, '$tag', '$sensorTypeID', '$deviceId', 'sensor_type', NOW())";
        $result	    = mysql_query($sql);
        if(!$result) {
            $msg  = 'Invalid query: ' . mysql_error() . "\n";
            $msg .= 'Whole query: ' . $sql;
            $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$msg);
            die(json_encode($response));
        }
    }

    // Insert into DB
    $sql	= "INSERT INTO $tbl_name (`id` ,`device_id` ,`sensor_type` ,`sensor_value` ,`date`) VALUES (NULL ,  '$deviceId', '$sensorTypeID',  '$sensorValue',  '$time')";
    $result	= mysql_query($sql);
    if ($result) {
        $msg = "sensor value added";
        $response = array("status"=>"ok", "msg"=>$msg);
        echo json_encode($response);

        // send to device service manager
        sendToDeviceServiceManager(($deviceId.".".$sensorTypeID), $sensorDataType, $sensorName, $sensorValue);
    } else {
        $msg  = 'Invalid query: ' . mysql_error() . "\n";
        $msg .= 'Whole query: ' . $sql;
        $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$msg);
        die(json_encode($response));
    }
} else {
    $msg = "Error: no sensorName or sensorValue given";
    $response = array("status"=>"error", "faultcode"=>$fault_parameter, "msg"=>$msg);
    die(json_encode($response));
}

?>
