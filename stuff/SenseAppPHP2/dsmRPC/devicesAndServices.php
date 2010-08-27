<?php
//if(isset($_SESSION['devices']))
//{


if(!isset($HTTP_POST_VARS) && isset($_POST))
{
	$HTTP_POST_VARS = $_POST;
}

// Display result
if(isset($HTTP_POST_VARS["submit"]))
{  
      $device_id = $HTTP_POST_VARS["device_id"];
      $ds_id = $HTTP_POST_VARS["ds_id"];
      $ds_type = $HTTP_POST_VARS["ds_type"];

      if($HTTP_POST_VARS["submit"] == "connect")
      {
	$method = "dsm.ConnectToService";
	if(strpos($ds_id, ".") == 0)
	  $ds_id = $device_id.".".$ds_id;
      }
      if($HTTP_POST_VARS["submit"] == "disconnect")
      {
	  $method = "dsm.DisconnectFromService";
      }
     		
      
      $index = "arg0";
      $i = 0;
     
      while(isset($HTTP_POST_VARS[$index]))
      {		 
	  $arrayArgs[0] = new xmlrpcval($device_id);
	  $arrayArgs[1] = new xmlrpcval($HTTP_POST_VARS[$index]);	
	  $arrayArgs[2] = new xmlrpcval($ds_id);
	  $arrayArgs[3] = new xmlrpcval($ds_type);
	  
	  $data_type = $HTTP_POST_VARS[$index];
	  ++$i;
	  $index = "arg$i";
	  // send the data type
				
	  $f=new xmlrpcmsg($method,$arrayArgs);	
	  
	  //print "<pre>Sending the following request:\n\n" . htmlentities($f->serialize()) . "\n\nDebug info of server data follows...\n\n";
	  $c=new xmlrpc_client("/server.php", "localhost", 8000);
	  $c->setDebug(0);
	  $r=&$c->send($f);
	  if(!$r->faultCode())
	  {
		  $v=$r->value();		  
		  $result = "ok";	
	  }
	  else
	  {
		  print "An error occurred: ";
		  print "Code: " . htmlspecialchars($r->faultCode())
			  . " Reason: '" . htmlspecialchars($r->faultString()) . "'</pre><br/>";
	  }      
	}
      // register the service with the proxy
      if($HTTP_POST_VARS["submit"] == "connect" && $result == "ok")
      {
	    $url = "http://demo.almende.com/commonSense2/deviceservices/$ds_type.php";
	    $dsRegistryServer = "http://demo.almende.com:8080/registerdeviceservice?ds_id=".$ds_id."&ds_type=".$ds_type."&virtual=true&url=".$url;
	    file_get_contents($dsRegistryServer);
	   
      }
     if($HTTP_POST_VARS["submit"] == "disconnect" && $result == "ok")
      {
	    $url = "http://demo.almende.com/commonSense2/deviceservices/$ds_type.php";
	    $dsRegistryServer = "http://demo.almende.com:8080/unregisterdeviceservice?ds_id=".$ds_id."&ds_type=".$ds_type."&virtual=true&url=".$url;
	    file_get_contents($dsRegistryServer);
	  
      }
      
}	
print "<table width=100%><tr><td>".activeDeviceServices()."</td><td>".deviceServices()."</td></tr></table>";

function deviceServices()
{
    $devices	= $_SESSION['devices'];	      
    echo "<h2>Connect a device to a service</h2>";
    print "<table border=1><tr><td>";
    print  "<table><tr><td>Device</td><td>Service Name</td><td>Service ID</td></tr>";
    $deviceServices;

    foreach($devices as $devices_Id)
    {		  
  
	$g=new xmlrpcmsg('dsm.GetDeviceServices', array(new xmlrpcval($devices_Id)));
	// print "<pre>Sending the following request:\n\n" . htmlentities($g->serialize()) . "\n\nDebug info of server data follows...\n\n";
	  $client=new xmlrpc_client("/server.php", "localhost", 8000);
	  $client->setDebug(0);
	  $result=&$client->send($g);
	  if(!$result->faultCode())
	  { 
	    // A jason object is returned
	    $sensorValue=$result->value()->scalarval();
	   // print $sensorValue;
	    $jsonObject = json_decode($sensorValue, True);
	    //print $jsonObject;
	    $serviceArray = $jsonObject['deviceServices'];
	     $deviceService[$devices_Id] = $serviceArray;
	    $tmpDevices = array(0);
	    $args = "";
	    $argsCnt = 0;
	    $outputForm =  "<form action=\"deviceServiceManagerRPC.php\" method=\"POST\">
			    <input name=\"device_id\" type=hidden value=\"$devices_Id\">
			     <tr><td><a href='../deviceservices/gps_service.php?device_id=$devices_Id'>$devices_Id</a></td><td><select name=\"ds_type\">";
	    while(list($key, $value) = each($serviceArray))
	    { 	      
	     
	      if(isset($value['ds_type']))
	      {
		  $service = $value['ds_type'];
		  $data_type = $value['data_type'];
		  $args .= "<input type=hidden name=\"arg$argsCnt\" value=\"$data_type\">";
		  $argsCnt++;
		if(!isset($tmpDevices[$service]))
		{
		   
		    $tmpDevices[$service] = $value['data_type'];
		    $outputForm .= "<option value=\"$service\">$service</option>";	
		}
	      }
	    }	   
	    print $outputForm."</select>$args</td>
	   <td> <input name=\"ds_id\" value=\"\"></td>
	     <td><input name=\"submit\" type=submit value=\"connect\"></td></tr></form>";
	  }
	  else
	  {
		  print "An error occurred: ";
		  print "Code: " . htmlspecialchars($result->faultCode())
			  . " Reason: '" . htmlspecialchars($result->faultString()) . "'</pre><br/>";
	  }
    }
print "</table></tr></td></table>";
}
function activeDeviceServices()
{  
    $_SESSION['deviceService'] = $deviceServices;
 
    $devices	= $_SESSION['devices'];	      
    print "<h2>Active Services</h2>";
    print "<table border=3 CELLPADDING=0 CELLSPACING=0><tr><td>Device_id</td><td>Service_ID</td><td>Name</td></tr>";
    $deviceServices;    
    $argsCnt = array();
    $tmpDevices = array();	
    foreach($devices as $devices_Id)
    {		  
	$g=new xmlrpcmsg('dsm.GetActiveServices', array(new xmlrpcval($devices_Id)));
	  // print "<pre>Sending the following request:\n\n" . htmlentities($g->serialize()) . "\n\nDebug info of server data follows...\n\n";
	  $client=new xmlrpc_client("/server.php", "localhost", 8000);
	  $client->setDebug(0);
	  $result=&$client->send($g);
	  if(!$result->faultCode())
	  { 
	    // A jason object is returned
	    $sensorValue=$result->value()->scalarval();
	   // print $sensorValue;
	    $jsonObject = json_decode($sensorValue, True);
	    //print $jsonObject;
	    $serviceArray = $jsonObject['activeDeviceServices'];
	    $deviceService[$devices_Id] = $serviceArray;	    	    
	    
	    while(list($key, $value) = each($serviceArray))
	    { 	      
	      if(isset($value['ds_type']))
	      {
		  $service = $value['ds_type'];
		  $ds_id = $value['ds_id'];	  
		  $data_type = $value['data_type'];
		  
		if(!isset($tmpDevices[$service.$ds_id.$devices_Id]))
		{		  
		    $argCnt[$service.$ds_id.$devices_Id] = 0;
		    $tmpDevices[$service.$ds_id.$devices_Id] = "<tr><td>$devices_Id</td><td><a href=\"deviceService.php?ds_id=$ds_id&ds_type=$service\">$ds_id</a></td><td><a href=\"deviceService.php?ds_id=$ds_id&ds_type=$service\">$service</a></td>
		    <form action=\"deviceServiceManagerRPC.php\" method=\"POST\">			      
			    <input name=\"device_id\" type=hidden value=\"$devices_Id\">
			      <input name=\"ds_type\" type=hidden value=\"$service\">
			      <input type=hidden name=\"arg".$argCnt[$service.$ds_id.$devices_Id]."\" value=\"$data_type\">
			      <input name=\"ds_id\" type=hidden value=\"$ds_id\">";
		   
		}
		else		
		   $tmpDevices[$service.$ds_id.$devices_Id] .="<input type=hidden name=\"arg".$argCnt[$service.$ds_id.$devices_Id]."\" value=\"$data_type\">";	
		  $argCnt[$service.$ds_id.$devices_Id] = $argCnt[$service.$ds_id.$devices_Id]+1;
	      }
	    }	   
	  }
	  else
	  {
		  print "An error occurred: ";
		  print "Code: " . htmlspecialchars($result->faultCode())
			  . " Reason: '" . htmlspecialchars($result->faultString()) . "'</pre><br/>";
	  }

  }
  while(list($key, $value) = each($tmpDevices))
  { 	
      print $value."<td><input name=\"submit\" type=submit value=\"disconnect\"></form></td></tr>";
  }
  print "</table>";
}

?>