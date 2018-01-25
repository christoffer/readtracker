# ReadTracker

An app for keeping track on the books you've read, and give you insight into your reading patterns.

Published on [Google Play](https://play.google.com/store/apps/details?id=com.readtracker).

![Alt text](https://travis-ci.org/christoffer/readtracker.svg?branch=master "Build status")

# Maintainers wanted!

If you would find this project interesting, feel free to contact me to take over maintenance. Sadly, I have no spare time to work on it myself.

# Development

## Add your keystore

The keystore configuration is read from a local `gradle.properties` file located in `$HOME/.gradle/gradle.properties`. A sample configuration can be found in the included `gradle.properties`. Start by coping the lines from there into `~/.gradle/gradle.properties` (create the file if it doesn't exist).

For development you can use the Android debug keystore, usually located in `~/.android/debug.keystore`. To use the Android debug store you can use the configuration below by pasting it into `~/.gradle/gradle.properties` (be sure to update the path to the keystore to the absolute path to `~/.android/debug.keystore` on your machine):

```
READTRACKER_RELEASE_STORE_FILE=/Users/<your username>/.android/debug.keystore
READTRACKER_RELEASE_KEY_ALIAS=androiddebugkey
READTRACKER_RELEASE_STORE_PASSWORD=android
READTRACKER_RELEASE_KEY_PASSWORD=android
```

## Building

The project can either be built using Android studio or the command line Gradle wrapper. To build using the Gradle wrapper, cd into the root of the project and run: `./gradlew build`.

## Note

This app has been in development for a few years a side project of mine for learning Android, so the code definitely has it's warts here and there. Pull request or suggestions for code clean-ups greatly appreciated.

## Screenshots

![Alt text](/gh-img/home.png?raw=true "Home screen")
![Alt text](/gh-img/session-log.png?raw=true "Session Log")
![Alt text](/gh-img/tracking.png?raw=true "Time tracking")
