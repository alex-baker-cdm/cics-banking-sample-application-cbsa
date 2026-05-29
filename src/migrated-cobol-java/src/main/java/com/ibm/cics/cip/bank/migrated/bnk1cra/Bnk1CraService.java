/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNK1CRA.cbl (Credit/Debit Account - BMS screen)
 * Original Author: Jon Collett
 *
 * This class preserves all business logic from the original COBOL program:
 * - Input validation (EDIT-DATA section)
 * - Amount validation (VALIDATE-AMOUNT section)
 * - Credit/Debit processing (UPD-CRED-DATA section)
 * - Error handling and messaging
 *
 * CICS-specific screen handling (SEND MAP, RECEIVE MAP, pseudo-conversational
 * flow) is replaced by a service method pattern suitable for modern callers.
 */
package com.ibm.cics.cip.bank.migrated.bnk1cra;

import java.math.BigDecimal;

import com.ibm.cics.cip.bank.migrated.bnk1cra.AccountOperationService.AccountOperationRequest;
import com.ibm.cics.cip.bank.migrated.bnk1cra.AccountOperationService.AccountOperationResponse;
import com.ibm.cics.cip.bank.migrated.bnk1cra.AmountValidator.ValidationResult;

/**
 * Java 21 equivalent of the COBOL program BNK1CRA.
 *
 * <p>BNK1CRA is the Credit/Debit program in the BANKING application BMS suite.
 * It handles user input for crediting or debiting an account, validates the input,
 * delegates to the DBCRFUN sub-program for the actual database operation, and
 * returns the result with updated balances.
 *
 * <p>The CICS pseudo-conversational screen flow has been replaced with a
 * stateless service method {@link #processCreditDebit(CreditDebitRequest, OriginContext)}.
 */
public class Bnk1CraService {

    private final AccountOperationService accountOperationService;

    /**
     * Context about the originating session/user, equivalent to the
     * CICS INQUIRE ASSOCIATION data in SUBPGM-ORIGIN.
     *
     * @param applId       application ID (SUBPGM-APPLID)
     * @param userId       user ID (SUBPGM-USERID)
     * @param facilityName facility name (SUBPGM-FACILITY-NAME)
     * @param networkId    network ID (SUBPGM-NETWRK-ID)
     * @param facilityType facility type (SUBPGM-FACILTYPE)
     */
    public record OriginContext(
            String applId,
            String userId,
            String facilityName,
            String networkId,
            int facilityType
    ) {
    }

    public Bnk1CraService(AccountOperationService accountOperationService) {
        this.accountOperationService = accountOperationService;
    }

    /**
     * Processes a credit or debit request.
     * This method encapsulates the full flow of the original COBOL program:
     * PROCESS-MAP -> EDIT-DATA -> UPD-CRED-DATA.
     *
     * @param request the credit/debit request from the screen
     * @param origin  the originating context (user, application, facility info)
     * @return the result of the operation
     */
    public CreditDebitResult processCreditDebit(CreditDebitRequest request,
                                                OriginContext origin) {
        String validationError = validateInput(request);
        if (validationError != null) {
            return CreditDebitResult.failure(validationError);
        }

        return applyDebitCredit(request, origin);
    }

    /**
     * Validates the input fields from the screen.
     * Equivalent to the EDIT-DATA section in the COBOL source.
     *
     * @param request the request to validate
     * @return error message if invalid, null if valid
     */
    String validateInput(CreditDebitRequest request) {
        String accountNumber = request.accountNumber();

        if (accountNumber == null || accountNumber.isBlank()) {
            return "Please enter an account number.";
        }

        String deeditedAccount = deEditField(accountNumber);

        if (!isNumeric(deeditedAccount)) {
            return "Please enter an account number.";
        }

        if (isAllZeros(deeditedAccount)) {
            return "Please enter a non zero account number.";
        }

        char sign = request.sign();
        if (sign != '+' && sign != '-') {
            return "Please enter + or - preceding the amount";
        }

        ValidationResult amountValidation = AmountValidator.validate(
                request.amount() != null ? request.amount().toPlainString() : null);
        if (!amountValidation.valid()) {
            return amountValidation.message();
        }

        return null;
    }

    /**
     * Applies the debit or credit to the account via the account operation service.
     * Equivalent to the UPD-CRED-DATA section in the COBOL source.
     */
    private CreditDebitResult applyDebitCredit(CreditDebitRequest request,
                                              OriginContext origin) {
        BigDecimal amount = request.amount();

        if (request.sign() == '-') {
            amount = amount.negate();
        }

        AccountOperationRequest opRequest = new AccountOperationRequest(
                request.accountNumber(),
                amount,
                origin != null ? origin.applId() : "",
                origin != null ? origin.userId() : "",
                origin != null ? origin.facilityName() : "",
                origin != null ? origin.networkId() : "",
                origin != null ? origin.facilityType() : 0
        );

        AccountOperationResponse response = accountOperationService.applyDebitCredit(opRequest);

        if (!response.success()) {
            return handleFailure(response);
        }

        return CreditDebitResult.success(
                "Amount successfully applied to the account.",
                request.accountNumber(),
                response.sortCode(),
                response.availableBalance(),
                response.actualBalance()
        );
    }

    /**
     * Handles failure responses from the account operation service.
     * Equivalent to the EVALUATE SUBPGM-FAIL-CODE block in UPD-CRED-DATA.
     */
    private CreditDebitResult handleFailure(AccountOperationResponse response) {
        Character failCode = response.failCode();

        if (failCode == null) {
            return CreditDebitResult.failure(
                    "Sorry but the AMOUNT could not be applied due to an unexpected error.");
        }

        FailCode code = FailCode.fromCode(failCode);
        if (code == null) {
            return CreditDebitResult.failure(
                    "Sorry but the AMOUNT could not be applied due to an unexpected error. " + failCode);
        }

        return switch (code) {
            case ACCOUNT_NOT_FOUND -> CreditDebitResult.failure(
                    "Sorry but the ACCOUNT no was not found for SORTCODE "
                            + (response.sortCode() != null ? response.sortCode() : "")
                            + " . Amount not applied. ");
            case UNEXPECTED_ERROR -> CreditDebitResult.failure(
                    "Sorry but the AMOUNT could not be applied due to an unexpected error.");
            case INSUFFICIENT_FUNDS -> CreditDebitResult.failure(
                    "Sorry insufficient funds available to process the request.");
        };
    }

    /**
     * Simulates EXEC CICS BIF DEEDIT - removes numeric formatting characters from a field.
     * In COBOL, DEEDIT removes commas, currency signs ($), and spaces but preserves
     * alphabetic characters and digits.
     */
    private static String deEditField(String field) {
        if (field == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(field.length());
        for (int i = 0; i < field.length(); i++) {
            char c = field.charAt(i);
            if (c == ',' || c == '$' || c == ' ') {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAllZeros(String s) {
        if (s == null || s.isEmpty()) {
            return true;
        }
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }
}
