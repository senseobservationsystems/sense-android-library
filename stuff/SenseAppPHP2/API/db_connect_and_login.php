<?php
include_once("db_only_connect.php");
include_once("login_check.php");

// try to login if session is not ok
if(isset($_REQUEST['email']) && isset($_REQUEST['password']) && $_SESSION['email'] != $_REQUEST['email'])
  $relogin = 1;
if(isset($_REQUEST['uuid']) && $_REQUEST['uuid'] != $_SESSION['uuid'])
  $relogin =1;
if (false == isset($_SESSION['userId']) || isset($relogin)) {
    if (($return = valid_login()) < 0) {               
        die("ERROR: login failed:$return\n");               
    }                                      
}  
?>
