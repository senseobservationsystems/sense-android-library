<?php
include("db_connect.php");
include_once("error_codes.php");
$tbl_name="devices"; // Table name

if(!isset($_SESSION['userId'])) {
    $msg = "not logged in.";
    $response = array("status"=>"error", "faultcode"=>$fault_login, "msg"=>$msg);
    die(json_encode($response));
}

// Get Input
$userId		= $_SESSION['userId'];
$uuid 		= $_REQUEST['uuid'];
$type		= $_REQUEST['type'];

if ($uuid) {
     // Check if the phone is already added
    $sql = "SELECT * FROM `$tbl_name` WHERE uuid='$uuid' AND user_id='$userId'";
    $result	= mysql_query($sql);
    if (!$result) {
        $msg  = 'Invalid query: ' . mysql_error() . '\nWhole query: ' . $sql;
        $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$msg);
        die(json_encode($response));
    }
    $count	= mysql_num_rows($result);

    // If the device is already added table row count is 1
    if ($count > 0) {
        $msg  = 'Error: device already exists for this user';
        $response = array("status"=>"error", "faultcode"=>$fault_parameter, "msg"=>$msg);
        die(json_encode($response));
    } else {
        // Insert into DB
        $sql = "INSERT INTO `$tbl_name` (`id`,`user_id`,`type`,`uuid`,`date`) VALUES (NULL,'$userId','$type','$uuid',NOW())";
        $result = mysql_query($sql);
        if (!$result) {
            $msg  = 'Invalid query: ' . mysql_error() . "\n";
            $msg .= 'Whole query: ' . $sql;
            $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$msg);
            die(json_encode($response));
        }

        // Fetch sp_id
        $sql = "SELECT * FROM `$tbl_name` WHERE `uuid`='$uuid' AND `user_id` = '$userId'";
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

        // Create tag for device
        if (((int) $uuid) <= 1000000) {
            $tag = "/$userId/MyriaNed node $uuid/";
        } else {
            $tag = "/$userId/$type #$sp_id/";
        }
        $sql = "INSERT INTO `tags` (`id`,`tag`,`tagged_id`,`parent_id`,`type`,`date`) ";
        $sql .= "VALUES (NULL,'/$userId/$type #$sp_id/','$sp_id','$userId','devices',NOW())";
        $result = mysql_query($sql);
        if (!$result) {
            $msg  = 'Invalid query: ' . mysql_error() . "\n";
            $msg .= 'Whole query: ' . $sql;
            $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$msg);
            die(json_encode($response));
        }

        $msg = "device added";
        $response = array("status"=>"ok", "msg"=>$msg);
        die(json_encode($response));
    }
} else {
    $msg  = "Error: uuid not found";
    $response = array("status"=>"error", "faultcode"=>$fault_parameter, "msg"=>$msg);
    die(json_encode($response));
}

?>
