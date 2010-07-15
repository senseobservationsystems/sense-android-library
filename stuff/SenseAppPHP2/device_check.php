<?php
include("db_connect.php");
$tbl_name="devices"; // Table name
if(!isset($_SESSION['userId']))
	die("Error: not logged in");

// Get Input
$userId		= $_SESSION['userId'];
$uuid 		= $_REQUEST['uuid'];

if($uuid)
{
	// To protect MySQL injection (more detail about MySQL injection)	
	$uuid 		= stripslashes($uuid);	
	
	// Check if the phone is already added
	$sql="SELECT * FROM $tbl_name WHERE uuid='$uuid' and user_id='$userId'";
	$result=mysql_query($sql);
	if(!$result)			
	{	
		$message  = 'Invalid query: ' . mysql_error() . "\n";
		$message .= 'Whole query: ' . $query;
		die($message);
	}	
	$count=mysql_num_rows($result);
	if($count==1)
	{	
		$row = mysql_fetch_assoc($result);	
		$_SESSION['deviceId']  = $row['id'];
		
		echo "OK";	
	}
	else 			
		echo "Error: unkown device";
	
}
else
	echo "Error: no uuid given";

?>
