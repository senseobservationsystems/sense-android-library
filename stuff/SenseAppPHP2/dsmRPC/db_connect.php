<?php
session_start();

$host		= "demo.almende.com";//"localhost"; 		// Host name 
$mysql_username	= "ted";//"commonSense"; 	// Mysql username 
$mysql_password	= "09091983rd";//"senseo1234"; 	// Mysql password 
$db_name	= "commonSense2"; 	// Database name 

// Connect to server and select databse.
mysql_connect("$host", "$mysql_username", "$mysql_password")or die("cannot connect");
mysql_select_db("$db_name")or die("cannot select DB");
                                   
?>
