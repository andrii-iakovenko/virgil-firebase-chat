## Configure Virgil application

- Create new Virgil application at https://developer.virgilsecurity.com/dashboard/
- Add new application token

## Configure Firebase project
- Create new project at console https://console.firebase.google.com/
- Go to Authentication - Sign-in method screen
- Enable authentication with Google provider
- Go to Settings - General tab
- Fill 'Package name' field with `com.google.firebase.codelab.friendlychat` value
- Add SHA-1 and SHA-256 fingerprints
- Download `google-services.json'

## Configure Android application
- Copy `google-services.json` file to `add` directory
- Open Android application in Android Studio
- Put proper values into `app/src/main/res/xml/prefs.xml` file
- Run Android application on device
