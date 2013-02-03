#!/bin/bash

${ANDROID_HOME:=""}

supportDir=""
if [ ${ANDROID_HOME} != "" ];
then
  # Has android home set, look for default place of the support dir
  maybeSupportDir=$ANDROID_HOME/extras/android/support/v4/android-support-v4.jar
  if [ -e $maybeSupportDir ]
  then
    echo "You have a file at: " $maybeSupportDir
    echo "Do you want to use that one? (y/n)"
    read answer
    if [ "$answer" == "y" ]
    then
      supportDir=$maybeSupportDir
    fi
  fi
fi


if [ "$supportDir" == "" ]
then
  echo "Enter the full path to the Android Support Jar (android-support-v4.jar)"
  echo "This file is usually under <android-home>/extras/android/support/v4/android-support-v4.jar"
  echo "Enter blank line to abort"
  printf "> "
  read supportDir
fi

if [ "$supportDir" != "" ]
then
  printf "Using support lib jar: [%s]" $supportDir
  mvn install:install-file -DgroupId=com.google.android -DartifactId=support-v4 -Dversion=11.0 -Dpackaging=jar -Dfile=$supportDir
else
  echo "Skipping maven install"
fi
