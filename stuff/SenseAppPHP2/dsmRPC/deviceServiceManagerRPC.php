<?php
	include_once("../db_only_connect.php");
	include_once("xmlrpc/lib/xmlrpc.inc");
	$userName 	= $_SESSION['userName'];
?>
<html>
<title>Device Service Manager | SENSE</title>
<link rel="shortcut icon" href="http://www.sense-os.nl/sites/default/files/sense_os_favicon.png" type="image/x-icon">
<link href="style.css" rel="stylesheet" type="text/css" media="screen" />
</head>
<body leftmargin="5">

<h1>Device Service Manager</h1>
<?php

print "<hr><table width=100%><tr><td>You are here: <a href=\"login\">Home</a></td><td>hi <a href=\"sensorDataApi\">$userName</a> - <a href=\"login?logout=1\">logout</td></tr></table><hr><br><br>";

//print the devices
include_once("devicesAndServices.php");
?>
</body>
</html>
