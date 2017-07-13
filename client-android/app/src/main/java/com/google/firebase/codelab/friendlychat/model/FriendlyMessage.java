/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.codelab.friendlychat.model;

import java.util.HashMap;
import java.util.Map;

public class FriendlyMessage {

    private String id;
    private String senderCardId;
    private Map<String, String> encryptedMessages;
    private String text;
    private String name;
    private String email;
    private String photoUrl;

    public FriendlyMessage() {
        encryptedMessages = new HashMap<>();
    }

    public FriendlyMessage(String senderCardId, String name, String email, String photoUrl) {
        this();
        this.senderCardId = senderCardId;
        this.name = name;
        this.email = email;
        this.photoUrl = photoUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void addEncryptedMessage(String recipientCardId, String encryptedMessage) {
        encryptedMessages.put(recipientCardId, encryptedMessage);
    }

    public Map<String, String> getEncryptedMessages() {
        return encryptedMessages;
    }

    public void setEncryptedMessages(Map<String, String> encryptedMessages) {
        this.encryptedMessages = encryptedMessages;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSenderCardId() {
        return senderCardId;
    }

    public void setSenderCardId(String senderCardId) {
        this.senderCardId = senderCardId;
    }
}
