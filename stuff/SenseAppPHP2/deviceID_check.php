<?php
include_once("db_connect.php");
include_once("error_codes.php");

if(!isset($_SESSION['userId'])) {
    $message  = "not logged in";
    $response = array("status"=>"error", "faultcode"=>$fault_login, "msg"=>$message);
    die(json_encode($response));
}
$userId		= $_SESSION['userId'];

// Check if a device id is send
if (isset($_REQUEST['uuid'])) {
    $devices	= $_SESSION['devices'];
    $uuid	= $_REQUEST['uuid'];
    $deviceId	= $devices[$uuid];
    // if the device is not in the list, then generate a new device in the db
    if (!isset($deviceId)) {
        // The type range is set to:
        // 1-1.000.000 = MyriaNode
        // 1.000.000 >= smartPhone
        if (((int)$uuid) <= 1000000) {
            $type = "myrianode";
        } else {
            $type = "smartPhone";
        }
        // Insert into DB
        $sql = "INSERT INTO `devices` (`id`,`user_id`,`type`,`uuid`,`date`) VALUES (NULL,'$userId','$type','$uuid',NOW())";
        $result = mysql_query($sql);
        if (!$result) {
            $message  = 'Invalid query: ' . mysql_error() . '\nWhole query: ' . $sql;
            $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$message);
            die(json_encode($response));
        }
        
        // Fetch sp_id
        $sql = "SELECT * FROM `devices` WHERE `uuid`='$uuid' AND `user_id` = '$userId'";
        $result = mysql_query($sql);
        if (!$result) {
            $msg  = 'Invalid query: ' . mysql_error() . '\nWhole query: ' . $sql;
            $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$msg);
            die(json_encode($response));
        }
        $row                    = mysql_fetch_assoc($result);
        $sp_id                  = $row['id'];
        $_SESSION['deviceId']   = $sp_id;
        $devices                = $_SESSION['devices'];
        $devices[$uuid]         = $sp_id;
	$_SESSION['devices']	= $devices;

        // create tag for the new device
        $tag = "";
        if (((int) $uuid) <= 1000000) {
            $tag = "/$userId/MyriaNed node $uuid/";
        } else {
            $tag = "/$userId/$type #$sp_id/";
        }
        $sql = "INSERT INTO `tags` (`id`,`tag`,`tagged_id`,`parent_id`,`type`,`date`) ";
        $sql .= "VALUES (NULL,'$tag','$sp_id','$userId','devices',NOW())";
        $result = mysql_query($sql);
        if (!$result) {
            $message  = 'Invalid query: ' . mysql_error() . '\nWhole query: ' . $sql;
            $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$message);
            die(json_encode($response));
        }
    }
} else if ($_SESSION['deviceId']) {
    $deviceId = $_SESSION['deviceId'];
}

if(!isset($deviceId)) {
    $msg = "Error: no deviceID";
    $response = array("status"=>"error", "faultcode"=>$fault_parameter, "msg"=>$msg);
    die(json_encode($response));
}

?>