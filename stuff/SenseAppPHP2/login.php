<?php
include_once("login_check.php");
include_once("error_codes.php");

// check login with valid_login() from login_check.php
switch (valid_login()) {
    case -1:
        $msg    = "invalid SQL query using provided credentials";
        $response = array("status"=>"error", "faultcode"=>$fault_login, "msg"=>$msg);
        die(json_encode($response));
        break;
    case -2:
        $msg    = "invalid SQL query for user's devices failed";
        $response = array("status"=>"error", "faultcode"=>$fault_login, "msg"=>$msg);
        die(json_encode($response));
        break;
    case -3:
        $msg    = "more than one user found (?!)";
        $response = array("status"=>"error", "faultcode"=>$fault_login, "msg"=>$msg);
        die(json_encode($response));
        break;
    case -4:
        $msg    = "username or password not provided";
        $response = array("status"=>"error", "faultcode"=>$fault_parameter, "msg"=>$msg);
        die(json_encode($response));
        break;
    default:
        $msg = "logged in";
        $response = array("status"=>"ok", "msg"=>$msg);
        die(json_encode($response));
        break;
}

