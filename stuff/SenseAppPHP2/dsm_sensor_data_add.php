<?php
include_once("db_only_connect.php");
include_once("sendToDeviceServiceManager.php");
include_once("error_codes.php");

//include_once("deviceID_check.php");
$tbl_name="sensor_data"; // Table name

// Get input
$sensorValue		= $_REQUEST['sensorValue'];
$device_id		= $_REQUEST['device_id'];

if(isset($_REQUEST['sensorName']))
  $sensorName = $_REQUEST['sensorName'];
else
  $sensorName = $_REQUEST['ds_type'];

if(isset($_REQUEST['sensorDeviceType']))
  $sensorDeviceType 	= $_REQUEST['sensorDeviceType'];
else
  $sensorDeviceType	= $_REQUEST['ds_id'];

$sensorDeviceType   	= mysql_real_escape_string($sensorDeviceType);
$sensorDeviceType 	= stripslashes($sensorDeviceType);
$sensorName   		= mysql_real_escape_string($sensorName);
$sensorName	 	= stripslashes($sensorName);
$sensorValue	   	= mysql_real_escape_string($sensorValue);
$sensorValue	 	= stripslashes($sensorValue);
$device_id 	  	= mysql_real_escape_string($device_id);
$device_id	 	= stripslashes($device_id);

// find the sensor device type based on the device id and sensor id
$pos = strpos($device_id, ".");
if($pos != false)
{
    $sensorType = substr($device_id, $pos+1);
    $device_id	= substr($device_id, 0, $pos);
   if(!$sensorDeviceType)
	{
	    $sql = "select * from sensor_type where id='$sensorType'";
	    $result	= mysql_query($sql);
	    if ($result) 
	    {
	      $row = mysql_fetch_assoc($result);
	      $sensorDeviceType = $row['device_type'];
	    }
	}
}

if(!isset($device_id)) {
    $msg  = "Error: no deviceID";
    $response = array("status"=>"error", "faultcode"=>$fault_parameter, "msg"=>$msg);
    die(json_encode($response));
}

if(isset($_REQUEST['sensorDataType'])) {
    $sensorDataType = $_REQUEST['sensorDataType'];
}else{
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
    $time = $_REQUEST['sampleTime'];
} else {
    $time = microtime(true);
}

if($sensorName && $sensorValue) {
    // Check if the sensor exists
    $sql	= "SELECT * FROM sensor_type WHERE name = '$sensorName' and device_type = '$sensorDeviceType'";
    $result	= mysql_query($sql);
    $count	= mysql_num_rows($result);

    // Get sensorType
    if ($count > 0) {
        $row 		= mysql_fetch_assoc($result);
        $sensorTypeID 	= $row['id'];
    } else {
        // Create new sensor type
        $sql	= "INSERT INTO sensor_type (`id` ,`name`, `data_type`, `device_type` ) VALUES (NULL ,  '$sensorName', '$sensorDataType','$sensorDeviceType')";
        $result	= mysql_query($sql);
        if ($result) {
            // Get sensorType
            $sql		= "SELECT * FROM sensor_type WHERE name = '$sensorName' and device_type = '$sensorDeviceType'";
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
    $sql    = "SELECT `id` FROM `tags` WHERE `tagged_id`='$sensorTypeID' AND `parent_id`='$device_id'";
    $result	= mysql_query($sql);
    $count	= mysql_num_rows($result);

    // create new tag for this sensor if it doesn't exist already
    if ($count < 1) {
        
        // get type of device (to make special MyriaNed node tags)
        $sql = "SELECT `type` FROM `devices` ";
        $sql .= "WHERE `id` = '$device_id' ";
        $sql .= "LIMIT 1";
        $result = mysql_query($sql);
        $row = mysql_fetch_assoc($result);
        $deviceType = $row['type'];
        
        // get tag of parent (i.e. the device)
        $sql = "SELECT `tag` FROM `tags` ";
        $sql .= "WHERE `tagged_id` = '$device_id' AND `type` = 'devices' ";
        $sql .= "LIMIT 1";
        $result = mysql_query($sql);
        $row = mysql_fetch_assoc($result);

        // use parent tag as base
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
        $sql = "INSERT INTO `tags` ";
        $sql .= "(`id`, `tag`, `tagged_id`, `parent_id`, `type`, `date`) ";
        $sql .= "VALUES (NULL, '$tag', '$sensorTypeID', '$device_id', 'sensor_type', NOW())";
        $result = mysql_query($sql);
        if(!$result) {
            $msg  = 'Invalid query: ' . mysql_error() . "\nWhole query: " . $sql;
            $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$msg);
            die(json_encode($response));
        }
    }



    // Insert into DB
    $sql = "INSERT INTO $tbl_name (`id` ,`device_id` ,`sensor_type` ,`sensor_value` ,`date`) VALUES (NULL ,  '$device_id', '$sensorTypeID',  '$sensorValue',  '$time')";
    $result	= mysql_query($sql);
    if ($result) {
        $msg 		= "server value added";
        $response 	= array("status"=>"ok", "msg"=>$msg);
        echo json_encode($response);
		  
	// check for an external service to send data to
	$sql 	= "select * from devices where id='$device_id'";
	$result	= mysql_query($sql);
	if($result)
	{	    
	    $row 	= mysql_fetch_assoc($result);
	    $userId 	= $row['user_id'];
	    // find external service with user and sensor to send data to
	    $sql 	= "select * from external_services where user_id='$userId' and sensor_type='".$_REQUEST['ds_id']."'";
	    $result	= mysql_query($sql);	
	  
	    if($result)
	    {	
		if( mysql_num_rows($result) == 1)
		{				 
		    $row 	= mysql_fetch_assoc($result);
		    // only Gtalk service for now
		    if($row['service'] == "gtalk")
		    {		
		      $url = "http://demo.almende.com/commonSense2/dsmRPC/xmp/setGtalkStatus";
		      $params['username'] 	= $row['username'];
		      $params['password'] 	= $row['password'];
		      $params['displayTime'] 	= 10;
		      $params['status']		= $sensorValue;
		      curl_post_async($url, $params);
		    }
		}
	    }

	}
	// send to device service manager
        sendToDeviceServiceManager($device_id.".".$sensorTypeID, $sensorDataType, $sensorName, $sensorValue);
	
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

function curl_post_async($url, $params)
{
    foreach ($params as $key => &$val) {
      if (is_array($val)) $val = implode(',', $val);
        $post_params[] = $key.'='.urlencode($val);
    }
    $post_string = implode('&', $post_params);

    $parts=parse_url($url);

    $fp = fsockopen($parts['host'], 
        isset($parts['port'])?$parts['port']:80, 
        $errno, $errstr, 30);

    if($fp == FALSE)
      echo "Couldn't open a socket to ".$url." (".$errstr.")";

    $out = "POST ".$parts['path']." HTTP/1.1\r\n";
    $out.= "Host: ".$parts['host']."\r\n";
    $out.= "Content-Type: application/x-www-form-urlencoded\r\n";
    $out.= "Content-Length: ".strlen($post_string)."\r\n";
    $out.= "Connection: Close\r\n\r\n";
    if (isset($post_string)) $out.= $post_string;

    fwrite($fp, $out);
    fclose($fp);
}
?>
