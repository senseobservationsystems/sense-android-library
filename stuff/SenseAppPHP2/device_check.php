<?php
include("db_connect.php");
include_once("error_codes.php");
$tbl_name="devices"; // Table name

if (!isset($_SESSION['userId'])) {
    $msg = "not logged in";
    $response = array("status"=>"error", "faultcode"=>$fault_login, "msg"=>$msg);
    die(json_encode($response));
}

// Get Input
$userId		= $_SESSION['userId'];
$uuid 		= $_REQUEST['uuid'];

if ($uuid) {
     // Check if the phone is already added
    $sql = "SELECT * FROM `$tbl_name` WHERE `uuid`='$uuid' and `user_id`='$userId'";
    $result = mysql_query($sql);
    if (!$result) {
        $msg  = 'Invalid query: ' . mysql_error() . "\n";
        $msg .= 'Whole query: ' . $sql;
        $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$msg);
        die(json_encode($response));
    }
    $count = mysql_num_rows($result);
    if ($count == 1) {
        $row = mysql_fetch_assoc($result);
        $_SESSION['deviceId'] = $row['id'];

        $msg = "device recognized";
        $response = array("status"=>"ok", "msg"=>$msg);
        die(json_encode($response));
    } else {
        $msg  = "Error: unknown device";
        $response = array("status"=>"error", "faultcode"=>$fault_parameter, "msg"=>$msg);
        die(json_encode($response));
    }
} else {
    $msg  = "Error: no uuid given";
    $response = array("status"=>"error", "faultcode"=>$fault_parameter, "msg"=>$msg);
    die(json_encode($response));
}

?>
