<?php
session_start();
include_once("error_codes.php");

$host		= "localhost"; 		// Host name 
$mysql_username	= "commonSense"; 	// Mysql username 
$mysql_password	= "senseo1234"; 	// Mysql password 
$db_name	= "commonSense2"; 	// Database name 


// Connect to server and select databse.
$resource = mysql_connect("$host", "$mysql_username", "$mysql_password");
if (!$resource) {
    $msg = "Cannot connect to MySQL";
    $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$msg);
    die(json_encode($response));   
}
$db_ok = mysql_select_db("$db_name")or die("cannot select DB");
if(!$db_ok) {
    $msg = "Cannot select database";
    $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$msg);
    die(json_encode($response));     
}
?>
