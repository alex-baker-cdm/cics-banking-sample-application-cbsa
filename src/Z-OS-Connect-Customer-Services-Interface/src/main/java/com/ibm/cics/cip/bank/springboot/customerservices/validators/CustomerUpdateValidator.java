/*                                                                        */
/* Copyright IBM Corp. 2023                                               */
/*                                                                        */
package com.ibm.cics.cip.bank.springboot.customerservices.validators;

import com.ibm.cics.cip.bank.springboot.customerservices.jsonclasses.updatecustomer.UpdateCustomerForm;
import com.ibm.cics.cip.bank.springboot.customerservices.validators.exceptions.CustomerValidationException;

public class CustomerUpdateValidator
{

	static final String COPYRIGHT = "Copyright IBM Corp. 2023";


	public void validateCustomerUpdate(UpdateCustomerForm form,
			String originalName, String originalAddress)
			throws CustomerValidationException
	{
		String newName = form.getCustName();
		String newAddress = form.getCustAddress();

		validateNotBothSpaces(newName, newAddress);
		validateNotBothStartWithSpace(newName, newAddress);
		validateAtLeastOneFieldChanged(newName, newAddress, originalName,
				originalAddress);
	}


	public void validateImmutableFields(UpdateCustomerForm form,
			String originalCustNumber, String originalSortcode,
			String originalDoB, int originalCreditScore)
			throws CustomerValidationException
	{
		if (form.getCustNumber() != null
				&& !form.getCustNumber().equals(originalCustNumber))
		{
			throw new CustomerValidationException(
					"Customer number cannot be modified");
		}

		if (form.getCustDoB() != null && !form.getCustDoB().isEmpty()
				&& !form.getCustDoB().equals(originalDoB))
		{
			throw new CustomerValidationException(
					"Date of birth cannot be modified");
		}

		if (form.getCustCreditScore() != 0
				&& form.getCustCreditScore() != originalCreditScore)
		{
			throw new CustomerValidationException(
					"Credit score cannot be modified");
		}
	}


	private void validateNotBothSpaces(String name, String address)
			throws CustomerValidationException
	{
		if (isOnlySpaces(name) && isOnlySpaces(address))
		{
			throw new CustomerValidationException(
					"Name and address cannot both be spaces");
		}
	}


	private void validateNotBothStartWithSpace(String name, String address)
			throws CustomerValidationException
	{
		if (startsWithSpace(name) && startsWithSpace(address))
		{
			throw new CustomerValidationException(
					"Name and address cannot both start with a space");
		}
	}


	private void validateAtLeastOneFieldChanged(String newName,
			String newAddress, String originalName, String originalAddress)
			throws CustomerValidationException
	{
		if (originalName == null || originalAddress == null
				|| (originalName.isEmpty() && originalAddress.isEmpty()))
		{
			return;
		}

		boolean nameChanged = !newName.equals(originalName);
		boolean addressChanged = !newAddress.equals(originalAddress);

		if (!nameChanged && !addressChanged)
		{
			throw new CustomerValidationException(
					"At least one field (name or address) must be changed");
		}
	}


	private boolean isOnlySpaces(String value)
	{
		return value != null && !value.isEmpty() && value.trim().isEmpty();
	}


	private boolean startsWithSpace(String value)
	{
		return value != null && !value.isEmpty() && value.charAt(0) == ' ';
	}
}
