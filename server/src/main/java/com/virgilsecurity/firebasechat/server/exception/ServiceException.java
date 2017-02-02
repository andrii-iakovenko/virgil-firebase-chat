package com.virgilsecurity.firebasechat.server.exception;

public class ServiceException extends RuntimeException {

	private static final long serialVersionUID = -3743518986775318580L;
	private int code;

	public ServiceException(int code, String message) {
		super(message);
		this.code = code;
	}

	public int getCode() {
		return code;
	}

}
