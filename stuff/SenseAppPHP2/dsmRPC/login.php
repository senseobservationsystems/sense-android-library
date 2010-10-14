<?php
include_once("../db_only_connect.php");
$tbl_name="users"; // Table name

// Define $email and $password 
$email		= $_REQUEST['email']; 
$password	= $_REQUEST['password'];

if(isset($_REQUEST['logout']))
{
  session_destroy();
  session_start();
  //unset($_SESSION['userId']);
  //sunset($_SESSION['devices']);
}
if($email && $password)
{
	// To protect MySQL injection (more detail about MySQL injection)
	$email 		= stripslashes($email);
	$password 	= stripslashes($password);
	$email 		= mysql_real_escape_string($email);
	$password 	= mysql_real_escape_string($password);
	$password 	= md5(md5($password));

	// Check the login credentials
	$sql	= "SELECT * FROM $tbl_name WHERE email='$email' and password='$password'";
	$result	= mysql_query($sql);	
	if(!$result)			
	{	
		$message  = 'Invalid query: ' . mysql_error() . "\n";
		$message .= 'Whole query: ' . $query;
		die($message);
	}
	$count	= mysql_num_rows($result);

	// If result matched $email and $password, table row must be 1 row
	if($count == 1)
	{
		// Register id
		$row = mysql_fetch_assoc($result);	
		$userId = $row['id'];
		$_SESSION['userId']  = $userId;	
		$userName = $row['name'];
		if(strlen($userName) == 0)
		  $_SESSION['userName']  = $row['email'];
		else
		  $_SESSION['userName']  = $userName;	
	        $_SESSION['uuid']  = $row['UUID'];
		// cach the devices in the database connected to this userId
		$sql	= "SELECT * FROM devices WHERE user_id='$userId'";
		$result	= mysql_query($sql);	
		if(!$result)			
		{	
			$message  = 'Invalid query: ' . mysql_error() . "\n";
			$message .= 'Whole query: ' . $query;
			die($message);
		}
		else
		{
		  $devices;
		  while ($row = mysql_fetch_assoc($result))
		  {
		    $devices[$row['uuid']] = $row['id'];
		  }
		  $_SESSION['devices'] = $devices;
		}		
	}
	else 	
		$error =  "Wrong username or password<br>";	
}

if(!isset($_SESSION['userId']))
{
?>
<html>
<title>Device Service Manager | SENSE</title>
<link rel="shortcut icon" href="http://www.sense-os.nl/sites/default/files/sense_os_favicon.png" type="image/x-icon">
<link href="style.css" rel="stylesheet" type="text/css" media="screen" />
</head>
<body>
<?php
  print  "<h1>Device Service Manager login</h1><br>
  $error
  <form action=\"login\" method=\"POST\">
  <table>
  <tr><td>Email:</td><td> <input name=\"email\" value=\"\"></td></tr><br>
  <tr><td>Password:</td><td><input name=\"password\" type=password value=\"\"></td></tr>
  <tr><td><input type=\"submit\" value=\"login\" name=\"submit\"></td></tr></form>
  </table>";
?>
</body>
</html>
<?php
}
else
{
  include_once("deviceServiceManagerRPC.php");  
}
?>
