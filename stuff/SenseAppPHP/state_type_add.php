<?php
include("db_connect.php");
$tbl_name="state_types"; // Table name
if(!isset($_SESSION['userId']))
	die("Error: not logged in");

// Get input
$userId		= $_SESSION['userId'];
$name		= $_REQUEST['name'];

if($name)
{
	// To protect MySQL injection (more detail about MySQL injection)	
	$name 		= stripslashes($name);	

	// Check if state already exists	
	$sql	= "SELECT * FROM $tbl_name WHERE name='$name'";
	$result	= mysql_query($sql);	
	$count	= mysql_num_rows($result);	
	if($count == 1)	
		echo "Error: state already exists";


	// Insert into DB
	$sql	= "INSERT INTO $tbl_name (`id` ,`name` ) VALUES (NULL ,  '$name')";
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
	echo "Error: no state name given";

?>
