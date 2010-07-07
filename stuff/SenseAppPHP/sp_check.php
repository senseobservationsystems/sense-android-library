<?php
include("db_connect.php");
$tbl_name="smartPhone"; // Table name
if(!isset($_SESSION['userId']))
	die("Error: not logged in");

// Get Input
$userId		= $_SESSION['userId'];
$imei 		= $_REQUEST['imei'];

if($imei)
{
	// To protect MySQL injection (more detail about MySQL injection)	
	$imei 		= stripslashes($imei);	
	
	// Check if the phone is already added
	$sql="SELECT * FROM $tbl_name WHERE imei='$imei' and user_id='$userId'";
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
		$_SESSION['spId']  = $row['id'];
		
		echo "OK";	
	}
	else 			
		echo "Error: unkown imei";
	
}
else
	echo "Error: no imei given";

?>
