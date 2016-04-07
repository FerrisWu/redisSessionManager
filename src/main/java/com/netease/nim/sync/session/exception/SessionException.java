package com.netease.nim.sync.session.exception;

public class SessionException extends RuntimeException {
	private static final long serialVersionUID = 5332450455602798146L;

	public SessionException() {
    }

    public SessionException(String message) {
        super(message);
    }

    public SessionException(String message, Throwable cause) {
        super(message, cause);
    }

    public SessionException(Throwable cause) {
        super(cause);
    }
}
