<?php
// Set the latest supported Sense App version
$supportedVersion = array(2,0);
$version = $_REQUEST['version'];
$version = explode('.', $version);
$oldVersion = 0;
if(sizeof($version) > 0)
{
	if($version[0] < $supportedVersion[0])
	{
		$oldVersion = 1;	
	}
	else if(sizeof($version) > 1 && $version[0] == $supportedVersion[0])
	{
		if($version[1] < $supportedVersion[1])
			$oldVersion = 1;
	}
}

if($oldVersion == 1)
 echo "{\"message\":\"A new version of the Sense Platform can be found on the market. This version is no longer supported.\"}";
else
 echo "{\"message\":\"\"}";
?>
