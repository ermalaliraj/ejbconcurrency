package com.ea.ejbconcurrency.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class ServerException extends RuntimeException {

	private static final long serialVersionUID = -1972201540461744391L;

	public ServerException(String message) {
		super(message);
	}

	public ServerException(Exception e) {
		super(e);
	}

	public ServerException(String msg, Exception e) {
		super(msg, e);
	}

}