# Flavour Atlas for Android

Offline cooking library: 149 recipes (90 from across Asia), ingredients, techniques and food safety.
No internet permission: nothing ever leaves the device.

## Get the app
Open **Releases** (right side of this page) → newest **Flavour Atlas build N** → download the APK → install.

## Update the recipes or app
1. Replace `app/src/main/assets/index.html` (Add file → Upload files → Commit).
2. The **Actions** tab builds automatically (about 2 minutes).
3. Install the new APK from **Releases** over the old one.

The version number rises automatically with every build, so there is nothing else to edit.

## Keep safe
`keystore/flavour-atlas.jks` signs every build. Keep it: it lets updates install without losing your saved notes and photos.

## Project layout
- `app/src/main/assets/index.html` – the whole app page
- `app/src/main/java/com/flavouratlas/app/MainActivity.java` – the Android shell (offline WebView, backups, file picker, crash screen)
- `.github/workflows/build.yml` – builds and publishes the APK
