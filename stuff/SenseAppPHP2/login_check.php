<?php
include_once("db_connect.php");

function valid_login() {
$tbl_name="users"; // Table name
    $email		= $_REQUEST['email']; 
    $password		= $_REQUEST['password'];

    if ($email && $password) {
        // To protect MySQL injection (more detail about MySQL injection)
	    $email 	= stripslashes($email);
	    $password 	= stripslashes($password);
	    $email 	= mysql_real_escape_string($email);
	    $password 	= mysql_real_escape_string($password);
	    $password 	= md5($password);

	    // Check the login credentials	    
	    $sql	= "SELECT * FROM $tbl_name WHERE email='$email' and password='$password'";
	    
	    $result	= mysql_query($sql);	
	    if(!$result) {
	        // problem with SQL query with provided credentials
		    return -1;
	    }
	    $count	= mysql_num_rows($result);

	    // If result matched $email and $password, table row must be 1 row
	    if($count == 1) {
		    // Register id
		    $row = mysql_fetch_assoc($result);	
		    $userId = $row['id'];		
		    $_SESSION['userId']  = $userId;	
		    $_SESSION['email']	 =  $email;		    
		    $userName = $row['name'];
		    if(strlen($userName) == 0)
		    {
		    	$_SESSION['userName']  = $row['email'];
		    }
		    else
		    {
		    	$_SESSION['userName']  = $userName;
		    }	
		    $_SESSION['uuid']  = $row['UUID'];
		    
		    // cache the devices in the database connected to this userId
		    $sql	= "SELECT * FROM devices WHERE user_id='$userId'";
		    $result	= mysql_query($sql);	
		    if(!$result) {
		        // problem with SQL query for devices
			    return -2;
		    } else {
		      $devices;
		      while ($row = mysql_fetch_assoc($result)) {
		        $devices[$row['uuid']] = $row['id'];
		      }
		      $_SESSION['devices'] = $devices;
		    }
		    // login ok!
		    return $userId;
	    } else {
	        // more than one user found (?!)
		    return -3;
        }
    } else {
        // no username or password provided
        return -4;
    }
}
?>
