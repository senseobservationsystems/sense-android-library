<?php
	include("../db_connect.php");
	include("xmlrpc/lib/xmlrpc.inc");
	$ds_id		= $_REQUEST['ds_id']; 
	$ds_type	= $_REQUEST['ds_type'];
	$userName 	= $_SESSION['userName'];
?>
<html>

<head><title>Device Service Panel</title>
<link rel="shortcut icon" href="http://www.sense-os.nl/sites/default/files/sense_os_favicon.png" type="image/x-icon">
<link href="style.css" rel="stylesheet" type="text/css" media="screen" />
</head>
<body>
<h1>Device Service Panel: <?php echo $ds_id." ".$ds_type;?></h1>
<?php
//print "<a href=\"login.php?logout=1\" >logout</a><br>";
//print the devices

print "<hr><table width=100%><tr><td>You are here: <a href=\"login\">Home</a>-<a href=\"deviceService?ds_id=$ds_id&ds_type=$ds_type\">DeviceService</a></td><td>hi <a href=\"sensorDataApi\">$userName</a> - <a href=\"login?logout=1\">logout</td></tr></table><hr><br><br>";

print "<table width=100%><tr><td>";

  // Play nice to PHP 5 installations with REGISTER_LONG_ARRAYS off
if(!isset($HTTP_POST_VARS) && isset($_POST))
{
	$HTTP_POST_VARS = $_POST;
}

// Display result
if(isset($HTTP_POST_VARS["method"]))
{
	$method=$HTTP_POST_VARS["method"];
	$index = "arg0";
	$i = 0;
		
	while(isset($HTTP_POST_VARS[$index]))
	{			 
	  $arrayArgs[$i] = new xmlrpcval($HTTP_POST_VARS[$index]);
	  ++$i;
	  $index = "arg$i";
	}		
	
	  $f=new xmlrpcmsg($method,$arrayArgs);	
	
	//print "<pre>Sending the following request:\n\n" . htmlentities($f->serialize()) . "\n\nDebug info of server data follows...\n\n";
	$c=new xmlrpc_client("/server.php", "localhost", 8000);
	$c->setDebug(0);
	$r=&$c->send($f);
	if(!$r->faultCode())
	{
		$v=$r->value();
		print "result:". htmlspecialchars($v->scalarval()) . "<br><br>";			
	}
	else
	{
		print "An error occurred: ";
		print "Code: " . htmlspecialchars($r->faultCode())
			. " Reason: '" . htmlspecialchars($r->faultString()) . "'</pre><br/>";
	}
}	

// first query for a list of methods
$f=new xmlrpcmsg('system.listMethods', array(null));
//print "<pre>Sending the following request:\n\n" . htmlentities($f->serialize()) . "\n\nDebug info of server data follows...\n\n";
$c=new xmlrpc_client("/server.php", "localhost", 8000);
$c->setDebug(0);
$r=&$c->send($f);
if(!$r->faultCode())
{
    // First list the DSM methods with the DSM prefix
    $val=$r->value();
    // iterating over values of an array object
    for ($i = 0; $i < $val->arraySize(); $i++)
    {
      $v = $val->arrayMem($i);
      $methodName = $v->scalarval();	    
      $pos = strpos($methodName, "_");
      $pos = strpos($methodName, ".", $pos);
      $devID_Service = substr($methodName, 0, $pos);		      
      if($devID_Service != $ds_id."_".$ds_type)       
	  continue;	    
    $legalMethod = 'false';	      
    if(isset($_SESSION['devices']))
    {
	$devices	= $_SESSION['devices'];	      
	foreach($devices as $devices_Id)
	{		 
	  $pos = strpos($devID_Service, ".");
	  $ID = substr($devID_Service, 0, $pos); 
	  if($devices_Id == $ID)
	    $legalMethod = 'true';
	}
    }
  if($legalMethod == 'false')
  continue;
	
	    

// Find for each device the device service
  // echo "Method:".$methodName."<br>";
    //if(strrpos($methodName, "system") != 0)
      //displayMethod($methodName);
    //{
$g=new xmlrpcmsg('system.methodHelp', array(new xmlrpcval($methodName)));
// print "<pre>Sending the following request:\n\n" . htmlentities($g->serialize()) . "\n\nDebug info of server data follows...\n\n";
  $client=new xmlrpc_client("/server.php", "localhost", 8000);
  $client->setDebug(0);
  $result=&$client->send($g);
  if(!$result->faultCode())
  {    
      // Get method help
    $value=$result->value()->scalarval();			
    if(substr_count($value,"()") == 1 || substr_count($value,"(") == 0)
    {
	$methodCall=new xmlrpcmsg($methodName, array(null));
      // print "<pre>Sending the following request:\n\n" . htmlentities($g->serialize()) . "\n\nDebug info of server data follows...\n\n";
	$clientMethodCall=new xmlrpc_client("/server.php", "localhost", 8000);
	$clientMethodCall->setDebug(0);
	$resultMethodCall=&$clientMethodCall->send($methodCall);
	if(!$resultMethodCall->faultCode())
	{   		      
	    
	    $methodArr = split(" ", $value);				
	    $methodName = $methodArr[1];
	    $valueMethodCall=$resultMethodCall->value()->scalarval();
	    print "<b>".substr($methodName, 0, strlen($methodName)-2).": </b>";
	    if($methodArr[0] == "json")
	    {
		  $jsonObject = json_decode($valueMethodCall, True);
		  printJson($jsonObject);				     
	    }
	    else
	      print $valueMethodCall."<br>";
	}else
	{
	  print "An error occurred: Code: " . htmlspecialchars($resultMethodCall->faultCode()). " Reason: '" . htmlspecialchars($resultMethodCall>faultString()) . "'</pre><br/>";
	}
    }			    
    else
    {
      if(!$paramMethods)
	print $paramMethods = "<br>";
      print $value."\n";
      $parameterCnt = substr_count($value,",")+1;			
      print "<form action=\"deviceService\" method=\"POST\">
	      <input name=\"method\" type=hidden value=\"" . $methodName . "\">";
	// iterating over values of an array object
      for ($y = 0; $y < $parameterCnt; $y++)
      {			
	print "<input name=\"arg$y\" value=\"\">";		   
      }     
      print "<input type=hidden name=ds_id value=$ds_id>";
      print "<input type=hidden name=ds_type value=$ds_type>";
      print " <input type=\"submit\" value=\"request\" name=\"submit\"></form>";
    }
  }
  else
  {
	  print "An error occurred: ";
	  print "Code: " . htmlspecialchars($result->faultCode())
		  . " Reason: '" . htmlspecialchars($result->faultString()) . "'</pre><br/>";
  }
  //  }
  }	
}
else
{
	print "An error occurred: ";
	print "Code: " . htmlspecialchars($r->faultCode())
		. " Reason: '" . htmlspecialchars($r->faultString()) . "'</pre><br/>";
}

function printJson($jsonObject)
{
while(list($key, $value) = each($jsonObject))
  { 	print $key.": ";   
      if(is_array($value))
      {
	print "<br>";
	printJson($value);
      }
      else
	print $value."<br>";
  }
}
print "</td><td width=80% height=100%><iframe width=90% height=100% src=\"../deviceservices/$ds_type.php?viewService=1&ds_type=$ds_type&ds_id=$ds_id\"></iframe></td></tr></table>";
?>
</body>
</html>
