<?php
print "<h1>Register device service</h1>";
$ds_id		= $_REQUEST['ds_id'];
$ds_type	= $_REQUEST['ds_type'];
if(isset($_REQUEST['url']))
  $url = $_REQUEST['url'];
else
  $url = "http://demo.almende.com/commonSense2/deviceservices/$ds_type.php";
$dsRegistryServer = "http://demo.almende.com:8080/registerdeviceservice?ds_id=".$ds_id."&ds_type=".$ds_type."&virtual=true&url=".$url;
$result = file_get_contents($dsRegistryServer);
?>