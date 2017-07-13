package com.virgilsecurity.firebasechat.server.model;

import java.beans.Transient;

/**
 * @author Andrii Iakovenko
 *
 */
public class User {
	
	private String cardId;

	private String email;

	private String password;

	public User() {
	}

	public User(String cardId, String email, String password) {
		this.cardId = cardId;
		this.email = email;
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Transient
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getCardId() {
		return cardId;
	}

	public void setCardId(String cardId) {
		this.cardId = cardId;
	}
}
