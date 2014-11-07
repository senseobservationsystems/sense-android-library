#!/bin/dash
# This script creates the AppInfoVersion.java for the sense-android-library AppInfoSensor
# If the Sense Cortex source is used then specify the git aim root directory
#specify the cortex lib dir if used, else comment out
#cortex_dir=../../Cworkspace/aim
# Specify the version of the cortex binary used
cortex_version=""
#specify the sense lib git root dir
sense_lib_dir=$(pwd)
#sense lib version file
sense_lib_version_file=$sense_lib_dir/sense-android-library/src/nl/sense_os/service/phonestate/AppInfoVersion.java

if [ -n "$cortex_dir" ]
then
	cd $cortex_dir
	cortex_version=`git describe --tags --dirty`
fi

cd $sense_lib_dir
sense_lib_version=`git describe --tags --dirty`

echo "package nl.sense_os.service.phonestate;\n" > $sense_lib_version_file
echo "public abstract class AppInfoVersion { \n" >> $sense_lib_version_file
echo "\tpublic final static String SENSE_LIBRARY_VERSION = \""$sense_lib_version"\";" >> $sense_lib_version_file
echo "\tpublic final static String CORTEX_VERSION = \""$cortex_version"\";" >> $sense_lib_version_file
echo "}" >> $sense_lib_version_file