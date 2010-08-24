<?php
include_once("login_check.php");

// check login with valid_login() from login_check.php
switch (valid_login()) {
    case -1:
        $message    = "invalid SQL query using provided credentials";	
        die($message);
        break;
    case -2:
        $message    = "invalid SQL query for user's devices failed";
        die($message);
        break;
    case -3:
        $message    = "more than one user found (?!)";
        die($message);
        break;
    case -4:
        $message    = "username or password not provided";
        die($message);
        break;
    default:
        echo "OK";
        break;
}

