<?php
include("db_connect.php");
$tbl_name="sp_state"; // Table name
if(!isset($_SESSION['userId']))
	die("Error: not logged in");
if(!isset($_SESSION['spId']))
	die("Error: smartPhone not checked");

// Get input
$userId		= $_SESSION['userId'];
$spId 		= $_SESSION['spId'];
$stateName	= $_REQUEST['stateName'];
$stateValue	= $_REQUEST['stateValue'];

if($stateName && $stateValue)
{
	// To protect MySQL injection (more detail about MySQL injection)	
	$stateName 		= stripslashes($stateName);	
	$stateValue 		= stripslashes($stateValue);
	
	// Check if the state exists	
	$sql	= "SELECT * FROM state_types WHERE name = '$stateName'";
	$result	= mysql_query($sql);
	if(!$result)			
	{	
		$message  = 'Invalid query: ' . mysql_error() . "\n";
		$message .= 'Whole query: ' . $query;
		die($message);
	}	
	$count	= mysql_num_rows($result);

	// Get stateType	
	if($count == 1)	
	{
		$row 		= mysql_fetch_assoc($result);	
		$stateType 	= $row['id'];
	}
	// Create new state type
	else
	{	
		$sql	= "INSERT INTO state_types (`id` ,`name` ) VALUES (NULL ,  '$stateName')";
		$result	= mysql_query($sql);
		if($result)
		{
			// Get stateType
			$sql		= "SELECT * FROM state_types WHERE name = '$stateName'";
			$result		= mysql_query($sql);	
			$row 		= mysql_fetch_assoc($result);	
			$stateType 	= $row['id'];
		}
		else
		{	
			$message  = 'Invalid query: ' . mysql_error() . "\n";
			$message .= 'Whole query: ' . $query;
			die($message);
		}			
	}

	// Insert into DB
	$sql	= "INSERT INTO $tbl_name (`id` ,`sp_id` ,`state_type` ,`state_value` ,`date`) VALUES (NULL ,  '$spId', '$stateType',  '$stateValue',  NOW())";
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
	echo "Error: no stateName or stateValue given";

?>
