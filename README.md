# Sense for Android apps

Library project for Android apps that use the Sense platform and communicate with CommonSense.

## Javadoc

See this projects' [GitHub pages](http://senseobservationsystems.github.com/sense-android-library).

## Tutorial

Please read the tutorial on the [Sense Developer Portal](http://developer.sense-os.nl/Libraries/Android).

## Maven

When using Maven (3.0.5+), you can add this project as an 
[APKLIB](https://code.google.com/p/maven-android-plugin/wiki/ApkLib)
into your local Maven repository as follows.

First add some dependencies to your local Maven repository that are not available 
in Maven central:

#### Dependency 1: compatibility-v4 library (rev 12)

Note that you need the compatibility-v4 library, revision 12. Google didn't put that 
into Maven central (yet). If your local Maven repository does not contain it, install 
it locally by using the 
[maven-android-sdk-deployer](https://github.com/mosabua/maven-android-sdk-deployer).

#### Dependency 2: cordova 2.7.0

Navigate to the Library's `libs/` folder and execute the following command:

```bash
mvn install:install-file \
  -Dfile=cordova-2.7.0.jar \
  -DgroupId=org.apache.cordova \
  -DartifactId=cordova \
  -Dversion=2.7.0 \
  -Dpackaging=jar \
  -DgeneratePom=true
```
Now put the Sense Library to your local Maven repository:

```bash
git clone https://github.com/senseobservationsystems/sense-android-library
cd sense-android-library/sense-android-library/
mvn install
```
which should result in a `BUILD SUCCESS`.

After that, you can reference the Sense Library in a Maven-flavoured Android App 
by adding the following dependency to your POM:

```xml
<dependency> 
    <groupId>nl.sense_os</groupId> 
    <artifactId>sense-android-library</artifactId> 
    <version>1.0-SNAPSHOT</version> 
    <type>apklib</type>
</dependency>
```

## Pre-build commands

The `build/` folder contains scripts that need to be executed right before every build.
In Eclipse, they can be added by going to Properties > Builders > New... and selecting the script in the "Location" field.

#### setAppVersion.sh
Copy the contents of `setAppVerionExample.sh` into this file, and update the paths for your own system. (Do not modify the example file!)
If it does not work, try the following solutions:
- Change the first line to `#!/bin/sh` if using `sh` instead of `dash`.
- Use only absolute paths (so no `$(pwd)`).

