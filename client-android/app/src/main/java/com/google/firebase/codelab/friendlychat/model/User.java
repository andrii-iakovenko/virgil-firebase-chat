package com.google.firebase.codelab.friendlychat.model;

/**
 * Created by Andrii Iakovenko.
 */

public class User {

    private String cardId;
    private String email;
    private String name;

    public User() {
    }

    public User(String cardId, String email, String name) {
        this.cardId = cardId;
        this.email = email;
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (cardId != null ? !cardId.equals(user.cardId) : user.cardId != null) return false;
        if (email != null ? !email.equals(user.email) : user.email != null) return false;
        return name != null ? name.equals(user.name) : user.name == null;

    }

    @Override
    public int hashCode() {
        int result = cardId != null ? cardId.hashCode() : 0;
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
