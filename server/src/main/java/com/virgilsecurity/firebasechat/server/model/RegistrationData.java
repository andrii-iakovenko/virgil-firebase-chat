package com.virgilsecurity.firebasechat.server.model;

public class RegistrationData {

	private String customToken;

	private String cardId;

	public RegistrationData() {
	}

	public RegistrationData(String customToken, String cardId) {
		this.customToken = customToken;
		this.cardId = cardId;
	}

	public String getCustomToken() {
		return customToken;
	}

	public void setCustomToken(String customToken) {
		this.customToken = customToken;
	}

	public String getCardId() {
		return cardId;
	}

	public void setCardId(String cardId) {
		this.cardId = cardId;
	}

}
