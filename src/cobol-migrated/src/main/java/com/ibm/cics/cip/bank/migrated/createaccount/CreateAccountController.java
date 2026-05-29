/*
 *
 *    Copyright IBM Corp. 2023
 *
 *    Migrated from COBOL source: BNK1CAC.cbl (Create Account - BMS screen)
 *    Original author: Jon Collett
 *
 *    This class is the Java 21 equivalent of the BNK1CAC COBOL program.
 *    It handles the Create Account BMS screen logic including:
 *    - Input validation (customer number, account type, interest rate, overdraft)
 *    - Delegation to the account creation service (equivalent to LINK CREACC)
 *    - Error handling and failure code interpretation
 *
 */
package com.ibm.cics.cip.bank.migrated.createaccount;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CreateAccountController {

    private static final Logger logger = Logger.getLogger(
            CreateAccountController.class.getName());

    private static final BigDecimal MAX_INTEREST_RATE = new BigDecimal("9999.99");
    private static final int CUSTOMER_NUMBER_LENGTH = 10;
    private static final String SORT_CODE_INITIAL = "000000";
    private static final String ACCOUNT_NUMBER_INITIAL = "00000000";

    private final AccountCreationService accountCreationService;

    public CreateAccountController(AccountCreationService accountCreationService) {
        this.accountCreationService = Objects.requireNonNull(
                accountCreationService, "accountCreationService must not be null");
    }

    public CreateAccountResult processCreateAccount(CreateAccountRequest request) {
        logger.entering(getClass().getName(), "processCreateAccount");

        ValidationResult validation = validateInput(request);
        if (!validation.valid()) {
            logger.log(Level.INFO, "Validation failed: {0}", validation.errorMessage());
            return CreateAccountResult.failure(validation.errorMessage());
        }

        CreateAccountResult result = createAccount(request);

        logger.exiting(getClass().getName(), "processCreateAccount");
        return result;
    }

    public ValidationResult validateInput(CreateAccountRequest request) {
        if (request == null) {
            return ValidationResult.error("Request must not be null", "request");
        }

        ValidationResult customerValidation = validateCustomerNumber(
                request.customerNumber());
        if (!customerValidation.valid()) {
            return customerValidation;
        }

        ValidationResult accountTypeValidation = validateAccountType(
                request.accountType());
        if (!accountTypeValidation.valid()) {
            return accountTypeValidation;
        }

        ValidationResult interestRateValidation = validateInterestRate(
                request.interestRate());
        if (!interestRateValidation.valid()) {
            return interestRateValidation;
        }

        ValidationResult overdraftValidation = validateOverdraftLimit(
                request.overdraftLimit());
        if (!overdraftValidation.valid()) {
            return overdraftValidation;
        }

        return ValidationResult.ok();
    }

    private ValidationResult validateCustomerNumber(String customerNumber) {
        if (customerNumber == null || customerNumber.isBlank()) {
            return ValidationResult.error(
                    "Please enter a 10 digit Customer Number",
                    "customerNumber");
        }

        String trimmed = customerNumber.trim();
        if (trimmed.length() != CUSTOMER_NUMBER_LENGTH) {
            return ValidationResult.error(
                    "Please enter a 10 digit Customer Number",
                    "customerNumber");
        }

        if (!trimmed.chars().allMatch(Character::isDigit)) {
            return ValidationResult.error(
                    "Please enter a numeric Customer number",
                    "customerNumber");
        }

        return ValidationResult.ok();
    }

    private ValidationResult validateAccountType(String accountType) {
        if (accountType == null || accountType.isBlank()) {
            return ValidationResult.error(
                    "Account Type should be ISA,CURRENT,LOAN,SAVING or MORTGAGE",
                    "accountType");
        }

        if (AccountType.fromString(accountType).isEmpty()) {
            return ValidationResult.error(
                    "Account Type should be ISA,CURRENT,LOAN,SAVING or MORTGAGE",
                    "accountType");
        }

        return ValidationResult.ok();
    }

    private ValidationResult validateInterestRate(BigDecimal interestRate) {
        if (interestRate == null) {
            return ValidationResult.error(
                    "Please supply a numeric interest rate",
                    "interestRate");
        }

        if (interestRate.scale() > 2) {
            return ValidationResult.error(
                    "Only up to two decimal places are supported",
                    "interestRate");
        }

        if (interestRate.compareTo(BigDecimal.ZERO) < 0) {
            return ValidationResult.error(
                    "Please supply a zero or positive interest rate",
                    "interestRate");
        }

        if (interestRate.compareTo(MAX_INTEREST_RATE) > 0) {
            return ValidationResult.error(
                    "Please supply an interest rate less than 9999.99%",
                    "interestRate");
        }

        return ValidationResult.ok();
    }

    private ValidationResult validateOverdraftLimit(int overdraftLimit) {
        if (overdraftLimit < 0) {
            return ValidationResult.error(
                    "Overdraft Limit must be numeric positive int",
                    "overdraftLimit");
        }

        return ValidationResult.ok();
    }

    private CreateAccountResult createAccount(CreateAccountRequest request) {
        AccountType resolvedType = AccountType.fromString(request.accountType())
                .orElseThrow();

        CreateAccountParams params = new CreateAccountParams(
                request.customerNumber().trim(),
                SORT_CODE_INITIAL,
                ACCOUNT_NUMBER_INITIAL,
                resolvedType.getValue(),
                request.interestRate(),
                request.overdraftLimit());

        CreateAccountResult result = accountCreationService.createAccount(params);

        if (!result.success()) {
            String failureMessage = resolveFailureMessage(result.failureMessage());
            return CreateAccountResult.failure(failureMessage);
        }

        return result;
    }

    private String resolveFailureMessage(String failCode) {
        if (failCode == null || failCode.isBlank()) {
            return "The account was not created.";
        }

        return switch (failCode) {
            case "1" -> "The supplied customer number does not exist.";
            case "2" -> "The customer data cannot be accessed, unable to create account.";
            case "3" -> "Account record creation failed. (unable to ENQ ACCOUNT NC).";
            case "4" -> "Account record creation failed, (unable to increment ACCOUNT NC).";
            case "5" -> "Account record creation failed, (unable to restore ACCOUNT NC).";
            case "6" -> "Account record creation failed, (unable to WRITE to ACCOUNT file).";
            case "7" -> "Account record creation failed, (unable to INSERT into ACCOUNT).";
            case "8" -> "Account record creation failed, (too many accounts).";
            case "9" -> "Account record creation failed, unable to count accounts.";
            case "A" -> "Account record creation failed, account type unsupported.";
            default -> "The account was not created.";
        };
    }
}
