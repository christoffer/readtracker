# Ensuring the Android SDK licenses are accepted, to let gradle download any missing dependency
#
# Inspired from https://developer.android.com/studio/intro/update.html#download-with-gradle
# Inspired from
#
# Note: These values below represent the hashes of the agreed SDK licenses. Last changed: 2017-10-10
# If they change in the future, gradle will not be able to download new dependencies, and the hashes
# below need to be updated again according to the steps explained in the article above.

mkdir "$ANDROID_HOME/licenses" || true \
    && echo -e "\nd56f5187479451eabf01fb78af6dfcb131a6481e" > "$ANDROID_HOME/licenses/android-sdk-license" \
    && echo -e "\n84831b9409646a918e30573bab4c9c91346d8abd" > "$ANDROID_HOME/licenses/android-sdk-preview-license" \
    && echo -e "\n601085b94cd77f0b54ff86406957099ebe79c4d6" > "$ANDROID_HOME/licenses/android-googletv-license" \
    && echo -e "\n33b6a2b64607f11b759f320ef9dff4ae5c47d97a" > "$ANDROID_HOME/licenses/google-gdk-license" \
    && echo -e "\nd975f751698a77b662f1254ddbeed3901e976f5a" > "$ANDROID_HOME/licenses/intel-android-extra-license" \
    && echo -e "\ne9acab5b5fbb560a72cfaecce8946896ff6aab9d" > "$ANDROID_HOME/licenses/mips-android-sysimage-license"