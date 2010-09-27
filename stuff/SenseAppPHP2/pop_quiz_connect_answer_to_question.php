<?php
include("db_connect.php");
include("error_codes.php");

$tbl_name="pop_quiz_question_answer"; // Table name

if(!isset($_SESSION['userId'])) {
    $msg = "not logged in";
    $response = array("status"=>"error", "faultcode"=>$fault_login, "msg"=>$msg);
    die(json_encode($response));
}

// Get input
$userId		= $_SESSION['userId'];
$questionId	= $_REQUEST['questionId'];
$answerId	= $_REQUEST['answerId'];

if ($questionId && $answerId) {
    // To protect MySQL injection (more detail about MySQL injection)
    $questionId 		= stripslashes($questionId);
    $answerId 		= stripslashes($answerId);

    // Check if the question exists
    $sql		= "SELECT * FROM pop_quiz_question WHERE id = '$questionId' and user_id='$userId'";
    $resultQ	= mysql_query($sql);
    $count		= mysql_num_rows($resultQ);

    if ($count == 0) {
        $msg = 'Error: question id not found ';
        $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$msg);
        die(json_encode($response));
    }
    // Check if the answer exists
    $sql		= "SELECT * FROM pop_quiz_answer WHERE id = '$answerId' and user_id='$userId'";
    $resultA	= mysql_query($sql);
    $count		= mysql_num_rows($resultA);

    if ($count == 0) {
        $msg = 'Error: answer id not found ';
        $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$msg);
        die(json_encode($response));
    }
    // Check if the question answer couple exists
    $sql	= "SELECT * FROM $tbl_name WHERE question_id = '$questionId' and answer_id='$answerId'";
    $result	= mysql_query($sql);
    $count	= mysql_num_rows($result);

    if ($count != 0) {
        $msg = 'Error: combination already exists';
        $response = array("status"=>"error", "faultcode"=>$fault_parameter, "msg"=>$msg);
        die(json_encode($response));
    }

    // Add the answer and question to the database
    $sql	= "INSERT INTO $tbl_name (`id` ,`question_id` ,`answer_id`) VALUES (NULL ,  '$questionId',  '$answerId')";
    $result	= mysql_query($sql);
    if ($result) {
        $msg = "question/answer connected";
        $response = array("status"=>"ok", "msg"=>$msg);
        die(json_encode($response));
    } else {
        $msg  = 'Invalid query: ' . mysql_error() . "\n";
        $msg .= 'Whole query: ' . $sql;
        $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$msg);
        die(json_encode($response));
    }
} else {
    $msg = "Error: no questionId or answerId given";
    $response = array("status"=>"error", "faultcode"=>$fault_parameter, "msg"=>$msg);
    die(json_encode($response));
}
?>
