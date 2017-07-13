/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
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
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Indexables;
import com.google.firebase.appindexing.builders.PersonBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.codelab.friendlychat.fdb.ExecutorValueEventListener;
import com.google.firebase.codelab.friendlychat.model.FriendlyMessage;
import com.google.firebase.codelab.friendlychat.model.User;
import com.google.firebase.codelab.friendlychat.utils.Constants;
import com.google.firebase.codelab.friendlychat.utils.HttpUtils;
import com.google.firebase.codelab.friendlychat.utils.RecipientStorage;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.virgilsecurity.sdk.client.VirgilClient;
import com.virgilsecurity.sdk.client.exceptions.VirgilException;
import com.virgilsecurity.sdk.client.model.CardModel;
import com.virgilsecurity.sdk.client.model.dto.SearchCriteria;
import com.virgilsecurity.sdk.crypto.Crypto;
import com.virgilsecurity.sdk.crypto.PrivateKey;
import com.virgilsecurity.sdk.crypto.PublicKey;
import com.virgilsecurity.sdk.crypto.VirgilCrypto;
import com.virgilsecurity.sdk.device.DefaultDeviceManager;
import com.virgilsecurity.sdk.pfs.VirgilPFSClientContext;
import com.virgilsecurity.sdk.securechat.DefaultUserDataStorage;
import com.virgilsecurity.sdk.securechat.SecureChat;
import com.virgilsecurity.sdk.securechat.SecureChatContext;
import com.virgilsecurity.sdk.securechat.SecureSession;
import com.virgilsecurity.sdk.securechat.utils.SessionStateResolver;
import com.virgilsecurity.sdk.storage.VirgilKeyStorage;
import com.virgilsecurity.sdk.utils.ConvertionUtils;
import com.virgilsecurity.sdk.utils.StringUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener {

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        public TextView messageTextView;
        public TextView messengerTextView;
        public CircleImageView messengerImageView;

        public MessageViewHolder(View v) {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
            messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
        }
    }

    private static final String TAG = "MainActivity";
    private static final int REQUEST_INVITE = 1;
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 255;
    public static final String ANONYMOUS = "anonymous";
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    private static final String MESSAGE_URL = "http://friendlychat.firebase.google.com/message/";

    private String mUsername;
    private String mUseremail;
    private SharedPreferences mSharedPreferences;
    private String mBaseURL;

    private Button mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder> mFirebaseAdapter;
    private ProgressBar mProgressBar;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private FirebaseAnalytics mFirebaseAnalytics;
    private EditText mMessageEditText;
    private AdView mAdView;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private GoogleApiClient mGoogleApiClient;

    private Crypto mCrypto;
    private VirgilClient mVirgilClient;
    private SecureChatContext secureChatContext;
    private SecureChat secureChat;

    private CardModel mCard;
    private PublicKey mPublicKey;
    private PrivateKey mPrivateKey;
    private RecipientStorage mRecipients;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mBaseURL = mSharedPreferences.getString(Constants.BASE_URL, "");
        mUsername = ANONYMOUS;

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            //TODO load user name and email from your server
            mUsername = mFirebaseUser.getUid();
            mUseremail = mFirebaseUser.getUid();
        }

        // Virgil client
        String privateKeyStr = mSharedPreferences.getString(Constants.PRIVATE_KEY + mUseremail, "");
        if (StringUtils.isBlank(privateKeyStr)) {
            Log.i(TAG, "User's private key not found. No sence to start this activity");
            mFirebaseAuth.signOut();
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }
        mCrypto = new VirgilCrypto();
        mVirgilClient = new VirgilClient(mSharedPreferences.getString(Constants.VIRGIL_TOKEN, ""));

        mPrivateKey = mCrypto.importPrivateKey(ConvertionUtils.base64ToBytes(privateKeyStr));
        mPublicKey = mCrypto.extractPublicKey(mPrivateKey);
        mRecipients = new RecipientStorage();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>(
                FriendlyMessage.class,
                R.layout.item_message,
                MessageViewHolder.class,
                mFirebaseDatabaseReference.child(Constants.MESSAGES_CHILD)) {

            @Override
            protected FriendlyMessage parseSnapshot(DataSnapshot snapshot) {
                final FriendlyMessage friendlyMessage = super.parseSnapshot(snapshot);
                if (friendlyMessage != null) {
                    friendlyMessage.setId(snapshot.getKey());
                    Log.i(TAG, "Message text: " + friendlyMessage.getText());

                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (!mRecipients.isExist(friendlyMessage.getSenderCardId())) {
                                        updateRecipients();
                                    }
                                    if (!mRecipients.isExist(friendlyMessage.getSenderCardId())) {
                                        throw new Exception("Sender is unknown");
                                    }
                                    // Get message sent to me
                                    String encryptedMessage = friendlyMessage.getEncryptedMessages().get(mCard.getId());
                                    if (encryptedMessage == null) {
                                        friendlyMessage.setText(getString(R.string.message_could_not_be_dectypted));
                                    } else {
                                        //TODO Decrypt message
                                        CardModel senderCard = mRecipients.getRecipient(friendlyMessage.getSenderCardId());
                                        SecureSession session;
                                        if (SessionStateResolver.isInitiationMessage(encryptedMessage)) {
                                            // Start new session as responder
                                            session = secureChat.loadUpSession(senderCard, encryptedMessage);
                                        } else {
                                            session = secureChat.activeSession(senderCard.getId());
                                            if (session == null) {
                                                // Restore session from message
                                                session = secureChat.loadUpSession(senderCard, encryptedMessage);
                                            }
                                        }
                                        String decryptedMessage = session.decrypt(encryptedMessage);
                                        friendlyMessage.setText(decryptedMessage);
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "Message not encrypted " + friendlyMessage.getId() + " : " + e.getMessage());
                                    // Non-encrypted message or can't be decrypted
                                    friendlyMessage.setText(getString(R.string.message_could_not_be_dectypted));
                                }
                            }
                        });
                        thread.start();
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Interrupted thread", e);
                    }
                }
                return friendlyMessage;
            }

            @Override
            protected void populateViewHolder(MessageViewHolder viewHolder, FriendlyMessage friendlyMessage, int position) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                viewHolder.messageTextView.setText(friendlyMessage.getText());
                viewHolder.messengerTextView.setText(friendlyMessage.getName());
                if (friendlyMessage.getPhotoUrl() == null) {
                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,
                            R.drawable.ic_account_circle_black_36dp));
                } else {
                    Glide.with(MainActivity.this)
                            .load(friendlyMessage.getPhotoUrl())
                            .into(viewHolder.messengerImageView);
                }

                // write this message to the on-device index
                FirebaseAppIndex.getInstance().update(getMessageIndexable(friendlyMessage));

                // log a view action on it
                FirebaseUserActions.getInstance().end(getMessageViewAction(friendlyMessage));
            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
                // to the bottom of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) && lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);

        // Initialize and request AdMob ad.
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Initialize Firebase Measurement.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Initialize Firebase Remote Config.
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        // Define Firebase Remote Config Settings.
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings =
                new FirebaseRemoteConfigSettings.Builder()
                        .setDeveloperModeEnabled(true)
                        .build();

        // Define default config values. Defaults are used when fetched config values are not
        // available. Eg: if an error occurred fetching values from the server.
        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put("friendly_msg_length", DEFAULT_MSG_LENGTH_LIMIT);

        // Apply config settings and default values.
        mFirebaseRemoteConfig.setConfigSettings(firebaseRemoteConfigSettings);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);

        // Fetch remote config.
        fetchConfig();

        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
                .getInt(CodelabPreferences.FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mSendButton = (Button) findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage(mMessageEditText.getText().toString());
                mMessageEditText.setText("");
                mFirebaseAnalytics.logEvent(MESSAGE_SENT_EVENT, null);
            }
        });

        initSecureChat();
    }

    private void sendMessage(final String message) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                updateRecipients();

                FriendlyMessage friendlyMessage = new FriendlyMessage(mCard.getId(), mUsername, mUseremail, null);

                // Encrypt message for every user in chat
                for (CardModel card : mRecipients.getAllRecipients()) {
                    try {
                        // Is session active?
                        SecureSession session = secureChat.activeSession(card.getId());
                        if (session == null) {
                            // No session yet, start new one
                            session = secureChat.startNewSession(card, null);
                        }
                        String encryptedMessage = session.encrypt(message);
                        friendlyMessage.addEncryptedMessage(card.getId(), encryptedMessage);
                    }
                    catch (Exception e) {
                        Log.e(TAG, "Can't encrypt message for " + card.getId());
                    }
                }

                // Send message with Firebase
                mFirebaseDatabaseReference.child(Constants.MESSAGES_CHILD).push().setValue(friendlyMessage);
            }
        });
        thread.start();
    }

    private Action getMessageViewAction(FriendlyMessage friendlyMessage) {
        return new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(friendlyMessage.getName(), MESSAGE_URL.concat(friendlyMessage.getId()))
                .setMetadata(new Action.Metadata.Builder().setUpload(false))
                .build();
    }

    private Indexable getMessageIndexable(FriendlyMessage friendlyMessage) {
        PersonBuilder sender = Indexables.personBuilder()
                .setIsSelf(mUsername == friendlyMessage.getName())
                .setName(friendlyMessage.getName())
                .setUrl(MESSAGE_URL.concat(friendlyMessage.getId() + "/sender"));

        PersonBuilder recipient = Indexables.personBuilder()
                .setName(mUsername)
                .setUrl(MESSAGE_URL.concat(friendlyMessage.getId() + "/recipient"));

        Indexable messageToIndex = Indexables.messageBuilder()
                .setName(friendlyMessage.getText())
                .setUrl(MESSAGE_URL.concat(friendlyMessage.getId()))
                .setSender(sender)
                .setRecipient(recipient)
                .build();

        return messageToIndex;
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                mFirebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                mFirebaseUser = null;
                mUsername = ANONYMOUS;
                startActivity(new Intent(this, SignInActivity.class));
                return true;
            case R.id.fresh_config_menu:
                fetchConfig();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Fetch the config to determine the allowed length of messages.
    public void fetchConfig() {
        long cacheExpiration = 3600; // 1 hour in seconds
        // If developer mode is enabled reduce cacheExpiration to 0 so that each fetch goes to the
        // server. This should not be used in release builds.
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Make the fetched config available via FirebaseRemoteConfig get<type> calls.
                        mFirebaseRemoteConfig.activateFetched();
                        applyRetrievedLengthLimit();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // There has been an error fetching the config
                        Log.w(TAG, "Error fetching config: " + e.getMessage());
                        applyRetrievedLengthLimit();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                // Use Firebase Measurement to log that invitation was sent.
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_sent");

                // Check how many invitations were sent and log.
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                Log.d(TAG, "Invitations sent: " + ids.length);
            } else {
                // Use Firebase Measurement to log that invitation was not sent
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_not_sent");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, payload);

                // Sending failed or it was canceled, show failure message to the user
                Log.d(TAG, "Failed to send invitation.");
            }
        }
    }

    /**
     * Apply retrieved length limit to edit text field. This result may be fresh from the server or it may be from
     * cached values.
     */
    private void applyRetrievedLengthLimit() {
        Long friendly_msg_length = mFirebaseRemoteConfig.getLong("friendly_msg_length");
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(friendly_msg_length.intValue())});
        Log.d(TAG, "FML is: " + friendly_msg_length);
    }

    private void updateRecipients() {

        try {
            // Get users from server
            URL url = new URL(mBaseURL + "/users");

            List<User> users = new ArrayList<>(Arrays.asList(HttpUtils.execute(url, "GET", null, null, User[].class)));
            if (users.isEmpty()) {
                if (mRecipients.isExist(mCard.getId())) {
                    return;
                } else {
                    users.add(new User(mCard.getId(), mUseremail, mUsername));
                }
            }

            Set<String> cardIds = new HashSet<>();
            Set<String> identities = new HashSet<>();
            for (User user : users) {
                identities.add(user.getEmail());
                cardIds.add(user.getCardId());
            }

            // Find Virgil Cards for users
            SearchCriteria criteria = SearchCriteria.byIdentities(identities);
            List<CardModel> cards = mVirgilClient.searchCards(criteria);
            if (!cards.isEmpty()) {
                for (CardModel card : cards) {
                    if (!cardIds.contains(card.getId())) {
                        // Skip cards which are not registered
                        continue;
                    }
                    Log.d(TAG, "Add Card for " + card.getSnapshotModel().getIdentity() + " : " + card.getId());
                    mRecipients.addRecipient(card);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "An error occurred while updating recipients", e);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    private void initSecureChat() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String cardId = mSharedPreferences.getString(Constants.MY_CARD_ID + mUseremail, "");
                mCard = mVirgilClient.getCard(cardId);

                updateRecipients();

                VirgilPFSClientContext ctx = new VirgilPFSClientContext(mSharedPreferences.getString(Constants.VIRGIL_TOKEN, ""));
                secureChatContext = new SecureChatContext(mCard, mPrivateKey, mCrypto, ctx);
                secureChatContext.setKeyStorage(new VirgilKeyStorage(Environment.getExternalStorageDirectory() + "/keyStorage"));
                secureChatContext.setDeviceManager(new DefaultDeviceManager());
                secureChatContext.setUserDefaults(new DefaultUserDataStorage());
                secureChat = new SecureChat(secureChatContext);

                secureChat.initialize();
            }
        });
        thread.start();

    }

}
