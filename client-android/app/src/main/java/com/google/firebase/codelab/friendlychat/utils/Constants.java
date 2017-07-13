package com.google.firebase.codelab.friendlychat.utils;

/**
 * Created by Andrii Iakovenko.
 */

public interface Constants {
    String VIRGIL_TOKEN = "virgil_token";
    String BASE_URL = "base_url";
    String IDENTITY_TYPE = "firebase_user";
    String PRIVATE_KEY = "private_key_";
    String MY_CARD_ID = "virgil_card_";

    String MESSAGES_CHILD = "messages";

    String EVENT = "EVENT";
    String CUSTOM_TOKEN = "CUSTOM_TOKEN";
    String ERROR_MESSAGE = "ERROR_MESSAGE";

    interface EVENTS {
        String LOGIN_PASSED = "LOGING_PASSED";
        String LOGIN_FAILED = "LOGIN_FAILED ";
        String REGISTRATION_PASSED = "REGISTRATION_PASSED";
        String REGISTRATION_FAILED = "REGISTRATION_FAILED";
        String FIREBASE_SIGNIN_PASSED = "FIREBASE_SIGNIN_PASSED";

    }

}
