# ReadTracker

An app for keeping track on the books you've read, and give you insight into your reading patterns.

Published on [Google Play](https://play.google.com/store/apps/details?id=com.readtracker).

![Alt text](https://travis-ci.org/christoffer/readtracker.svg?branch=master "Build status")

# Maintainers wanted!

If you would find this project interesting, feel free to contact me to take over maintenance. Sadly, I have no spare time to work on it myself.

# Development

## Building

The project can either be built using Android studio or the command line Gradle wrapper. To build using the Gradle wrapper, cd into the root of the project and run: `./gradlew build`.

### Note on compiling on the command line

The gradle wrapper will use whatever the default java version of the command line is. However, it needs Java 8 to compile, so if another version is the current one you'll see an error like "Unsupported major.minor version 52.0" (the number might differ depending on which Java version you have the current one).

An easy way to solve this if you're on OSX is to just use the bundled OpenJDK that comes with Android Studio. This is trivially done like this (assuming that you have Android Studio installed in the default location):

```
$ export JAVA_HOME='/Applications/Android Studio.app/Contents/jre/jdk/Contents/Home'
$ ./gradlew build
```

## Note

This app has been in development for a few years a side project of mine for learning Android, so the code definitely has it's warts here and there. Pull request or suggestions for code clean-ups greatly appreciated.

## Screenshots

![Alt text](/gh-img/home.png?raw=true "Home screen")
![Alt text](/gh-img/session-log.png?raw=true "Session Log")
![Alt text](/gh-img/tracking.png?raw=true "Time tracking")
