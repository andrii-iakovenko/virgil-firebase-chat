package com.google.firebase.codelab.friendlychat.utils;

/**
 * Created by Andrii Iakovenko.
 */

public interface Constants {
    String VIRGIL_TOKEN = "virgil_token";
    String VIRGIL_APP_ID = "virgil_app_id";
    String VIRGIL_APP_KEY = "virgil_app_key";
    String VIRGIL_APP_KEY_PWD = "virgil_app_key_pwd";
    String IDENTITY_TYPE = "firebase_user";
    String PRIVATE_KEY = "private_key_";

    String MESSAGES_CHILD = "messages";
    String USERS_CHILD = "users";

    String EVENT = "EVENT";

    interface EVENTS {
        String SIGNING_PASSED = "SIGNING_PASSED";
        String SIGNING_FAILED = "SIGNING_FAILED";
    }

}
