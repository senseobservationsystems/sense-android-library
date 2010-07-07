<?php
include("db_connect.php");
$tbl_name="sp_sensor"; // Table name
if(!isset($_SESSION['userId']))
	die("Error: not logged in");
if(!isset($_SESSION['spId']))
	die("Error: smartPhone not checked");

// Get input
$userId		= $_SESSION['userId'];
$spId 		= $_SESSION['spId'];
$sensorName	= $_REQUEST['sensorName'];
$sensorValue	= $_REQUEST['sensorValue'];

if($sensorName && $sensorValue)
{
	// To protect MySQL injection (more detail about MySQL injection)	
	$sensorName 		= stripslashes($sensorName);	
	$sensorValue 		= stripslashes($sensorValue);

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
		$sql	= "INSERT INTO sensor_type (`id` ,`name` ) VALUES (NULL ,  '$sensorName')";
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
	$time = microtime(false);
	$sql	= "INSERT INTO $tbl_name (`id` ,`sp_id` ,`sensor_type` ,`sensor_value` ,`date`) VALUES (NULL ,  '$spId', '$sensorType',  '$sensorValue',  '$time')";
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
