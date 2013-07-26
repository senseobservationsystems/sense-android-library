# Sense for Android apps

Library project for Android apps that use the Sense platform and communicate with CommonSense.

## Javadoc

See this projects' [GitHub pages](http://senseobservationsystems.github.com/sense-android-library).

## Tutorial

Please read the tutorial on the [Sense Developer Portal](http://developer.sense-os.nl/Libraries/Android).

## Maven

When using Maven (3.0.5+), you can add this project as an [APKLIB](https://code.google.com/p/maven-android-plugin/wiki/ApkLib)
into your local Maven repository as follows:

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
