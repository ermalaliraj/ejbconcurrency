package com.ea.ejbconcurrency.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class BusinessException extends RuntimeException {

	private static final long serialVersionUID = -20379031643854514L;

	public BusinessException(String message, Throwable cause) {
		super(message, cause);
	}

	public BusinessException(String message) {
		super(message);
	}

	public BusinessException(Throwable cause) {
		super(cause);
	}

}