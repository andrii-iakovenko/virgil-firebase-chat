# Virgil + Firebase demo chat

## Setting up your project

### Target

* Java 7+
* Android API 22+

### Prerequisites

* Java Development Kit (JDK) 7+
* Maven 3+
* Android Studio

## Configuration

### Configure Virgil application

- Create new Virgil application at https://developer.virgilsecurity.com/dashboard/
- Add new token

Collect attributes from this page:
|| Attribute name || Description ||
| VIRGIL_ACCESS_TOKEN | The generated access token |
| VIRGIL_APP_ID | APP ID |
| VIRGIL_APP_KEY | Application private key. See file downloaded when you created an application |
| VIRGIL_APP_KEY_PWD | The password which used for applicaion private key protection |

### Configure Firebase project
- Create new project at console https://console.firebase.google.com/
- Go to Authentication - Sign-in method screen
- Enable authentication with Google provider
- Go to Settings - General tab
- Fill 'Package name' field with `com.google.firebase.codelab.friendlychat` value
- Add SHA-1 and SHA-256 fingerprints
- Download `google-services.json'
- Go to Settings - Service Accounts - Firebase Admin SDK and generate new private key

Collect attributes from this page:
|| Attribute name || Description ||
| FIREBASE_SERVICE_ACCOUNT_KEY | This file downloaded authmatically when you generate new private key at Settings - Service Accounts - Firebase Admin SDK |
| FIREBASE_DATABASE_URL | Copy value from the header on Database - Data tab |
| google-services.json | Download it from Settings - General - Your apps |

### Configure server

Build server running next command in the `server` directory

```
mvn clean package
```

Start server with command

```
java -jar target/server.jar \
 -DserviceAccountKey=[FIREBASE_SERVICE_ACCOUNT_KEY] \
 -DdatabaseUrl=[FIREBASE_DATABASE_URL] \
 -DaccessToken=[VIRGIL_ACCESS_TOKEN] \
 -DappId=[VIRGIL_APP_ID] \
 -DappKey=[VIRGIL_APP_KEY] \
 -DappKeyPwd=[VIRGIL_APP_KEY_PWD]
```

FIREBASE_SERVICE_ACCOUNT_KEY is a path to the file.
VIRGIL_APP_KEY is a Base64-encoded private key.

Type `SERVER_URL/users` in adress line of your Web browser. The result should be `[]`.
Where SERVER_URL is a URL like `http://192.168.77.1:8080`. Don't use `localhost` or `127.0.0.1` as a host name!  

### Configure Android application
- Copy `google-services.json` file to the `app` directory
- Open Android application in Android Studio
- Put proper values into `app/src/main/res/xml/prefs.xml` file
- Run Android application on device

## How it works

When Server starts, it should initialize Firebase app with code

```java
// Application.java

FileInputStream serviceAccount = new FileInputStream(serviceAccountKeyPath);
FirebaseOptions options = new FirebaseOptions.Builder()
        .setCredential(FirebaseCredentials.fromCertificate(serviceAccount)).setDatabaseUrl(databaseUrl)
        .build();

FirebaseApp.initializeApp(options);
```

Android application should register new User. 

First of all user's key pair should be generated. Private key used for message decryption.

```java
// SignInActivity.java

// Generate new key pair for user
Crypto crypto = new VirgilCrypto();
KeyPair keyPair = crypto.generateKeys();
```

> User's public key should be registered at Virgil services. Since Virgil application private key required for creating Virgil card, Virgil card should be registered from Server only.
> Access token is only Virgil application parameter which could be stored at mobile device. Also you may obtain access token from Server if you need.

For registering new user mobile application sends request to Server.

```java
// SignInActivity.java
Uri uri = Uri.parse(mBaseURL + "/signup").buildUpon()
        .appendQueryParameter("email", email)
        .appendQueryParameter("password", password)
        .appendQueryParameter("key", ConvertionUtils.toBase64String(crypto.exportPrivateKey(keyPair.getPrivateKey())))
        .build();

URL url = new URL(uri.toString());
String customToken = HttpUtils.execute(url, "GET", null, null, String.class);
```

> In demo applicaion we can use GET request for registration and login. You SHOULD NOT use GET request for real apps. Use POST request instead!

> Don't log private keys on server side. It's unsafe.

Server registers Virgil card for new user.

```java
// UserController.java

PrivateKey privateKey = crypto.importPrivateKey(ConvertionUtils.base64ToBytes(key));
PublicKey publicKey = crypto.extractPublicKey(privateKey);

CreateCardRequest createCardRequest = new CreateCardRequest(email, "firebase_user",
        crypto.exportPublicKey(publicKey));

RequestSigner requestSigner = new RequestSigner(crypto);

requestSigner.selfSign(createCardRequest, privateKey);
requestSigner.authoritySign(createCardRequest, appId, appKey);

Card card = virgilClient.createCard(createCardRequest);
```

Then it saves new user in storage (we just use a map) and generates custom token which will be used by mobile app for authentication in Firebase.

This links should be useful:
- https://firebase.google.com/docs/auth/admin/create-custom-tokens
- https://firebase.google.com/docs/auth/android/custom-auth

```java
// UserController.java

final Task<String> task = FirebaseAuth.getInstance().createCustomToken(email)
        .addOnSuccessListener(new OnSuccessListener<String>() {

    public void onSuccess(String customToken) {
        log.debug("Generated custom token: {}", customToken);
    }
});
try {
    Tasks.await(task);
} catch (ExecutionException | InterruptedException e) {
    throw new ServiceException(003001, "Firebase authentication failed");
}

String customToken = task.getResult();
```

When mobile application receives custom token from the Server, it could authenticate in Firebase.

```java
// SignInActivity.java
mFirebaseAuth.signInWithCustomToken(customToken)
        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {

    @Override
    public void onComplete(@NonNull Task<AuthResult> task) {
        if (!task.isSuccessful()) {
            // Not authenticated
        } else {
            // Authenticated
        }
    }
});
```

Before sending message, mobile application should collect keys for all recipients.

```java
// MainActivity.java

// Find Virgil Cards for users
SearchCriteria criteria = SearchCriteria.byIdentities(identities);
List<Card> cards = mVirgilClient.searchCards(criteria);
if (!cards.isEmpty()) {
    for (Card card : cards) {
        PublicKey publicKey = mCrypto.importPublicKey(card.getPublicKey());

        Log.d(TAG, "Add public key for " + card.getIdentity());
        ...
    }
}
```

Then message could be encrypted

```java
// MainActivity.java

List<PublicKey> recipients = ...;
String encryptedMessage = message;
try {
    byte[] data = mCrypto.encrypt(ConvertionUtils.toBytes(message), recipients.toArray(new PublicKey[recipients.size()]));
    encryptedMessage = ConvertionUtils.toBase64String(data);
} catch (Exception e) {
    Log.e(TAG, "Encryption error: " + e.getMessage());
}
```

and sent to Firebase database

```java
// MainActivity.java

FriendlyMessage friendlyMessage = new FriendlyMessage(encryptedMessage, mUsername, mUseremail, null);
mFirebaseDatabaseReference.child(Constants.MESSAGES_CHILD).push().setValue(friendlyMessage);
```

When mobile application receives a message, it should decrypt it with user's private key.
```java
// MainActivity.java

FriendlyMessage friendlyMessage = ...
...
byte[] decrypted = mCrypto.decrypt(ConvertionUtils.base64ToBytes(friendlyMessage.getText()), mPrivateKey);
friendlyMessage.setText(ConvertionUtils.toString(decrypted));
```
