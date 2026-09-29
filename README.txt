Flavour Atlas for Android - offline, no internet permission.

Build: push this folder to a PRIVATE GitHub repository. The "Build APK"
workflow runs automatically (Actions tab). Download the "flavour-atlas-apk"
artifact, unzip it, and install app-debug.apk on your phone and tablet.

Update later: replace app/src/main/assets/index.html, raise versionCode in
app/build.gradle, push again, and install the new APK over the old one.
Keep keystore/flavour-atlas.jks: it lets updates install without losing data.
