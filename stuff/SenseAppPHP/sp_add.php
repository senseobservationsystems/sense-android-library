<?php
include("db_connect.php");
$tbl_name="smartPhone"; // Table name
if(!isset($_SESSION['userId']))
	die("Error: not logged in.");

// Get Input
$userId		= $_SESSION['userId'];
$brand		= $_REQUEST['brand'];
$type		= $_REQUEST['type'];
$ipAddress	= $_REQUEST['ipAddress'];
$imei 		= $_REQUEST['imei'];
$phoneNr	= $_REQUEST['phoneNr'];

if($imei)
{
	// To protect MySQL injection (more detail about MySQL injection)
	$type 		= stripslashes($type);
	$ipAddress 	= stripslashes($ipAddress);
	$brand 		= stripslashes($brand);
	$imei 		= stripslashes($imei);
	$phoneNr 	= mysql_real_escape_string($phoneNr);
	
	// Check if the phone is already added
	$sql	= "SELECT * FROM $tbl_name WHERE imei='$imei' and user_id='$userId'";
	$result	= mysql_query($sql);
	if(!$result)			
	{	
		$message  = 'Invalid query: ' . mysql_error() . "\n";
		$message .= 'Whole query: ' . $query;
		die($message);
	}
	$count	= mysql_num_rows($result);

	// If phone is already added table row count is 1
	if($count==1)	
		echo "Error: phone already exists";	
	else 
	{
		// Insert into DB
		$sql	= "INSERT INTO $tbl_name (`id` ,`user_id` ,`brand` ,`type` ,`imei`, `ip_address`, `phone_number`, `date`) VALUES (NULL ,  '$userId', '$brand',  '$type', '$imei', '$ipAddress', '$phoneNr', NOW())";
		$result	= mysql_query($sql);	
		if(!$result)		
		{	
			$message  = 'Invalid query: ' . mysql_error() . "\n";
			$message .= 'Whole query: ' . $query;
			die($message);
		}

		// Fetch sp_id
		$sql			= "SELECT * FROM $tbl_name WHERE imei='$imei'";
		$result			= mysql_query($sql);
		if(!$result)		
		{	
			$message  = 'Invalid query: ' . mysql_error() . "\n";
			$message .= 'Whole query: ' . $query;
			die($message);
		}
		$row 			= mysql_fetch_assoc($result);		
		$_SESSION['spId']  	= $row['id'];
		echo "OK";
	}
}
else
	echo "Error: no imei given";

?>
