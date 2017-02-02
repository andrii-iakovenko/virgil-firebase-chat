package com.virgilsecurity.firebasechat.server.model;

import java.beans.Transient;

/**
 * @author Andrii Iakovenko
 *
 */
public class User {

	private String email;

	private String password;

	public User() {
	}

	public User(String email, String password) {
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
}
