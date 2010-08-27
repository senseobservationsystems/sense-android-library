<?php
session_start();
include_once("login_check.php");


$host		= "localhost"; 		// Host name 
$mysql_username	= "commonSense"; 	// Mysql username 
$mysql_password	= "senseo1234"; 	// Mysql password 
$db_name	= "commonSense2"; 	// Database name 

// Connect to server and select databse.
mysql_connect("$host", "$mysql_username", "$mysql_password")or die("cannot connect");
mysql_select_db("$db_name")or die("cannot select DB");
// try to login if session is not ok
if (false == isset($_SESSION['userId'])) {
    if (($return = valid_login()) < 0) {               
        die("login failed:$return\n");               
    }                                      
}  
?>
