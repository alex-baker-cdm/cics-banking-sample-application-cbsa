/*                                                                        */
/* Copyright IBM Corp. 2023                                               */
/*                                                                        */
package com.ibm.cics.cip.bank.springboot.customerservices.validators.exceptions;

public class CustomerValidationException extends Exception
{

	private static final long serialVersionUID = 1L;

	static final String COPYRIGHT = "Copyright IBM Corp. 2023";


	public CustomerValidationException(String message)
	{
		super(message);
	}


	public CustomerValidationException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
