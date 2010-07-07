<?php
include("db_connect.php");
$tbl_name="sp_sensor"; // Table name
if(!isset($_SESSION['userId']))
	die("Error: not logged in");
if(!isset($_SESSION['spId']))
	die("Error: smartPhone not checked");

$baseURL = "http://demo.almende.com/commonSense/";
// Get input
$userId		= $_SESSION['userId'];
$spId 		= $_SESSION['spId'];
$sensorName	= $_REQUEST['sensorName'];
//$sensorValue	= $_REQUEST['sensorValue'];

 // begin Dave B's Q&D file upload security code 
  $allowedExtensions = array("txt","csv","htm","html","xml", 
    "css","doc","xls","rtf","ppt","pdf","swf","flv","avi", 
    "wmv","mov","jpg","jpeg","gif","png", "3gp", "mp4"); 
  foreach ($_FILES as $file) { 
    if ($file['tmp_name'] > '') { 
      if (!in_array(end(explode(".", 
            strtolower($file['name']))), 
            $allowedExtensions)) { 
       die($file['name'].' is an invalid file type!<br/>'. 
        '<a href="javascript:history.go(-1);">'. 
        '&lt;&lt Go Back</a>'); 
      } 
    } 
  } 

if ($_FILES["file"]["error"] > 0)
{
    	echo "Return Code: " . $_FILES["file"]["error"] . "<br />";
}
else
{
	echo "Upload: " . $_FILES["file"]["name"] . "\n";
	//echo "Type: " . $_FILES["file"]["type"] . "<br />";
	//echo "Size: " . ($_FILES["file"]["size"] / 1024) . " Kb<br />";
	//echo "Temp file: " . $_FILES["file"]["tmp_name"] . "<br />";

		
	$fileBase = substr($_FILES["file"]["name"], 0, strlen($_FILES["file"]["name"])-4);
	$filePath = "upload/";
	$filePrefix = $userId."_".$spId."_";
	$newLocation =   $filePath.$filePrefix.$_FILES["file"]["name"];
	move_uploaded_file($_FILES["file"]["tmp_name"],$newLocation);
	$sensorValue = $baseURL.$newLocation;
	//if(substr_count(strtolower($_FILES["file"]["name"]),".3gp") || substr_count(strtolower($_FILES["file"]["name"]),".mp4"))
	//{
		$newLocationWave = $filePath.$filePrefix.$fileBase.".mp3";	
		$ffmpegCommand = "ffmpeg -y -i ". $newLocation ." -ac 1 -acodec libmp3lame -ar 22050 -f wav ".$newLocationWave;
		echo exec($ffmpegCommand);		
		echo exec("rm -rf ".$newLocation);
		$sensorValue = $baseURL.$newLocationWave;
	//}
	
	// update the database
	if($sensorName && $sensorValue)
	{
		// To protect MySQL injection (more detail about MySQL injection)	
		$sensorName 		= stripslashes($sensorName);	
		//$sensorValue 		= stripslashes($sensorValue);

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

		// Check if the database already has an entry
		$sql	= "SELECT * FROM sp_sensor WHERE sensor_value = '$sensorValue' and sensor_type = '$sensorType'";
		$result	= mysql_query($sql);	
		$count	= mysql_num_rows($result);

		// Get sensorType	
		if($count == 1)	
		{
			$row 		= mysql_fetch_assoc($result);	
			$sensorID 	= $row['id'];
			$time = microtime(true);
			$sql	= "UPDATE sp_sensor set date ='".$time."' where sensor_value = '$sensorValue' and sensor_type = '$sensorType'";
			$result	= mysql_query($sql);	
			if($result)
				echo "OK";
		}		
		else
		{	
			
			// Insert into DB
			$time = microtime(true);
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
	
	}
	else
		echo "Error: no sensorName or sensorValue given";
	}
 
?>
