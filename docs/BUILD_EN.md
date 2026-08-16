# Build and signing

## Requirements

- An Android development host.
- Android SDK.
- Android Studio or command-line Gradle.
- JDK 21.

## Debug build

From the repository root:

```powershell
.\gradlew.bat testMadaniDebugUnitTest --tests com.quran.labs.androidquran.data.QuranDisplayDataSearchTest
.\gradlew.bat assembleMadaniDebug
```

Output:

```text
app\build\outputs\apk\madani\debug\app-madani-debug.apk
```

## Release build

Create your own signing key and keep it outside the repository. Never publish the key or its password.

```powershell
.\gradlew.bat assembleMadaniRelease -PSTORE_FILE="PATH_TO_KEYSTORE" -PSTORE_PASSWORD="STORE_PASSWORD" -PKEY_ALIAS="KEY_ALIAS" -PKEY_PASSWORD="KEY_PASSWORD"
```

Output:

```text
app\build\outputs\apk\madani\release\app-madani-release.apk
```

## Future updates

- Keep the same signing key for every release.
- Increment the version code and version name.
- Build with the same key.
- Android can install the update over the previous version while preserving data when the application ID and signing identity stay unchanged.
