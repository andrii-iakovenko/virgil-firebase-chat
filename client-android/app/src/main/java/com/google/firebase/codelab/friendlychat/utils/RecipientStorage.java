package com.google.firebase.codelab.friendlychat.utils;

import com.virgilsecurity.sdk.client.model.CardModel;

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

    private Map<String, CardModel> recipients;

    public RecipientStorage() {
        this.recipients = new HashMap<>();
    }

    public void addRecipient(CardModel card) {
        synchronized (this) {
            recipients.put(card.getId(), card);
        }
    }

    public Set<CardModel> getAllRecipients() {
        synchronized (this) {
            return new HashSet<>(recipients.values());
        }
    }

    public boolean isExist(String cardId) {
        return recipients.containsKey(cardId);
    }

    public CardModel getRecipient(String cardId) {
        return recipients.get(cardId);
    }
}
