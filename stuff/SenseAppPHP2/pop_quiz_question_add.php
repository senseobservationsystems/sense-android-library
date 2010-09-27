<?php
include("db_connect.php");
include_once 'error_codes.php';

$tbl_name="pop_quiz_question"; // Table name

if(!isset($_SESSION['userId'])) {
    $msg = "not logged in";
    $response = array("status"=>"error", "faultcode"=>$fault_login, "msg"=>$msg);
    die(json_encode($response));
}

// Get input
$userId		= $_SESSION['userId'];
$question	= $_REQUEST['question'];

if ($question) {
    // To protect MySQL injection (more detail about MySQL injection)
    $question 		= stripslashes($question);
    $question		= strip_tags($question);

    // Insert into DB
    $sql	= "INSERT INTO $tbl_name (`id` ,`user_id` ,`question` ,`hide` ,`date`) VALUES (NULL ,  '$userId', '$question',  'false',  NOW())";
    $result	= mysql_query($sql);
    if($result) {
        $msg = "question added";
        $response = array("status"=>"ok", "msg"=>$msg);
        die(json_encode($response));
    } else {
        $msg  = 'Invalid query: ' . mysql_error() . "\n";
        $msg .= 'Whole query: ' . $sql;
        $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$msg);
        die(json_encode($response));
    }
} else {
    $msg = "Error: no question or answer given";
    $response = array("status"=>"error", "faultcode"=>$fault_parameter, "msg"=>$msg);
    die(json_encode($response));
}
?>
