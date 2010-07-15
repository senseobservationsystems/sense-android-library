<?php
include("db_connect.php");
$tbl_name="pop_quiz_question"; // Table name
if(!isset($_SESSION['userId']))
	die("Error: not logged in");

// Get input
$userId		= $_SESSION['userId'];
$question	= $_REQUEST['question'];

if($question)
{
	// To protect MySQL injection (more detail about MySQL injection)	
	$question 		= stripslashes($question);
	$question		= strip_tags($question);
	
	// Insert into DB
	$sql	= "INSERT INTO $tbl_name (`id` ,`user_id` ,`question` ,`hide` ,`date`) VALUES (NULL ,  '$userId', '$question',  'false',  NOW())";
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
	echo "Error: no question or answer given";
?>
