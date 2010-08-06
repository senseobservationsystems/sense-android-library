<?php
	include("../db_connect.php");
	include("xmlrpc/lib/xmlrpc.inc");
	$userName 	= $_SESSION['userName'];
?>
<html>
<head><title>xmlrpc</title></head>
<body>
<h1>Device Service Manager RPC demo</h1>
<?php

print "<table width=100%><tr><td>You are here: <a href=\"login.php\">Home</a></td><td>hi $userName - <a href=\"login.php?logout=1\">logout</td></tr></table><br><br>";

//print the devices
include_once("devicesAndServices.php");

?>
</body>
</html>
