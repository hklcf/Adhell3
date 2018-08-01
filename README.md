# Disclaimer
Adhell3 is merely an app that is using the Samsung Knox SDK APIs. <br/>
In order to use these APIs, the Knox SDK and the Samsung Knox License (SKL) key are needed. <br/>
These are Samsung's properties which are not available in this repository and therefore they need to be downloaded and obtained by the developer after accepting the agreement given by Samsung. <br/>
The developer is then responsible how this app will be used and I don't take any responsibilities of any damages caused by this app. <br/>

The Knox SDK can be downloaded here: https://seap.samsung.com/sdk/knox-android <br/>
The Samsung Knox License key can be obtained here: https://seap.samsung.com/license-keys/create#section-knox-sdk <br/>
The API can be found here: https://seap.samsung.com/api-references/android/reference/packages.html


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

- Backup and restore database<br/>
Backup the content of the database on internal storage so that later it can be used to restore the database if the database is somehow damage.

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
- Create `app.properties` file inside app folder: `app\app.properties`
- Put `package.name=your.package.name` in the first line of the file

### Android Studio
- Download and install latest Android Studio from https://developer.android.com/studio/index.html
- Open the Adhell3 project in Android Studio
- Install missing SDK, build-tools and other things that are prompted
- For `Configuration on demand is not supported` error, see this comment https://gitlab.com/fusionjack/adhell3/commit/1fb8ea98cf43507b32db56d9fb584b33dc6579f1#note_74463246

### Knox SDK
- Download latest Knox SDK zip and supportlib.jar from https://seap.samsung.com/sdk/knox-android
- Create `libs` sub-folder in `app` folder: `app\libs`
- Put the `knoxsdk.jar` and `supportlib.jar` to `app\libs` folder in the project 

## Migration guide from 3.0.0.x to 3.1.1.x
- Make a backup database in Adhell3
- Uninstall Adhell3 3.0.0.x (Use Settings - Delete Adhell)
- Install Adhell3 3.1.1.x
- Enable Admin and active key as usual
- Give Adhell3 a storage permission manually (Settings - Apps - Adhell 3 - Permissions - Storage)
- Restore database and enjoy!

## Migration note from Knox Standard SDK to Knox SDK
* Knox SDK license key is now needed<br/>
It contains two keys: Samsung Knox License (SKL) key and Backwards-compatible key<br/>
For devices with Knox 2.8 and newer, only SKL key is needed.<br/>
For devices with Knox 2.7.1 or earlier, the backwards-compatible key additionally to the SKL key is needed.

* Support only Knox version 2.6 - 3.1 (API level 19 - 25)<br/>
Knox version 2.6 is equivalent to Standard SDK version 5.6. See this site for the version mapping: https://seap.samsung.com/sdk/knox-version-mapping<br/>
The API to support Knox version 2.5 and earlier is not available anymore in Knox SDK. Hence, I drop these versions.
    
* App permission behavior changes<br/>
With Knox SDK, it is only possible to disable app permission that has protection-level of 'dangerous'.<br/>
If a permission is disabled, other permissions belong to the same group will also be disabled. E.g. it is not possible anymore to just disable WRITE_EXTERNAL_STORAGE without disabling READ_EXTERNAL_STORAGE.<br/>
This functionality is basically the same if you disable app permission in Android Settings - Apps - [AppName] - Permissions. You only see the permission groups.
    
* Not using local database anymore<br/>
Starting with 3.1.1.x, the Adhell3 database won't be created in internal storage anymore.<br/>
This was a convenience feature when most of the people need to re-install Adhell3 and each time with different package name.<br/>
Since you can compile it by yourself, you can just assign the same package name so that your existing Adhell3 will be updated.

## Customization
### Change the default 15k domain limit
* Put `domain.limit` in `app.properties`, e.g. `domain.limit=50000`

### Make SKL key and backwards-compatible key available when activating Adhell3
* Put `skl.key` in `app.properties`, e.g. `skl.key=KLM06-XXXXX-XXXXX-XXXXX-XXXXX-XXXXX`
* Put `backwards.key` in `app.properties`, e.g. `backwards.key=B6B2BXXXXXXXXXXXXXXXX`
* If you use these properties, beware when you distribute your app as the app contains the keys in plain text
* This is just for convenience purpose so that you don't need to make your keys somewhere available when you are on the phone, hence this only works on a new installation

### Domain prefix
* Prefix all domains (with the exception of Filter Lists) with * or nothing.
* Valid `domain.prefix` options: `domain.prefix=true`, `domain.prefix=false`
* If you choose not to define a prefixing option, domains will not be prefixed.
* Put `domain.prefix` in `app.properties`
* `domain.prefix=true` -> prefix all domains with `*`
* `domain.prefix=false` -> don't prefix anything, keep domains as they are
* nothing -> no prefix

### Hidden features
* Beware that enabling some hidden features may cause the device to malfunction if they are not used with precaution, especially when disabling system apps. Enable them at your own risk. 
* Add `enable.disableApps=true` in `app.properties` -> to enable 'Disable Apps' feature: <br/>
An ability to disable user or system applications entirely
* Add `enable.appComponent=true` in `app.properties` -> to enable 'App Component' feature: <br/>
An ability to disable app's permissions, services and receivers. Only user apps are supported.

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
You need an Samsung Knox License (SKL) key in order to use Adhell3. <br/>
You need to enroll as a developer in order to get this license. As for developer, the license needs to be renewed every 3 months.
- Enroll as a developer with this link: https://seap.samsung.com/enrollment
- Generate a license key with this link: https://seap.samsung.com/license-keys/create#section-knox-sdk
- Give an alias name, e.g. test
- Click on `Generate License Key`
- There will be two keys: Samsung Knox License key (SKL) and Backwards-compatible key<br/>
For devices with Knox 2.8 and newer, only SKL key is needed.<br/>
For devices with Knox 2.7.1 or earlier, the backwards-compatible key additionally to the SKL key is needed.


## Credits
* Adhell3 is based on FiendFyre's Adhell2 which is heavily modified by me.<br/>
* Big thanks to @mmotti who provides a host file for Adhell3. You can visit his Github here: https://github.com/mmotti/mmotti-host-file
* Adhell3 is using icons from https://material.io/icons