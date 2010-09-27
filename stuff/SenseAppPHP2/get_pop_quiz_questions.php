<?php
include("db_connect.php");
include_once("error_codes.php");

$tbl_name="pop_quiz_question"; // Table name
if(!isset($_SESSION['userId'])) {
    $msg = "not logged in";
    $response = array("status"=>"error", "faultcode"=>$fault_login, "msg"=>$msg);
    die(json_encode($response));
}

// Get input
$userId		= $_SESSION['userId'];
// To protect MySQL injection (more detail about MySQL injection)
$sql		= "SELECT * FROM pop_quiz_question WHERE user_id = '$userId' and hide = false";
$result		= mysql_query($sql);

if ($result) {
    while($row = mysql_fetch_array($result)) {
        $questionID =  $row['id'];
        $questionValue = $row['question'];
        echo "{\"questions\": [\n";
        echo "{\n";
        echo "\"question_id\":\"$questionID\",\n";
        echo "\"question_value\":\"$questionValue\",\n";
        echo "\"answer\":[\n";
        $answerList = "";
        $sql		= "SELECT pop_quiz_answer.* FROM pop_quiz_answer, pop_quiz_question_answer WHERE pop_quiz_answer.user_id = '$userId' and pop_quiz_answer.hide = false and pop_quiz_question_answer.question_id='$questionID' and pop_quiz_question_answer.answer_id=pop_quiz_answer.id";
        $resultAnswer	= mysql_query($sql);
        while ($rowAnswer = mysql_fetch_array($resultAnswer)) {
            $answerID = $rowAnswer['id'];
            $answerValue = $rowAnswer['answer'];
            $answerList .= "{\"answer_id\":\"$answerID\",\"answer_value\":\"$answerValue\"},";
        }
        echo substr($answerList, 0, -1); // remove the last ,
        echo "]\n";
        echo "}\n";
        echo "]\n";
        echo "}\n";
    }
} else {
    $msg  = 'Invalid query: ' . mysql_error() . "\n";
    $msg .= 'Whole query: ' . $sql;
    $response = array("status"=>"error", "faultcode"=>$fault_internal, "msg"=>$msg);
    die(json_encode($response));
}
?>
