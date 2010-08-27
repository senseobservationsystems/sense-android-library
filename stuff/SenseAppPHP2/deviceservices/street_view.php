<?php
if($_REQUEST['test']==2)
{
echo "<?xml version=\"1.0\" ?><root>
	<location> 
		  <longitude>10</longitude>
		  <latitude>20</latitude>
	</location>
	<orientation>
		    <pitch>10</pitch>
		    <yaw>20</yaw>
	</orientation>	
</root>";
}
else
{
?>
<script type="text/javascript" language="javascript">
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
	  alert(yawVal);
         } else {
            alert('There was a problem with the request.');
         }
      }
   }
   function do_xml() {
      makeRequest('gps_service.php', 'getData=1&device_id=<?php echo $device_id;?>');
   } 

</script>

<input type="button" name="button" value="GET XML" 
   onclick="javascript:do_xml();">

<br><br>
Table filled with data requested from the server:<br>
<table border="1" id="mytable"></table>
<?php
}
?>