/**
 * Copyright Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.codelab.friendlychat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.codelab.friendlychat.model.User;
import com.google.firebase.codelab.friendlychat.utils.Constants;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.virgilsecurity.sdk.client.RequestSigner;
import com.virgilsecurity.sdk.client.VirgilClient;
import com.virgilsecurity.sdk.client.exceptions.VirgilServiceException;
import com.virgilsecurity.sdk.client.requests.CreateCardRequest;
import com.virgilsecurity.sdk.client.utils.ConvertionUtils;
import com.virgilsecurity.sdk.client.utils.StringUtils;
import com.virgilsecurity.sdk.crypto.Crypto;
import com.virgilsecurity.sdk.crypto.KeyPair;
import com.virgilsecurity.sdk.crypto.PrivateKey;
import com.virgilsecurity.sdk.crypto.VirgilCrypto;

public class SignInActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;
    private SignInButton mSignInButton;

    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mFirebaseAuth;
    private DatabaseReference mFirebaseDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Intialize preferences
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.prefs, true);

        // Assign fields
        mSignInButton = (SignInButton) findViewById(R.id.sign_in_button);

        // Set click listeners
        mSignInButton.setOnClickListener(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        // Initialize FirebaseAuth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
    }

    private void handleFirebaseAuthResult(AuthResult authResult) {
        if (authResult != null) {
            // Welcome the user
            FirebaseUser user = authResult.getUser();
            Toast.makeText(this, "Welcome " + user.getEmail(), Toast.LENGTH_SHORT).show();

            // Go back to the main activity
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            default:
                return;
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                // Google Sign In failed
                Log.e(TAG, "Google Sign In failed. " + result.getStatus().getStatusCode());
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(SignInActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            registerUser();
                        }
                    }
                });
    }

    private void saveUser() {
        final FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
        Query query = mFirebaseDatabaseReference.child(Constants.USERS_CHILD).orderByChild("email").equalTo(firebaseUser.getEmail()).limitToFirst(1);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // User is already registered in database
                } else {
                    // Register new user
                    User user = new User(firebaseUser.getEmail(), firebaseUser.getDisplayName());
                    mFirebaseDatabaseReference.child(Constants.USERS_CHILD).push().setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            startMainActivity();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void registerUser() {
        final String email = mFirebaseAuth.getCurrentUser().getEmail();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String privateKeyBase64String = prefs.getString(Constants.PRIVATE_KEY + email, "");
        if (StringUtils.isBlank(privateKeyBase64String)) {
            Thread backgrond = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Generate new key pair for user
                    Crypto crypto = new VirgilCrypto();
                    KeyPair keyPair = crypto.generateKeys();

                    String virgilToken = prefs.getString(Constants.VIRGIL_TOKEN, "");
                    String appId = prefs.getString(Constants.VIRGIL_APP_ID, "");
                    String appKeyStr = prefs.getString(Constants.VIRGIL_APP_KEY, "");
                    String appKeyPwd = prefs.getString(Constants.VIRGIL_APP_KEY_PWD, "");
                    PrivateKey appKey = crypto.importPrivateKey(ConvertionUtils.base64ToArray(appKeyStr), appKeyPwd);
                    VirgilClient virgilClient = new VirgilClient(virgilToken);

                    // Create new Virgil Card
                    CreateCardRequest createCardRequest = new CreateCardRequest(email, Constants.IDENTITY_TYPE, crypto.exportPublicKey(keyPair.getPublicKey()));

                    RequestSigner requestSigner = new RequestSigner(crypto);

                    requestSigner.selfSign(createCardRequest, keyPair.getPrivateKey());
                    requestSigner.authoritySign(createCardRequest, appId, appKey);

                    try {
                        virgilClient.createCard(createCardRequest);

                        prefs.edit().putString(Constants.PRIVATE_KEY + email, ConvertionUtils.toBase64String(crypto.exportPrivateKey(keyPair.getPrivateKey()))).commit();

                        sendMessageToHandler(Constants.EVENTS.SIGNING_PASSED);
                    } catch (VirgilServiceException e) {
                        Log.e(TAG, "register Virgil Card", e);

                        sendMessageToHandler(Constants.EVENTS.SIGNING_FAILED);
                    }
                }
            });
            backgrond.start();

        } else {
            saveUser();
        }
    }

    private void startMainActivity() {
        startActivity(new Intent(SignInActivity.this, MainActivity.class));
        finish();
    }

    private void sendMessageToHandler(String event) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.EVENT, event);
        Message msg = handler.obtainMessage();
        msg.setData(bundle);

        handler.sendMessage(msg);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(android.os.Message msg) {

            String event = msg.getData().getString(Constants.EVENT);
            switch (event) {
                case Constants.EVENTS.SIGNING_PASSED:
                    saveUser();
                    break;
                case Constants.EVENTS.SIGNING_FAILED:
                    Toast.makeText(SignInActivity.this, "Can't create Virgil Card.",
                            Toast.LENGTH_SHORT).show();
                    mFirebaseAuth.signOut();
                    break;
            }
        }
    };

}
