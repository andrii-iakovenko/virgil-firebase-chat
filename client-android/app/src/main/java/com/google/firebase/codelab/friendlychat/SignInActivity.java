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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.codelab.friendlychat.utils.Constants;
import com.google.firebase.codelab.friendlychat.utils.HttpUtils;
import com.virgilsecurity.sdk.crypto.Crypto;
import com.virgilsecurity.sdk.crypto.KeyPair;
import com.virgilsecurity.sdk.crypto.VirgilCrypto;
import com.virgilsecurity.sdk.utils.ConvertionUtils;
import com.virgilsecurity.sdk.utils.StringUtils;

import java.net.URL;

public class SignInActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private static final String TAG = "SignInActivity";

    private EditText mEmail;
    private EditText mPassword;
    private Button mSignInButton;

    private FirebaseAuth mFirebaseAuth;

    private SharedPreferences mSharedPreferences;
    private String mBaseURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Intialize preferences
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.prefs, true);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mBaseURL = mSharedPreferences.getString(Constants.BASE_URL, "");

        // Assign fields
        mEmail = (EditText) findViewById(R.id.email);
        mPassword = (EditText) findViewById(R.id.password);
        mSignInButton = (Button) findViewById(R.id.sign_in_button);

        // Set click listeners
        mSignInButton.setOnClickListener(this);

        // Initialize FirebaseAuth
        mFirebaseAuth = FirebaseAuth.getInstance();
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
        String email = mEmail.getText().toString();
        String password = mPassword.getText().toString();
        String privateKeyBase64String = mSharedPreferences.getString(Constants.PRIVATE_KEY + email, "");
        if (StringUtils.isBlank(privateKeyBase64String)) {
            // Registration
            register(email, password);
        } else {
            // Login
            login(email, password);
        }
    }

    private void register(final String email, final String password) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Generate new key pair for user
                Crypto crypto = new VirgilCrypto();
                KeyPair keyPair = crypto.generateKeys();

                try {
                    // Register new user on chat server
                    Log.d(TAG, "Connect to server for signup");
                    Uri uri = Uri.parse(mBaseURL + "/signup").buildUpon()
                            .appendQueryParameter("email", email)
                            .appendQueryParameter("password", password)
                            .appendQueryParameter("key", ConvertionUtils.toBase64String(crypto.exportPrivateKey(keyPair.getPrivateKey())))
                            .build();

                    URL url = new URL(uri.toString());
                    String customToken = HttpUtils.execute(url, "GET", null, null, String.class);
                    Log.d(TAG, "Got custom token: " + customToken);

                    // Save private key for future use
                    mSharedPreferences.edit()
                            .putString(Constants.PRIVATE_KEY + email, ConvertionUtils.toBase64String(crypto.exportPrivateKey(keyPair.getPrivateKey())))
                            .commit();

                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.CUSTOM_TOKEN, customToken);
                    sendMessageToHandler(Constants.EVENTS.REGISTRATION_PASSED, bundle);
                } catch (Exception e) {
                    Log.e(TAG, "Error while registering new user", e);

                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.ERROR_MESSAGE, "Registration failed: " + e.getMessage());
                    sendMessageToHandler(Constants.EVENTS.REGISTRATION_FAILED, bundle);
                }
            }
        });
        thread.start();
    }

    private void login(final String email, final String password) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Log in on chat server
                    Log.d(TAG, "Connect to server for login");
                    Uri uri = Uri.parse(mBaseURL + "/login").buildUpon()
                            .appendQueryParameter("email", email)
                            .appendQueryParameter("password", password)
                            .build();

                    URL url = new URL(uri.toString());
                    String customToken = HttpUtils.execute(url, "GET", null, null, String.class);
                    Log.d(TAG, "Got custom token: " + customToken);

                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.CUSTOM_TOKEN, customToken);
                    sendMessageToHandler(Constants.EVENTS.LOGIN_PASSED, bundle);
                } catch (Exception e) {
                    Log.e(TAG, "Error while login", e);

                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.ERROR_MESSAGE, "Login failed: " + e.getMessage());
                    sendMessageToHandler(Constants.EVENTS.LOGIN_FAILED, bundle);
                }
            }

        });
        thread.start();
    }

    private void firebaseAuthWithCustomToken(String customToken) {
        Log.d(TAG, "firebaseAuthWithToken:" + customToken);
        mFirebaseAuth.signInWithCustomToken(customToken)
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
                            sendMessageToHandler(Constants.EVENTS.FIREBASE_SIGNIN_PASSED);
                        }
                    }
                });
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

    private void sendMessageToHandler(String event, Bundle bundle) {
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
                case Constants.EVENTS.LOGIN_PASSED:
                case Constants.EVENTS.REGISTRATION_PASSED:
                    String customToken = msg.getData().getString(Constants.CUSTOM_TOKEN);
                    firebaseAuthWithCustomToken(customToken);
                    break;
                case Constants.EVENTS.FIREBASE_SIGNIN_PASSED:
                    startMainActivity();
                    break;
                case Constants.EVENTS.LOGIN_FAILED:
                case Constants.EVENTS.REGISTRATION_FAILED:
                    String errorMessage = msg.getData().getString(Constants.ERROR_MESSAGE);
                    Toast.makeText(SignInActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

}
