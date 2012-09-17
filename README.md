# Sense Platform Library


Library for including the Sense Platform into Android apps. It allows you to start the Sense service, and gives you access to configure the sensors you want to use.

## Configuring eclipse
You need to install both the Android SDK and the Android Plugin for Eclipse. See the Android Developers website for a complete tutorial: http://developer.android.com/intl/fr/sdk/installing.html.

### Import Sense Library project in Eclipse

There are two ways to import the code: you can use the EGIT plugin for Eclipse (recommended), or you can checkout the code using some other git client.

#### Import using EGIT

1.    In Eclipse, go to File > Import... Choose GIT > Projects from Git
2.    Select URI, enter the git repository for this project: https://github.com/senseobservationsystems/sense-android-library.git
3.    Next, select the branches you need. The most stable code is found in master.
4.    Next, select the location for the repository and the branch to initially checkout, usually master. **Important** Choose a location *outside* your workspace, as eclipse can complain about overlapping paths otherwise.
5.    Next, choose Import existing projects.
6.    Finish the wizard.

You can now continue to Configure your project to use the library

#### Import otherwise

1.    Download the source code from github. The most stable code can be found in the master branch.
2.    Do NOT put the folder inside you Eclipse workspace folder, a bug in Eclipse makes it impossible to import Android projects when they are already in the workspace directory.
3.    In Eclipse, import the project 'File -> Import...' Choos 'Android -> Existing Android code into Workspace' 
4.    Select the directory that contains the downloaded source code, i.e. the parent directory of the download.
6.    Finish the wizard.


### Configuring your project to use the library

1.    Right-click on your own Android project that will supply the GUI and select Properties.
2.    Go the the Android pane, and click "Add" in the Library section and add the Sense Library Project.
3.    Press OK to apply the changes.

## Using the library

### Native Android (Service binding)

TODO

### Web apps (PhoneGap)

1.    In your HTML, import the javascript file that is located at: /stuff/example_html/sense_platform.js.
2.    Edit the plugins.xml file of your Android project and add
      `<plugin name="SensePlatform" value="com.phonegap.plugins.sense.SensePlugin"/>`

3.    The Sense platform methods are now accessible in javascript through window.plugins.sense.*

There is an example project for Android apps that use PhoneGap: https://dev.almende.com/projects/show/sense-phonegap?jump=my.
