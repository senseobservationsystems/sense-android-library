<?php
include("db_connect.php");
$tbl_name="devices"; // Table name
if(!isset($_SESSION['userId']))
	die("Error: not logged in.");

// Get Input
$userId		= $_SESSION['userId'];
$uuid 		= $_REQUEST['uuid'];
$type		= $_REQUEST['type'];

if($uuid)
{
	// To protect MySQL injection (more detail about MySQL injection)
	$uuid 		= stripslashes($uuid);
	$type 		= stripslashes($type);
	
	// Check if the phone is already added
	$sql	= "SELECT * FROM $tbl_name WHERE uuid='$uuid' and user_id='$userId'";
	$result	= mysql_query($sql);
	if(!$result)			
	{	
		$message    = 'Invalid query: ' . mysql_error() . "\n";
		$message   .= 'Whole query: ' . $query;
		die($message);
	}
	$count	= mysql_num_rows($result);

	// If the device is already added table row count is 1
	if($count==1)	
		echo "Error: device already exists";	
	else 
	{
		// Insert into DB
		$sql    = "INSERT INTO $tbl_name (`id` ,`user_id` ,`type` ,`uuid`, `date`) VALUES (NULL ,  '$userId', '$type', '$uuid', NOW())";
		$result = mysql_query($sql);	
		if(!$result)		
		{	
			$message    = 'Invalid query: ' . mysql_error() . "\n";
			$message   .= 'Whole query: ' . $query;
			die($message);
		}

		// Fetch sp_id
		$sql	= "SELECT * FROM $tbl_name WHERE uuid='$uuid'";
		$result = mysql_query($sql);
		if(!$result)		
		{	
			$message    = 'Invalid query: ' . mysql_error() . "\n";
			$message   .= 'Whole query: ' . $query;
			die($message);
		}
		$row                    = mysql_fetch_assoc($result);
		$sp_id                  = $row['id'];
		$_SESSION['deviceId']   = $sp_id;
		$devices                = $_SESSION['devices'];
		$devices[$uuid]         = $sp_id;
		
		// Create tag for device
		$sql    = "INSERT INTO `tags` (`id`, `tag`, `tagged_id`, `parent_id`, `type`, `date`) ";
		$sql   .= "VALUES (NULL, '/$userId/$type #$sp_id/', '$sp_id', '$userId', 'devices', NOW())";
		$result = mysql_query($sql);
		if(!$result)		
		{	
			$message    = 'Invalid query: ' . mysql_error() . "\n";
			$message   .= 'Whole query: ' . $query;
			die($message);
		}
		
		echo "OK";
	}
}
else
	echo "Error: no deviceId given";

?>
