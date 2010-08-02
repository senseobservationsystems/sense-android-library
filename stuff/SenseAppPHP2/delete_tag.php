<?php
include("login_check.php");

$userId = valid_login();

if ($userId >= 0) {

	$id		= $_REQUEST['id'];
	$id 		= stripslashes($id);
	$id 		= mysql_real_escape_string($id);	
    
	$sql		= "DELETE FROM tags WHERE id = '$id'";

	$result		= mysql_query($sql);	
	if($result) 
	{
	    echo "OK";  		
	}
	else {	
		$message  = 'Invalid query: ' . mysql_error() . "\n";
		$message .= 'Whole query: ' . $query;
		die($message);
	}
} else {
	$message  = 'Invalid login';
}

?>
