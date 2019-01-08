# Disclaimer
Adhell3 is merely an app that is using the Samsung Knox SDK APIs. <br/>
In order to use these APIs, the Knox SDK and a KPE Development license key are needed. <br/>
These are Samsung's properties which are not available in this repository and therefore they need to be downloaded and obtained by the developer after accepting the agreement given by Samsung. <br/>
The developer is then responsible how this app will be used and I don't take any responsibilities of any damages caused by this app. <br/>

The Knox SDK can be downloaded here: https://seap.samsung.com/sdk/knox-android <br/>
The Knox License key can be obtained here: https://seap.samsung.com/license-keys/create#section-knox-sdk <br/>
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

## Building APK (Windows)
Assume that our working directory is in `C:\Users\admin\adhell3`

### Java
- Download and install JDK 8 from http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
- Don't use JDK 9 as there is issue with Gradle. See Issue #78.

### Android SDK
- Download Android SDK tools from https://developer.android.com/studio/#downloads (sdk-tools-windows-xxxxxx.zip)
- Extract it to the working directory `C:\Users\admin\adhell3\tools`

### Adhell3 Project
- Download the project as a zip file from https://gitlab.com/fusionjack/adhell3/repository/master/archive.zip
- Extract the content to the working directory `C:\Users\admin\adhell3\project`
- Create `app.properties` file in `C:\Users\admin\adhell3\project\app` folder: `C:\Users\admin\adhell3\project\app\app.properties`
- Put `package.name=your.package.name` in the first line of the file

### Knox SDK
- Download latest Knox SDK and supportlib from https://seap.samsung.com/sdk/knox-android
- Extract `knoxsdk.jar` and `supportlib.jar` respectively from both zip files
- Rename the supportlib jar file name to `supportlib.jar` if it is not the case
- Create `libs` sub-folder in `C:\Users\admin\adhell3\project\app` folder: `C:\Users\admin\adhell3\project\app\libs`
- Put both jar files to `C:\Users\admin\adhell3\project\app\libs` folder

### Configuring Android SDK
- Run these following commands in a Windows console:<br/>
`cd C:\Users\admin\adhell3`<br/>
`mkdir android-sdk`<br/>
`cd tools\bin`<br/>
`sdkmanager "platform-tools" --sdk_root=C:\Users\admin\adhell3\android-sdk`
- Accept the license with `y`

### Building APK
- Run these following commands in a Windows console:<br/>
`cd C:\Users\admin\adhell3\project`<br/>
`set ANDROID_HOME=C:\Users\admin\adhell3\android-sdk`<br/>
`gradlew clean assembleDebug`<br/>
- Find the apk in `C:\Users\admin\adhell3\project\app\build\outputs\apk\debug`

## Customization
### Change the default 15k domain limit
* Put `domain.limit` in `app.properties`, e.g. `domain.limit=50000`

### Make license key and backwards-compatible key available when activating Adhell3
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

### Override default host with your own compiled host
* Put `default.host` in `app.properties`, e.g. `default.host=https://gitlab.com/fusionjack/hosts/raw/master/hosts`


## Prerequisite for using Adhell3
You need a KPE Development license key in order to use Adhell3. <br/>
You need to enroll as a developer in order to get this license. As for developer, the license needs to be renewed every 3 months.<br/>
When you receive a mail about license expiration, you usually cannot generate a new key immediately and you need to wait for some days.</br>
During this time, Adhell3 is still working properly. Adhell3 will show an activation dialog if the key cannot be used anymore. This time you should be able to generate a new key.

- Enroll as a developer with this link: https://seap.samsung.com/enrollment
- Generate a license key with this link: https://seap.samsung.com/license-keys/create#section-knox-sdk
- Give an alias name, e.g. test
- Click on `Generate License Key`
- There will be two keys: KPE key and backwards-compatible key<br/>
For devices with Knox 2.8 and newer, only KPE key is needed.<br/>
For devices with Knox 2.7.1 or earlier, the backwards-compatible key additionally to the KPE key is needed.


## Credits
* Adhell3 is based on FiendFyre's Adhell2 which is heavily modified by me.<br/>
* Big thanks to @mmotti who provides a host file for Adhell3. You can visit his Github here: https://github.com/mmotti/mmotti-host-file
* Adhell3 is using icons from https://material.io/icons