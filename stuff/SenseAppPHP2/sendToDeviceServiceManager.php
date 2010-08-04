<?php
include_once("db_connect.php");
error_reporting(0);
function sendDeviceServiceMessage($device_id, $dataType, $sensorValue, $sk)
{
	if (is_resource($sk)) 
	{
		// A message has the following structure:
		// 0x01 device_id 0x20 dataType 0x20 data_size 0x02 data 0x03

		$START_HEADER	= 0x01; // SOH
		$START_TEXT	= 0x02; // STX
		$END_TEXT	= 0x03; // ETX
		$SEPARATOR	= 0x20;	// SPACE

		$dataSize	= strlen($sensorValue);
	 	fputs($sk, chr($START_HEADER).$device_id.chr($SEPARATOR).$dataType.chr($SEPARATOR).$dataSize.chr($START_TEXT).$sensorValue.chr($END_TEXT));	//fclose($sk);				
	}	
}

function sendToDeviceServiceManager($device_id, $sensorDataType, $sensorName, $sensorValue)
{
	$host		=	"localhost" ;
	$port		=	1337;
	$timeout	=	2;
	$sk = stream_socket_client("tcp://$host:$port", $errno, $errstr, $timeout,STREAM_CLIENT_CONNECT | STREAM_CLIENT_PERSISTENT);

	//remove spaces from the sensor name
	$sensorName = str_replace(" ", "_", $sensorName);
	if($sensorDataType=='json')
	{
		// create the dataType from the sensor name + extra data in the Json object
		// parse JSON, for each value send
		$sensorValue = stripslashes($sensorValue);	     
		$jsonObject = json_decode($sensorValue);
		
		while(list($key, $value) = each($jsonObject))
		{ 
		    sendDeviceServiceMessage($device_id, $sensorName.".".str_replace(" ", "_", $key), $value, $sk); 
		}
	}
	else// If the data is not JSON, then the sensorName is used as the dataType
	 	sendDeviceServiceMessage($device_id, $sensorName, $sensorValue, $sk);      
}	
?>
