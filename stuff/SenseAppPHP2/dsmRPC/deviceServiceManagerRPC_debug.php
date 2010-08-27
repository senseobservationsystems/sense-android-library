<?php
	include("../db_connect.php");
	include("xmlrpc/lib/xmlrpc.inc");
?>
<html>
<head><title>xmlrpc</title></head>
<body>
<h1>Device Service Manager RPC demo</h1>
<?php
print "<a href=\"login.php?logout=1\" >logout</a><br>";
//print the devices
if(isset($_SESSION['devices']))
{
    $devices	= $_SESSION['devices'];	      
    echo "Your devices:<br>";
    foreach($devices as $devices_Id)
    {		  
      echo $devices_Id."<br>";
    }
  }


// Play nice to PHP 5 installations with REGISTER_LONG_ARRAYS off
if(!isset($HTTP_POST_VARS) && isset($_POST))
{
	$HTTP_POST_VARS = $_POST;
}

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
			print "result:". htmlspecialchars($v->scalarval()) . "<br/>";			
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
	      $devId = substr($methodName, 0,$pos);	      
	     if((substr($methodName,0,6) == "system" || substr($methodName, 0,3) == "dsm") && $methodName != "system.multicall")        
		  $legalMethod = 'true';	      
	    else
	      	  $legalMethod = 'false';
	    if(isset($_SESSION['devices']))
	    {
		$devices	= $_SESSION['devices'];	      
		foreach($devices as $devices_Id)
		{		  
		  if($devices_Id == $devId)
		    $legalMethod = 'true';
		}
	      }
	    if($legalMethod != 'true')
	      continue;
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
			print $value."\n";
			if(substr_count($value,"()") == 1 || substr_count($value,"(") == 0)
			    $parameterCnt = 0;
			else
			  $parameterCnt = substr_count($value,",")+1;			
			print "<form action=\"deviceServiceManagerRPC.php\" method=\"POST\">
				<input name=\"method\" type=hidden value=\"" . $methodName . "\">";
			  // iterating over values of an array object
			for ($y = 0; $y < $parameterCnt; $y++)
			{			
			  print "<input name=\"arg$y\" value=\"\">";		   
			}     
			print " <input type=\"submit\" value=\"request\" name=\"submit\"></form>";
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


	
?>
</body>
</html>
