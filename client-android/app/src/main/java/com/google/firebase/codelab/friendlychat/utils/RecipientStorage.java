package com.google.firebase.codelab.friendlychat.utils;

import com.virgilsecurity.sdk.crypto.PublicKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Andrii Iakovenko.
 */

public class RecipientStorage {

    private Map<String, Set<PublicKey>> recipients;

    public RecipientStorage() {
        this.recipients = new HashMap<>();
    }

    public void addRecipient(String identity, PublicKey publicKey) {
        synchronized (this) {
            Set<PublicKey> keys;
            if (recipients.containsKey(identity)) {
                keys = recipients.get(identity);
            } else {
                keys = new HashSet<>();
                recipients.put(identity, keys);
            }
            keys.add(publicKey);
        }
    }

    public List<PublicKey> getAllRecipients() {
        List<PublicKey> allKeys = new ArrayList<>();
        synchronized (this) {
            for (Set<PublicKey> publicKeys : recipients.values()) {
                allKeys.addAll(publicKeys);
            }
        }
        return allKeys;
    }
}
