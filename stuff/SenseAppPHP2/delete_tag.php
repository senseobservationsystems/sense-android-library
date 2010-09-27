<?php
include("login_check.php");
include_once("error_codes.php");

$userId = valid_login();

if ($userId >= 0) {

    $id		= $_REQUEST['id'];
    $id 	= stripslashes($id);
    $id 	= mysql_real_escape_string($id);

    $sql	= "DELETE FROM `tags` WHERE `id` = '$id'";

    $result = mysql_query($sql);
    if ($result)  {
        $msg = "tag deleted";
        $response = array("status"=>"ok", "msg"=>$msg);
        die(json_encode($response));
    } else {
        $msg  = 'Invalid query: ' . mysql_error() . "\n";
        $msg .= 'Whole query: ' . $sql;
        $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$msg);
        die(json_encode($response));
    }
} else {
    $msg = "Error logging in: " + $userId;
    $response = array("status"=>"error", "faultcode"=>$fault_login, "msg"=>$msg);
    die(json_encode($response));
}

?>
