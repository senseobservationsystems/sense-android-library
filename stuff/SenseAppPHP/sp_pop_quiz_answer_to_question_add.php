<?php
include("db_connect.php");
$tbl_name="sp_pop_quiz"; // Table name
if(!isset($_SESSION['userId']))
	die("Error: not logged in");
if(!isset($_SESSION['spId']))
	die("Error: smartPhone not checked");

// Get input
$userId		= $_SESSION['userId'];
$spId 		= $_SESSION['spId'];
$questionId	= $_REQUEST['questionId'];
$answerId	= $_REQUEST['answerId'];
$quizDate	= $_REQUEST['quizDate'];

if($questionId && $answerId && $quizDate)
{
	// To protect MySQL injection (more detail about MySQL injection)	
	$questionId 		= stripslashes($questionId);	
	$answerId 		= stripslashes($answerId);
	$quizDate 		= stripslashes($quizDate);


	// Check if the question exists	
	$sql	= "SELECT * FROM pop_quiz_question WHERE id = '$questionId'";
	$result	= mysql_query($sql);	
	$count	= mysql_num_rows($result);
	
	if($count == 0)	
	{
		die('Error: question id not found ');
	}
	// Check if the answer exists	
	$sql	= "SELECT * FROM pop_quiz_answer WHERE id = '$answerId'";
	$result	= mysql_query($sql);	
	$count	= mysql_num_rows($result);
	
	if($count == 0)	
	{
		die('Error: answer id not found ');
	}
	// Add the answer and question to the database
	$sql	= "INSERT INTO $tbl_name (`id` ,`sp_id` ,`pop_quiz_question_id` ,`pop_quiz_answer_id` ,`quiz_date`, `date`) VALUES (NULL ,  '$spId', '$questionId',  '$answerId', '$quizDate',  NOW())";
	$result	= mysql_query($sql);
	if($result)
		echo "OK";
	else
	{	
		$message  = 'Invalid query: ' . mysql_error() . "\n";
		$message .= 'Whole query: ' . $query;
		die($message);
	}	
	
	
}
else
	echo "Error: no questionId, answerId, or quiz_date given";

?>
