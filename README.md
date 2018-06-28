# Disclaimer
Adhell3 is merely an app that is using the Samsung Knox Standard SDK APIs. <br/>
In order to use these APIs, the Knox Standard SDK and an Enterprise License Key (ELM) are needed. <br/>
These are Samsung's properties which are not available in this repository and therefore they need to be downloaded and obtained by the developer after accepting the agreement given by Samsung. <br/>
The developer is then responsible how this app will be used and I don't take any responsibilities of any damages caused by this app. <br/>

The Knox Standard SDK can be downloaded here: https://seap.samsung.com/sdk/knox-standard-android <br/>
The Enterprise License Key can be obtained here: https://seap.samsung.com/license-keys/create/knox_standard_android <br/>
The API can be found here: https://seap.samsung.com/api-references/android-standard/reference/packages.html


# Background
The original Adhell was developed by Samsung's developer. After he was forced to remove the code from internet by Samsung, FiendFyre was stepped up by providing the Adhell2. But after a while, it is also discontinued.<br/>
Adhell3 is an extension of previous discontinued Adhell2 app with more additional features.


## Features
- Mobile and Wi-Fi access disabler<br/>
Disable internet access when on mobile or/and Wi-Fi for specific apps. This can be useful to avoid watching video accidentally by using mobile data.

- Custom deny firewall rule<br/>
This can be used for example to define a custom firewall rule to block ads for Chrome app on port 53 for all ip addresses:<br/>
    `com.android.chrome|*|53`

- Whitelist URL for a specific app<br/>
When you have a domain that you want to block system wide, but you need this domain on a particular app. Otherwise, the app won't work.<br/>
Instead whitelist-ing this app, you can just whitelist that domain for this app.<br/>
Example: Block the domain `graph.facebook.com` system wide, but allows it for Facebook Messenger so that it can be used for login:<br/>
    `com.facebook.orca|graph.facebook.com`

- Support local host source<br/>
The host file can be located on internal or external storage.<br/>
An example to use host.txt file which is located at internal storage:<br/>
    `file:///mnt/sdcard/hosts.txt`

- Show the content of host source<br/>
Show the list of domains of individual host source or the list of all blocked domains from all host sources.<br/>
This can be useful to check whether particular domain is in the list.<br/>
The list contains of unique domains.

- Retain data across installations<br/>
Adhell is usually generated with a unique package name. For the users that don't build Adhell with Android Studio, they cannot use the same package name. This means that they need to reinstall Adhell every times there is a new version and on every installation, they need to input the same data again and again. <br/>
With this feature, there is no need to export or import functionality again. The data will be kept on internal storage and by next installation, the same data will be used.

- Backup and restore database<br/>
Backup the content of the database on internal storage so that later it can be used to restore the database if the database is somehow damage.

- App components<br/>
Ability to disable app's permissions, services and receivers. Only user apps are supported.

- DNS per application basis<br/>
Set custom DNS for selected apps. Only user apps are supported.

## Prerequisite for building apk
### Java
- Install JDK 8 for your platform http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
- Don't use JDK 9 as there is issue with Gradle. See Issue #78.
 
### Git (Optional, but recommended)
- Install git for your platform: https://git-scm.com/book/en/v2/Getting-Started-Installing-Git

### Source code
- Using git: Clone the project with `git clone https://gitlab.com/fusionjack/adhell3.git`
- Without git: Download the source code as a zip file: https://gitlab.com/fusionjack/adhell3/repository/master/archive.zip
- Set `applicationId` with a valid package name in `app\build.gradle`, e.g.: com.dhf.erz58384

### Android Studio
- Download and install latest Android Studio from https://developer.android.com/studio/index.html
- Open the downloaded project in Android Studio
- Install missing SDK and build-tools
- For `Configuration on demand is not supported` error, see this comment https://gitlab.com/fusionjack/adhell3/commit/1fb8ea98cf43507b32db56d9fb584b33dc6579f1#note_74463246

### Knox Standard SDK
- Download it from https://seap.samsung.com/sdk/knox-standard-android
- Take the `libs` content and put it to `app\libs` folder in the project 

## How to build apk

### With Git
Plug in your device and run this following commands in a bash console:<br/>
1. `cd adhell3`<br/>
2. `git stash && git pull --rebase && git stash pop`<br/>
3. `bash gradlew clean installDebug`

Explanation:
1. Enter adhell3 folder
2. It stores your changes, e.g package name, updates the source code and re-apply your changes
3. Build and install apk on the device

### Without Git
1. Re-download the source code as a zip file and re-applies your changes manually<br/>
2. Plug in your device and run this following commands in Windows console:<br/>
`cd adhell3`<br/>
`gradlew clean installDebug`


## Prerequisite for using Adhell3
You need an Enterprise License Key (ELM) in order to use Adhell3. <br/>
You need to enroll as a developer in order to get this license. The license can be obtained here: https://seap.samsung.com/license-keys/create/knox_standard_android <br/>
As for developer, the license needs to be renewed every 3 months. Don't forget to revoke the old license key before creating a new one.
- Enroll as a developer with this link: https://seap.samsung.com/enrollment
- Generate a license key with this link: https://seap.samsung.com/license-keys/create#section-knox-standard-sdk
- Choose `Enterprise License Key`
- Give an alias name, e.g. test
- Click on `Generate License Key`


## Credits
* Adhell3 is based on FiendFyre's Adhell2 which is heavily modified by me.<br/>
* Big thanks to @mmotti who provides a host file for Adhell3. You can visit his Github here: https://github.com/mmotti/mmotti-host-file
* Adhell3 is using icons from https://material.io/icons