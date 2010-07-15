<?php
include("db_connect.php");
$tbl_name="pop_quiz_answer"; // Table name
if(!isset($_SESSION['userId']))
	die("Error: not logged in");

// Get input
$userId		= $_SESSION['userId'];
$answer		= $_REQUEST['answer'];

if($answer)
{
	// To protect MySQL injection (more detail about MySQL injection)	
	$answer 		= stripslashes($answer);	
	
	// Insert into DB
	$sql	= "INSERT INTO $tbl_name (`id` ,`user_id` ,`answer` ,`hide`) VALUES (NULL ,  '$userId', '$answer',  'false')";
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
	echo "Error: no answer or answer given";
?>
