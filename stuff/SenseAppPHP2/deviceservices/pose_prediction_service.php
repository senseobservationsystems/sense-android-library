<?php
$ds_id		= $_REQUEST['ds_id'];
$ds_type	= $_REQUEST['ds_type'];
$pose		= $_REQUEST['pose'];
$device_id	= $_REQUEST['device_id'];
$pictureArray = array("sit"=>"http://data.sense-os.nl/commonsense/deviceservices/sit.png","stand"=>"http://data.sense-os.nl/commonsense/deviceservices/stand.png","lie"=>"http://data.sense-os.nl/commonsense/deviceservices/lie.png","dance" => "http://data.sense-os.nl/commonsense/deviceservices/dance.png", "walk"=>"http://data.sense-os.nl/commonsense/deviceservices/walk.png");
include_once("../API/db_connect_and_login.php");
//  include_once("../db_only_connect.php");
  // get the last pose value from the databse
  $sql = "select * from sensor_type where name='$ds_type' and device_type='$ds_id'";
  $result = mysql_query($sql);	    
  if(!$result)			 
    die("error"); 
  $count	= mysql_num_rows($result);
  if($count == 0 )
    return;
  $row = mysql_fetch_assoc($result);
  $sensorTypeID = $row['id'];	
  $sql = "select * from sensor_data where sensor_type='$sensorTypeID' ORDER BY id DESC LIMIT 1";
  $result = mysql_query($sql);	
 if(!$result)			 
    die("error"); 
 $count	= mysql_num_rows($result);
  if($count == 0 )
    return;
  $row = mysql_fetch_assoc($result);
  $sensorValue = $row['sensor_value'];	 

    if(!isset($_SESSION[$ds_type.$sensorValue]))
    {
	if($pictureArray[$sensorValue])
	{
		$imgResult = $pictureArray[$sensorValue];
	}
	else
	      $imgResult = googleImageResults($sensorValue,1);
      if(strlen($imgResult) > 20)
	$_SESSION[$ds_type.$sensorValue] = $imgResult;
    }
    else
      $imgResult = $_SESSION[$ds_type.$sensorValue];
 
    print "<html><head><META HTTP-EQUIV=Refresh CONTENT=\"1\"></head><body>";
    print "<b>".$sensorValue."</b><br>";  
    print "<img src=\"".$imgResult."\" />";
    print "</body></html>";


function googleImageResults($query, $page=1, $safe='on', $dc="images.google.com"){
    $page--;
    $perpage = 21;
    $url=sprintf("http://%s/images?q=%s&gbv=2&start=%d&hl=en&ie=UTF-8&safe=%s&sa=N",
        $dc,urlencode($query),$page*$perpage,$safe);
 
     $html=file_get_contents($url);   
    
    $searchString = "gstatic.com/images?q=";
    $strMid = substr($html, strpos($html, $searchString));
    $strMid = substr($strMid, strpos($strMid, "http://"));
    $posEnd = strpos($strMid, "\"", 0);

    $imgUrl = substr($strMid, 0, $posEnd);      
    return $imgUrl;
}
?>
