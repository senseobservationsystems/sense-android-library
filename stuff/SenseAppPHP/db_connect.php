<?php
session_start(); 

$host		= "localhost"; 		// Host name 
$mysql_username	= "commonSense"; 	// Mysql username 
$mysql_password	= "senseo1234"; 	// Mysql password 
$db_name	= "commonSense"; 	// Database name 

// Connect to server and select databse.
mysql_connect("$host", "$mysql_username", "$mysql_password")or die("cannot connect"); 
mysql_select_db("$db_name")or die("cannot select DB");

?>
