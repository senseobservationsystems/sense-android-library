<?php 
include_once("../API/db_connect_and_login.php");

//$lat = $_REQUEST['latitude'];
//$long = $_REQUEST['longitude'];
$ds_id		= $_REQUEST['ds_id'];
$ds_type	= $_REQUEST['ds_type'];
$device_id	= $_REQUEST['device_id'];
$noPitch	= $_REQUEST['noPitch'];
$deviceCheck = 0;
if(isset($_SESSION['devices']))
foreach($_SESSION['devices'] as $device)
{
    if($device == $device_id)
      $deviceCheck = 1;
}
if($deviceCheck == 0)
  die("Wrong device id.");
// get from the database the long lat
$positionSensorID = 14;
$sql = "select * from sensor_type where name='position'";
$result = mysql_query($sql);	
if(!$result)			 
    die("error"); 
$positionSensorStr = "";
while( $row = mysql_fetch_assoc($result))
{
    if(strlen($positionSensorStr) > 0)
      $positionSensorStr .= " or ";
      $positionSensorID = $row['id'];
    $positionSensorStr .= " sensor_type ='$positionSensorID' ";
}
$sql = "select * from sensor_data where device_id='$device_id' and ( $positionSensorStr ) ORDER BY id DESC LIMIT 0,1";
$result = mysql_query($sql);	
if(!$result)			 
    die("error"); 
  $count	= mysql_num_rows($result);
  if($count == 0 )
    die("No Position data found\n");
  $row = mysql_fetch_assoc($result);
  $longLatValue = $row['sensor_value'];	

// get the values out of json
$longLatJson = json_decode($longLatValue);
while(list($key, $value) = each($longLatJson))
  $$key = $value;

//echo "long lat: $longLatValue\n" ;
// find orientation
$orientationID = 14;
$sql = "select * from sensor_type where name='orientation'";
$result = mysql_query($sql);	
if(!$result)			 
    die("error"); 
$orientationStr = "";
while( $row = mysql_fetch_assoc($result))
{
    if(strlen($orientationStr) > 0)
      $orientationStr .= " or ";
      $orientationID = $row['id'];
    $orientationStr .= " sensor_type ='$orientationID' ";
}
$sql = "select * from sensor_data where device_id='$device_id' and ( $orientationStr ) ORDER BY id DESC LIMIT 0,1";
$result = mysql_query($sql);	
if(!$result)			 
    die("error");  
$count	= mysql_num_rows($result);
if($count > 0)
{
  $row = mysql_fetch_assoc($result);
  $orientationValue = $row['sensor_value'];	
  $orientationJson = json_decode($orientationValue);
  while(list($key, $value) = each($orientationJson))
    $$key = $value;
  $yaw = $azimuth;
}
else
{
  $pitch = 0;
  $yaw = 0;
}
//<meta http-equiv="refresh" content="5">
if($_REQUEST['getData'])
{
echo "<?xml version=\"1.0\" ?><root>
	<location> 
		  <longitude>$longitude</longitude>
		  <latitude>$latitude</latitude>
	</location>
	<orientation>
		    <pitch>$pitch</pitch>
		    <yaw>$yaw</yaw>
	</orientation>	
</root>";
}
else
{
?>
<html>
<head>
<!--for demo.almende.com
 <script src="http://maps.google.com/maps?file=api&amp;v=2&amp;sensor=true&amp;key=ABQIAAAAhemGaS5PmIEPzvvfBHoE1RQHPyg5pH9x-vCl7Mtg7EdesbbpMRTozQtiOdyRBHcUwf7f5Sdsd-wW1Q" type="text/javascript"></script>-->
<script src="http://maps.google.com/maps?file=api&amp;v=2&amp;sensor=true&amp;key=ABQIAAAAhemGaS5PmIEPzvvfBHoE1RT1SJwgBNv1SjkOUMSX9xTcLZDcDBRl1p2f9slv3GObUzTn4MN_PuG-3A" type="text/javascript"></script>
<script type="text/javascript">
var map;
var myPano;   
var panoClient;
var nextPanoId;
var myPOV;
var usePitch = new Boolean(true);
var autoUpdate = new Boolean(true);
var useStreetView = new Boolean(true);
var theAlamo;
function initialize() {
	    panoClient = new GStreetviewClient();     
	    var theAlamo = new GLatLng(<?php echo "$latitude,$longitude"; ?>);
            map = new GMap2(document.getElementById("map_canvas"));
            map.setCenter(theAlamo, 13);
            map.setUIToDefault();
	    map.addOverlay(new GMarker(theAlamo));
	
	//  var fenwayPark = new GLatLng(51.89878,4.488473);
      if(useStreetView)
      {
	myPOV = {<?php echo "yaw:$yaw, pitch:$pitch";?>};
	panoramaOptions = { latlng:theAlamo, pov:myPOV};
	myPano = new GStreetviewPanorama(document.getElementById("pano"), panoramaOptions);
	GEvent.addListener(myPano, "error", handleNoFlash);
	panoClient.getNearestPanorama(theAlamo, showPanoData);
      }
}
function loadNewData(yawVal, pitchVal, longitudeVal, latitudeVal)
{
if(!usePitch)
  pitchVal = 0;
<?php
if($noPitch==1)
    echo  "var myPOV2 = {yaw:parseInt(yawVal,pitch:0)};\n";
else
  echo  "var myPOV2 = {yaw:parseInt(yawVal), pitch:parseInt(pitchVal)};\n";
?>
  var theAlamo2 = new GLatLng(latitudeVal, longitudeVal);  
  if(!theAlamo2.equals(theAlamo))
  {
      theAlamo = theAlamo2;
      map.setCenter(theAlamo, 13);
      map.setUIToDefault();
      map.addOverlay(new GMarker(theAlamo));      
  }  
    if(!theAlamo2.equals(theAlamo) || myPOV2['pitch'] != myPOV['pitch'] || myPOV2['yaw'] != myPOV['yaw'])
    {
      myPOV = myPOV2;
      myPano.setLocationAndPOV(theAlamo2, myPOV);    
      panoClient.getNearestPanorama(theAlamo2, showPanoData);
    }
}
function showPanoData(panoData) {
 
//   nextPanoId = panoData.links[[0[].panoId;
//   var displayString = [[
//     "Panorama ID: " + panoData.location.panoId,
//     "LatLng: " + panoData.location.latlng,
//     "Copyright: " + panoData.copyright,
//     "Description: " + panoData.location.description,
//     "Next Pano ID: " + panoData.links[[0[].panoId[].join("");
  map.openInfoWindowHtml(panoData.location.latlng, displayString);
  myPano.setLocationAndPOV(panoData.location.latlng,myPOV);
}
   var http_request = false;
   function makeRequest(url, parameters) {
      http_request = false;
      if (window.XMLHttpRequest) { // Mozilla, Safari,...
         http_request = new XMLHttpRequest();
         if (http_request.overrideMimeType) {
            http_request.overrideMimeType('text/xml');
         }
      } else if (window.ActiveXObject) { // IE
         try {
            http_request = new ActiveXObject("Msxml2.XMLHTTP");
         } catch (e) {
            try {
               http_request = new ActiveXObject("Microsoft.XMLHTTP");
            } catch (e) {}
         }
      }
      if (!http_request) {
         alert('Cannot create XMLHTTP instance');
         return false;
      }
      http_request.onreadystatechange = alertContents;
      http_request.open('GET', url + parameters, true);
      http_request.send(null);
   }

   function alertContents() {
      if (http_request.readyState == 4) {
         if (http_request.status == 200) {	    
            var xmldoc = http_request.responseXML;
	    // lat long
            var locationNode = xmldoc.documentElement.getElementsByTagName("location");

	    var latitude = locationNode[0].getElementsByTagName("latitude");	    
	    var latVal =  latitude[0].firstChild.nodeValue;

	    var longitude = locationNode[0].getElementsByTagName("longitude");
	    var longVal =  longitude[0].firstChild.nodeValue;
  
	    //orientation
	    var orientationNode = xmldoc.documentElement.getElementsByTagName("orientation");

	    var pitch = orientationNode[0].getElementsByTagName("pitch");	    
	    var pitchVal =  pitch[0].firstChild.nodeValue;

	    var yaw = orientationNode[0].getElementsByTagName("yaw");
	    var yawVal =  yaw[0].firstChild.nodeValue; 
	    loadNewData(yawVal, pitchVal, longVal, latVal);	   
         } else {
            alert('There was a problem with the request.'+http_request.status);
         }
      }
   }
   function do_xml() {
      if(autoUpdate)
	setTimeout("do_xml()",2000);   
      makeRequest('gps_service.php', '?getData=1&device_id=<?php echo $device_id;?>');
   } 
function togglePitch()
{
  usePitch = !usePitch;
  if(usePitch)
    document.getElementById("buttonPitch").value = "Disable Pitch";
  else
    document.getElementById("buttonPitch").value = "Enable Pitch";
}

function toggleUpdate()
{
  autoUpdate = !autoUpdate;
  if(autoUpdate)
  {
    do_xml();
    document.getElementById("buttonUpdate").value = "Disable Update";
  }
  else
    document.getElementById("buttonUpdate").value = "Enable Update";
}

function toggleStreetView()
{
  useStreetView = !useStreetView;
  if(useStreetView)
  {   
    document.getElementById("buttonStreetView").value = "Disable Street View";
    document.getElementById("map_canvas").style.height = "25%";
    document.getElementById("map_canvas").style.width = "100%";
    document.getElementById("pano").style.display = "block";    
    initialize(); 
  }
  else
  {
    document.getElementById("buttonStreetView").value = "Enable Street View";
    document.getElementById("map_canvas").style.height = "97%";
    document.getElementById("map_canvas").style.width = "100%";    
    document.getElementById("pano").style.display = "none";   
     initialize(); 
  }
}
do_xml();
</script>

</head>
<body onload="initialize()">
<input type="button" id="buttonPitch" value="Disable Pitch" 
   onclick="javascript:togglePitch();"><input type="button" id="buttonUpdate" value="Disable Update" onclick="javascript:toggleUpdate();"><input type="button" id="buttonStreetView" value="Disable Street View" onclick="javascript:toggleStreetView();">  
  <div id="map_canvas" style="width: 100%; height: 25%"></div>
<div id="pano" style="width: 100%; height: 72%"></div>
</body>
</html>
<?php 
}
?>
