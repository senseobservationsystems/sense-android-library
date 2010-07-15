<?php
$seconds = microtime(true);

echo $seconds."\n";
echo microtime(false);
//echo basename($_SERVER['REQUEST_URI']);
echo $_SERVER['SCRIPT_NAME'];

?>
