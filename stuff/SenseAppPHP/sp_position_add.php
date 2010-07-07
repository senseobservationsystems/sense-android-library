<?php
include("db_connect.php");
$tbl_name="sp_global_position"; // Table name
if(!isset($_SESSION['userId']))
	die("Error: not logged in");
if(!isset($_SESSION['spId']))
	die("Error: smartPhone not checked");

// Get input
$userId		= $_SESSION['userId'];
$spId 		= $_SESSION['spId'];
$longitude	= $_REQUEST['longitude'];
$latitude	= $_REQUEST['latitude'];

if($longitude && $latitude)
{
	// To protect MySQL injection (more detail about MySQL injection)	
	$longitude 		= stripslashes($longitude);	
	$latitude 		= stripslashes($latitude);
	
	// Insert into DB
	$sql	= "INSERT INTO $tbl_name (`id` ,`sp_id` ,`longitude` ,`latitude` ,`date`) VALUES (NULL ,  '$spId', '$longitude',  '$latitude',  NOW())";
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
	echo "Error: no longitude or longitude given";

?>
