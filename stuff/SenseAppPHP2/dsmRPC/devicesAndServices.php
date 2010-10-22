<?php

// find all the sensor types of a device
if(!isset($_SESSION['sensorDevices']) || isset($_REQUEST['reloadSensors']))
{
  $devices	= $_SESSION['devices'];	
   foreach($devices as $devices_Id)
    {	
      $sql = "select * from tags where parent_id='$devices_Id' and type='sensor_type'";
      $result	= mysql_query($sql);	
      $searchStr = "";
      if($result)   
      {
	  while( $row = mysql_fetch_assoc($result))
	  {
	    if(strlen($searchStr) > 0)
	      $searchStr .= " or ";
	    $searchStr .= " id='".$row['tagged_id']."'";	    
	  }
      }
      $sql = "select * from sensor_type where $searchStr";
      $result	= mysql_query($sql);	
      
      if($result)   
      {
	  while( $row = mysql_fetch_assoc($result))
	  {	    
	    if(strlen($row['device_type']) > 0)
	      $postFix = " (".$row['device_type'].")";	
	    else
	      $postFix = "";
	    $sensorDevices[$devices_Id.".".$row['id']] = $row['name'].$postFix;
	  }
      }
    }
  $_SESSION['sensorDevices'] = $sensorDevices;
}

if(!isset($HTTP_POST_VARS) && isset($_POST))
{
	$HTTP_POST_VARS = $_POST;
}
// change the status of gtalk external service
if(isset($HTTP_POST_VARS["request"]) && $HTTP_POST_VARS["request"] == "gtalk")
{
    $ds_id = $HTTP_POST_VARS["ds_id"];
    $ds_type = $HTTP_POST_VARS["ds_type"];
    $gtalk = $HTTP_POST_VARS["gtalkStatus"];
    $sensorType = -1;  
    if($gtalk == "on")    
      $sensorType = $ds_id;

    $sql	= "update external_services set sensor_type='$sensorType' where user_id='".$_SESSION['userId']."' and service='gtalk'";
    $result	= mysql_query($sql);	
    if(!$result)    
        $msgStr = "<br><b><font color=\"red\">Error: set your Gtalk credentials in your profile.</font></b><br>";   
}

// get the external service values
$sql = "select * from external_services where user_id='".$_SESSION['userId']."' and service='gtalk'";
$gtalkStatus;
$result	= mysql_query($sql);	

if($result) 
{
  if(mysql_num_rows($result) == 0 && $gtalk == "on")
  {
    echo $msgStr = "<b><font color=\"red\">Error: set your Gtalk credentials in your profile.</font></b><br><br>";   
  }
  while ($row = mysql_fetch_assoc($result)) 
  {
    $lable = $row['sensor_type'];
    $gtalkStatus[$lable] = "checked";          
  }
  $_SESSION['gtalkStatus'] = $gtalkStatus;
}


// Display result
if((isset($HTTP_POST_VARS["request"]) && $HTTP_POST_VARS["request"] == "disconnect") || isset($HTTP_POST_VARS["submit"]))
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
      if($HTTP_POST_VARS["request"] == "disconnect")
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
//       // register the service with the proxy
//       if($HTTP_POST_VARS["submit"] == "connect" && $result == "ok")
//       {
// 	    $url = "http://demo.almende.com/commonSense2/dsm_sensor_data_add.php";
// 	    $dsRegistryServer = "http://demo.almende.com:8080/registerdeviceservice?ds_id=".$ds_id."&ds_type=".$ds_type."&virtual=true&url=".$url;
// 	    file_get_contents($dsRegistryServer);
// 	   
//       }
//      if($HTTP_POST_VARS["request"] == "disconnect" && $result == "ok")
//       {
// 	    $url = "http://demo.almende.com/commonSense2/dsm_sensor_data_add.php";
// 	    $dsRegistryServer = "http://demo.almende.com:8080/unregisterdeviceservice?ds_id=".$ds_id."&ds_type=".$ds_type."&virtual=true&url=".$url;
// 	    file_get_contents($dsRegistryServer);
// 	  
//       }
      
}	
print activeDeviceServices()."<br>".deviceServices();

function deviceServices()
{
    $devices		= $_SESSION['devices'];	
    $sensorDevices 	= $_SESSION['sensorDevices'];
    echo "<h3>Connect a device to a service</h3> <a href=\"login?reloadSensors=1\">reload sensors</a>";
    print "<table style=\"border-left: black 1px solid; 
border-right: black 1px solid; border-top: black 1px solid; border-bottom: black 1px solid\"><tr><td>";
    print  "<table><tr><td><FONT SIZE=2>Sensor</td><td><FONT SIZE=2>Service type</td><td><FONT SIZE=2>Service ID</td></tr>";
    $deviceServices;
if(sizeof($sensorDevices) > 0)
    $lastDevice_id = "";
    foreach($sensorDevices as $devices_Id => $sensorName) // combination of device_id.sensor_type
    {		  
	  $pos = strpos($devices_Id, ".");
	if($pos !== false)
	{
	    $sensorType = substr($devices_Id, $pos+1);
	    $d_ID	= substr($devices_Id, 0, $pos); // device id as in the database
	}
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
	    $outputForm =  "<form action=\"deviceServiceManagerRPC\" method=\"POST\">
			    <input name=\"device_id\" type=hidden value=\"$devices_Id\">";		
	     if($lastDevice_id!=$d_ID)
	    {
	        $outputForm .= "<tr><td><a href='../deviceservices/gps_service?device_id=$d_ID'>Device: $d_ID</a></td></tr>";
		$lastDevice_id = $d_ID;
	    }
	    $outputForm .=    "<tr><td>$sensorName</td><td><select name=\"ds_type\">";
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
print "</table></tr></td></table><br>";
}
function activeDeviceServices()
{  
    $_SESSION['deviceService'] = $deviceServices;
 
    $devices		= $_SESSION['devices'];	         
    $gtalkStatus 	= $_SESSION['gtalkStatus'];
    $sensorDevices 	= $_SESSION['sensorDevices'];
    $deviceServices;    
    $argsCnt = array();
    $tmpDevices = array();	
if(sizeof($sensorDevices) > 0)
    foreach($sensorDevices as $devices_Id => $sensorName)
    {		  
	$pos = strpos($devices_Id, ".");
	if($pos !== false)
	{
	    $sensorType = substr($devices_Id, $pos+1);
	    $d_ID	= substr($devices_Id, 0, $pos); // device id as in the database
	}
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
		    $tmpDevices[$service.$ds_id.$devices_Id] = "<tr><td>$d_ID</td><td>$sensorName</td><td><a href=\"deviceService?ds_id=$ds_id&ds_type=$service\">$ds_id</a></td><td><a href=\"deviceService?ds_id=$ds_id&ds_type=$service\">$service</a></td>
		    <form name='$service.$ds_id' action=\"deviceServiceManagerRPC\" method=\"POST\">			      
			    <input name=\"device_id\" type=hidden value=\"$devices_Id\"/>
			      <input name=\"ds_type\" type=hidden value=\"$service\"/>
			      <input type=hidden name=\"arg".$argCnt[$service.$ds_id.$devices_Id]."\" value=\"$data_type\"/>
			      <input name=\"ds_id\" type=hidden value=\"$ds_id\"/>			
			      <input name=request type=hidden value=\"disconnect\"/>
			      <td><input name=\"gtalkStatus\" type=checkbox ".$gtalkStatus[$ds_id]." onChange=\"request.value='gtalk';submit();\"/></td>";		   
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
  if(sizeof($tmpDevices) > 0)
  {
    //echo $msgStr;
    print "<h3>Active Services</h3>";
    print "<table style=\"border-left: black 1px solid; 
border-right: black 1px solid; border-top: black 1px solid; border-bottom: black 1px solid\"><tr><td><FONT SIZE=2>Device id</td><td><FONT SIZE=2>Sensor</td><td><FONT SIZE=2>Service ID</td><td><FONT SIZE=2>Type</td><td><FONT SIZE=2>Gtalk status</td></tr>";
  }
  while(list($key, $value) = each($tmpDevices))
  { 	
      print $value."<td><input type=submit value=\"disconnect\"></form></td></tr>";
  }
  print "</table><br>";
}

?>
