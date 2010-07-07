<?php
include("db_connect.php");
$tbl_name="users"; // Table name

// Define $email and $password 
$email		= $_REQUEST['email']; 
$password	= $_REQUEST['password'];

if($email && $password)
{
	// To protect MySQL injection (more detail about MySQL injection)
	$email 		= stripslashes($email);
	$password 	= stripslashes($password);
	$email 		= mysql_real_escape_string($email);
	$password 	= mysql_real_escape_string($password);
	$password 	= md5($password);

	// Check the login credentials
	$sql	= "SELECT * FROM $tbl_name WHERE email='$email' and password='$password'";
	$result	= mysql_query($sql);	
	if(!$result)			
	{	
		$message  = 'Invalid query: ' . mysql_error() . "\n";
		$message .= 'Whole query: ' . $query;
		die($message);
	}
	$count	= mysql_num_rows($result);

	// If result matched $email and $password, table row must be 1 row
	if($count == 1)
	{
		// Register id
		$row = mysql_fetch_assoc($result);		
		$_SESSION['userId']  = $row['id'];
		echo "OK";
	}
	else 	
		echo "Wrong username or password";	
}
?>
