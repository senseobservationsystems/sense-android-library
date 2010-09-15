<?php
include_once("db_connect.php");

if(!isset($_SESSION['userId'])) {
    die("Error: not logged in");
}
$userId		= $_SESSION['userId'];

// Check if a device id is send
if (isset($_REQUEST['uuid'])) {
    $devices	= $_SESSION['devices'];
    $uuid		= $_REQUEST['uuid'];
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
            $message  = 'Invalid query: ' . mysql_error() . "\n";
            $message .= 'Whole query: ' . $query;
            die($message);
        }
        
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
            $message = 'Invalid query: ' . mysql_error() . "\n";
            $message .= 'Whole query: ' . $query;
            die($message);
        }

        // Fetch sp_id
        $sql = "SELECT * FROM `devices` WHERE `uuid`='$uuid' and `user_id`='$userId'";
        $result = mysql_query($sql);
        if (!$result) {
            $message  = 'Invalid query: ' . mysql_error() . "\n";
            $message .= 'Whole query: ' . $query;
            die($message);
        }
        $row = mysql_fetch_assoc($result);
        $devices[$uuid] = $row['id'];
        $_SESSION['devices'] = $devices;
        $deviceId = $row['id'];
    }
} else if ($_SESSION['deviceId']) {
    $deviceId = $_SESSION['deviceId'];
}

if(!isset($deviceId)) {
    die("Error: no deviceID");
}

?>