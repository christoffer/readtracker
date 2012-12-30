# Readtracker

## Note

The quality of the code in ReadTracker varies on a scale between acceptable and bad. I take no pride, nor shame, in it.
ReadTracker was created during a weekend hack on one of the bi-annual Readmill retreats, and continued development has
always been mainly during other hack weekends and or/during some spare time here and there. It was not initially
intended to be released as open source, and the focus has always been to A) learn Android B) dog food the Readmill API
and C) have fun. Hence the neglected refactorings.

However, since a few people I've talked with have shown interest in the project I've chosen to make the code publicly
available to anyone who might be interested in reading it, and/or want to contribute.

Thanks,
/Christoffer

## Installation

  1. Install maven
  2. Create the file `/assets/readmill.properties` with credentials from [your Readmill app](https://readmill.com/you/apps):

  * client-id=my-test-app-client-id
  * client-secret=my-test-app-client-secret

  3. run `mvn package` to download all dependencies, compile the app, run the test and package as an APK.
