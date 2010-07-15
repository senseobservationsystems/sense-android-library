<?php
include("db_connect.php");
$tbl_name="sensor_data"; // Table name
if(!isset($_SESSION['userId']))
	die("Error: not logged in");
if(!isset($_SESSION['deviceId']))
	die("Error: smartPhone not checked");

// Get input
$userId		= $_SESSION['userId'];
$deviceId 	= $_SESSION['deviceId'];
$sensorName	= $_REQUEST['sensorName'];
$sensorValue	= $_REQUEST['sensorValue'];
$sensorDataType = $_REQUEST['sensorDataType'] ;

if($sensorName && $sensorValue)
{
	// To protect MySQL injection (more detail about MySQL injection)	
	$sensorName 		= stripslashes($sensorName);	
	$sensorValue 		= stripslashes($sensorValue);
	$sensorDataType 	= stripslashes($sensorDataType);

	// Check if the sensor exists	
	$sql	= "SELECT * FROM sensor_type WHERE name = '$sensorName'";
	$result	= mysql_query($sql);	
	$count	= mysql_num_rows($result);

	// Get sensorType	
	if($count == 1)	
	{
		$row 		= mysql_fetch_assoc($result);	
		$sensorType 	= $row['id'];
	}
	// Create new sensor type
	else
	{	
		$sql	= "INSERT INTO sensor_type (`id` ,`name`, `data_type` ) VALUES (NULL ,  '$sensorName', '$sensorDataType')";
		$result	= mysql_query($sql);
		if($result)
		{
			// Get sensorType
			$sql		= "SELECT * FROM sensor_type WHERE name = '$sensorName'";
			$result		= mysql_query($sql);	
			$row 		= mysql_fetch_assoc($result);	
			$sensorType 	= $row['id'];
		}
		else
		{	
			$message  = 'Invalid query: ' . mysql_error() . "\n";
			$message .= 'Whole query: ' . $query;
			die($message);
		}			
	}
	
	// Insert into DB
	$time = microtime(true);
	$sql	= "INSERT INTO $tbl_name (`id` ,`device_id` ,`sensor_type` ,`sensor_value` ,`date`) VALUES (NULL ,  '$deviceId', '$sensorType',  '$sensorValue',  '$time')";
	$result	= mysql_query($sql);
	if($result)
		echo "OK";
	else
	{	
		$message  = 'Invalid query: ' . mysql_error() . "\n";
		$message .= 'Whole query: ' . $query;
	        die($message);
	}	
	
}
else
	echo "Error: no sensorName or sensorValue given";

?>
