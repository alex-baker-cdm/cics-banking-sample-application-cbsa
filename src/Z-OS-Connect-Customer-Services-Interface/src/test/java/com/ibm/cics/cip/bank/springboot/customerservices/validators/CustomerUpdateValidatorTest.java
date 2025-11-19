/*                                                                        */
/* Copyright IBM Corp. 2023                                               */
/*                                                                        */
package com.ibm.cics.cip.bank.springboot.customerservices.validators;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ibm.cics.cip.bank.springboot.customerservices.jsonclasses.updatecustomer.UpdateCustomerForm;
import com.ibm.cics.cip.bank.springboot.customerservices.validators.exceptions.CustomerValidationException;

class CustomerUpdateValidatorTest
{

	private CustomerUpdateValidator validator;

	private UpdateCustomerForm form;


	@BeforeEach
	void setUp()
	{
		validator = new CustomerUpdateValidator();
		form = new UpdateCustomerForm();
		form.setCustNumber("0000000001");
	}


	@Test
	void testValidateNotBothSpaces_BothSpaces_ThrowsException()
	{
		form.setCustName("   ");
		form.setCustAddress("   ");

		CustomerValidationException exception = assertThrows(
				CustomerValidationException.class,
				() -> validator.validateCustomerUpdate(form, "Original Name",
						"Original Address"));

		assertEquals("Name and address cannot both be spaces",
				exception.getMessage());
	}


	@Test
	void testValidateNotBothSpaces_OnlyNameSpaces_DoesNotThrow()
	{
		form.setCustName("   ");
		form.setCustAddress("123 Main St");

		assertDoesNotThrow(() -> validator.validateCustomerUpdate(form,
				"Original Name", "Original Address"));
	}


	@Test
	void testValidateNotBothSpaces_OnlyAddressSpaces_DoesNotThrow()
	{
		form.setCustName("John Doe");
		form.setCustAddress("   ");

		assertDoesNotThrow(() -> validator.validateCustomerUpdate(form,
				"Original Name", "Original Address"));
	}


	@Test
	void testValidateNotBothSpaces_NeitherSpaces_DoesNotThrow()
	{
		form.setCustName("John Doe");
		form.setCustAddress("123 Main St");

		assertDoesNotThrow(() -> validator.validateCustomerUpdate(form,
				"Original Name", "Original Address"));
	}


	@Test
	void testValidateNotBothStartWithSpace_BothStartWithSpace_ThrowsException()
	{
		form.setCustName(" John Doe");
		form.setCustAddress(" 123 Main St");

		CustomerValidationException exception = assertThrows(
				CustomerValidationException.class,
				() -> validator.validateCustomerUpdate(form, "Original Name",
						"Original Address"));

		assertEquals("Name and address cannot both start with a space",
				exception.getMessage());
	}


	@Test
	void testValidateNotBothStartWithSpace_OnlyNameStartsWithSpace_DoesNotThrow()
	{
		form.setCustName(" John Doe");
		form.setCustAddress("123 Main St");

		assertDoesNotThrow(() -> validator.validateCustomerUpdate(form,
				"Original Name", "Original Address"));
	}


	@Test
	void testValidateNotBothStartWithSpace_OnlyAddressStartsWithSpace_DoesNotThrow()
	{
		form.setCustName("John Doe");
		form.setCustAddress(" 123 Main St");

		assertDoesNotThrow(() -> validator.validateCustomerUpdate(form,
				"Original Name", "Original Address"));
	}


	@Test
	void testValidateNotBothStartWithSpace_NeitherStartsWithSpace_DoesNotThrow()
	{
		form.setCustName("John Doe");
		form.setCustAddress("123 Main St");

		assertDoesNotThrow(() -> validator.validateCustomerUpdate(form,
				"Original Name", "Original Address"));
	}


	@Test
	void testValidateAtLeastOneFieldChanged_NoChanges_ThrowsException()
	{
		form.setCustName("John Doe");
		form.setCustAddress("123 Main St");

		CustomerValidationException exception = assertThrows(
				CustomerValidationException.class,
				() -> validator.validateCustomerUpdate(form, "John Doe",
						"123 Main St"));

		assertEquals("At least one field (name or address) must be changed",
				exception.getMessage());
	}


	@Test
	void testValidateAtLeastOneFieldChanged_NameChanged_DoesNotThrow()
	{
		form.setCustName("Jane Doe");
		form.setCustAddress("123 Main St");

		assertDoesNotThrow(() -> validator.validateCustomerUpdate(form,
				"John Doe", "123 Main St"));
	}


	@Test
	void testValidateAtLeastOneFieldChanged_AddressChanged_DoesNotThrow()
	{
		form.setCustName("John Doe");
		form.setCustAddress("456 Oak Ave");

		assertDoesNotThrow(() -> validator.validateCustomerUpdate(form,
				"John Doe", "123 Main St"));
	}


	@Test
	void testValidateAtLeastOneFieldChanged_BothChanged_DoesNotThrow()
	{
		form.setCustName("Jane Doe");
		form.setCustAddress("456 Oak Ave");

		assertDoesNotThrow(() -> validator.validateCustomerUpdate(form,
				"John Doe", "123 Main St"));
	}


	@Test
	void testValidateAtLeastOneFieldChanged_NoOriginalData_DoesNotThrow()
	{
		form.setCustName("John Doe");
		form.setCustAddress("123 Main St");

		assertDoesNotThrow(
				() -> validator.validateCustomerUpdate(form, "", ""));
	}


	@Test
	void testValidateImmutableFields_CustomerNumberChanged_ThrowsException()
	{
		form.setCustNumber("0000000002");

		CustomerValidationException exception = assertThrows(
				CustomerValidationException.class,
				() -> validator.validateImmutableFields(form, "0000000001",
						"123456", "19900101", 700));

		assertEquals("Customer number cannot be modified",
				exception.getMessage());
	}


	@Test
	void testValidateImmutableFields_DateOfBirthChanged_ThrowsException()
	{
		form.setCustDoB("1995-01-01");

		CustomerValidationException exception = assertThrows(
				CustomerValidationException.class,
				() -> validator.validateImmutableFields(form, "0000000001",
						"123456", "01011990", 700));

		assertEquals("Date of birth cannot be modified",
				exception.getMessage());
	}


	@Test
	void testValidateImmutableFields_CreditScoreChanged_ThrowsException()
	{
		form.setCustCreditScore(800);

		CustomerValidationException exception = assertThrows(
				CustomerValidationException.class,
				() -> validator.validateImmutableFields(form, "0000000001",
						"123456", "19900101", 700));

		assertEquals("Credit score cannot be modified",
				exception.getMessage());
	}


	@Test
	void testValidateImmutableFields_NoChanges_DoesNotThrow()
	{
		form.setCustNumber("0000000001");
		form.setCustDoB("1990-01-01");
		form.setCustCreditScore(700);

		assertDoesNotThrow(() -> validator.validateImmutableFields(form,
				"0000000001", "123456", "01011990", 700));
	}


	@Test
	void testValidateImmutableFields_OnlyMutableFieldsChanged_DoesNotThrow()
	{
		form.setCustNumber("0000000001");
		form.setCustName("Jane Doe");
		form.setCustAddress("456 Oak Ave");
		form.setCustDoB("1990-01-01");
		form.setCustCreditScore(700);

		assertDoesNotThrow(() -> validator.validateImmutableFields(form,
				"0000000001", "123456", "01011990", 700));
	}


	@Test
	void testComplexScenario_ValidUpdate_DoesNotThrow()
	{
		form.setCustName("Jane Doe");
		form.setCustAddress("456 Oak Ave");

		assertDoesNotThrow(() -> validator.validateCustomerUpdate(form,
				"John Doe", "123 Main St"));
	}


	@Test
	void testComplexScenario_NameOnlyUpdate_DoesNotThrow()
	{
		form.setCustName("Jane Doe");
		form.setCustAddress("123 Main St");

		assertDoesNotThrow(() -> validator.validateCustomerUpdate(form,
				"John Doe", "123 Main St"));
	}


	@Test
	void testComplexScenario_AddressOnlyUpdate_DoesNotThrow()
	{
		form.setCustName("John Doe");
		form.setCustAddress("456 Oak Ave");

		assertDoesNotThrow(() -> validator.validateCustomerUpdate(form,
				"John Doe", "123 Main St"));
	}


	@Test
	void testEdgeCase_EmptyStrings_DoesNotThrow()
	{
		form.setCustName("");
		form.setCustAddress("");

		assertDoesNotThrow(
				() -> validator.validateCustomerUpdate(form, "", ""));
	}


	@Test
	void testEdgeCase_SingleSpace_BothFields_ThrowsException()
	{
		form.setCustName(" ");
		form.setCustAddress(" ");

		CustomerValidationException exception = assertThrows(
				CustomerValidationException.class,
				() -> validator.validateCustomerUpdate(form, "Original Name",
						"Original Address"));

		assertEquals("Name and address cannot both be spaces",
				exception.getMessage());
	}


	@Test
	void testEdgeCase_MultipleSpaces_BothFields_ThrowsException()
	{
		form.setCustName("     ");
		form.setCustAddress("          ");

		CustomerValidationException exception = assertThrows(
				CustomerValidationException.class,
				() -> validator.validateCustomerUpdate(form, "Original Name",
						"Original Address"));

		assertEquals("Name and address cannot both be spaces",
				exception.getMessage());
	}


	@Test
	void testEdgeCase_TabsAndSpaces_BothFields_ThrowsException()
	{
		form.setCustName("  \t  ");
		form.setCustAddress("  \t  ");

		CustomerValidationException exception = assertThrows(
				CustomerValidationException.class,
				() -> validator.validateCustomerUpdate(form, "Original Name",
						"Original Address"));

		assertEquals("Name and address cannot both be spaces",
				exception.getMessage());
	}
}
