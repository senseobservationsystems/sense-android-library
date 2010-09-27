<?php
include("login_check.php");
include_once("error_codes.php");

$userId = valid_login();

if ($userId >= 0) {

    $tag		= $_REQUEST['tag'];
    $tagged_id	= $_REQUEST['tagged_id'];
    $type		= $_REQUEST['type'];

    $tag 		= stripslashes($tag);
    $tag 		= mysql_real_escape_string($tag);
    $tagged_id 	= stripslashes($tagged_id);
    $tagged_id 	= mysql_real_escape_string($tagged_id);
    $type 		= stripslashes($type);
    $type 		= mysql_real_escape_string($type);

    $sql		= "SELECT * FROM tags WHERE tag = '$tag' and tagged_id='$tagged_id' and type='$type'";

    $result		= mysql_query($sql);
    if ($result) {
        $count	= mysql_num_rows($result);
        // does not exist create
        if ($count == 0) {
            $sql = "INSERT INTO tags (`id`, `tag`, `tagged_id`, `type`, `date`) VALUES(NULL, '$tag', '$tagged_id', '$type', NOW())";
            $result		= mysql_query($sql);
            if($result) {
                $msg = "tag added";
                $response = array("status"=>"ok", "msg"=>$msg);
                die(json_encode($response));
            } else {
                $msg  = 'Invalid query: ' . mysql_error() . "\n";
                $msg .= 'Whole query: ' . $sql;
                $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$msg);
                die(json_encode($response));
            }
        } else {
            $msg  = "Error: tag already exists";
            $response = array("status"=>"error", "faultcode"=>$fault_parameter, "msg"=>$msg);
            die(json_encode($response));
        }
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
