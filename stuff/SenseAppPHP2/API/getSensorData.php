<?php
// Checks for a valid login session
// Does a relogin when a new uuid or email and password are provided
include_once("db_connect_and_login.php");

// Get the requested variables
$sensorName	    	= $_REQUEST['sensorName'];
$devices = $_SESSION['devices'];

// check the device ID, and if its not send then use the last one
if(isset($_REQUEST['device_id']))
  $device_id	    	= $_REQUEST['device_id'];
$deviceID = "";
foreach($devices as $deviceID)
  if($device_id == $deviceID)      
    break;  
$device_id = $deviceID;

// get the right sensor type id
$sql = "select * from sensor_type where name='$sensorName'";
$result = mysql_query($sql);	
if(!$result)			 
    die("ERROR: unknown sensorName"); 
$sensorTypeStr = "";

while( $row = mysql_fetch_assoc($result))
{    
    if(strlen($sensorTypeStr) > 0)
      $sensorTypeStr .= " or ";
      $sensorTypeID = $row['id'];
      $sensorDataTypeArray[$sensorTypeID] = $row['data_type'];
      $sensorTypeStr .= " sensor_type ='$sensorTypeID' ";
}
// get the last value from the database
$sql = "select * from sensor_data where device_id='$device_id' and ( $sensorTypeStr ) ORDER BY date DESC LIMIT 1";
$result = mysql_query($sql);	
if(!$result)			 
    die("ERROR: sensor not available");  
$count	= mysql_num_rows($result);
if($count > 0)
{
  $row = mysql_fetch_assoc($result);
  $dataType = $sensorDataTypeArray[$row['sensor_type']];
  $sensorValue = $row['sensor_value'];	
  $sensorDate = $row['date'];
  echo "{\"sensorName\":\"$sensorName\",\"date\":$sensorDate,\"dataType\":\"$dataType\",\"sensorValue\":";
  if($dataType=="string")
    echo "\"$sensorValue\""; 
  else
    echo "$sensorValue"; 
  echo "}";
}  
else
  die("ERROR: no data available");  
?>