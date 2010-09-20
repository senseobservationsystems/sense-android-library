<?php

include_once("db_only_connect.php");
include_once("login_check.php");

// try to login if session is not ok
if (false == isset($_SESSION['userId'])) {
    if (($return = valid_login()) < 0) {
        $msg = "Error logging in: $return";
        $response = array("status"=>"error", "faultcode"=>1, "msg"=>$msg);
        die(json_encode($response));               
    }
}
?>
