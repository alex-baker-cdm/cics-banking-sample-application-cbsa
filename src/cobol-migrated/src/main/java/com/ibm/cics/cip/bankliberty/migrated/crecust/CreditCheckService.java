/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.crecust;

import java.util.List;

/**
 * Interface for the credit check service.
 * Abstracts the COBOL async credit check logic (OCR1-OCR5 transactions).
 *
 * In COBOL, this was implemented via CICS ASYNC API:
 * - 5 child transactions (OCR1-OCR5) run asynchronously
 * - Results are fetched after a 3-second delay
 * - Credit scores from responding agencies are averaged
 */
public interface CreditCheckService {

    /**
     * Number of credit agencies to query (mirrors COBOL loop of 5).
     */
    int DEFAULT_AGENCY_COUNT = 5;

    /**
     * Performs credit checks across multiple agencies and returns the
     * individual credit scores from agencies that responded.
     *
     * @param request the customer data to check
     * @return list of credit scores from responding agencies (may be empty)
     * @throws CreditCheckException if the credit check process itself fails
     */
    List<Integer> performCreditChecks(CreateCustomerRequest request)
            throws CreditCheckException;

    /**
     * Exception thrown when the credit check process fails.
     * Maps to COBOL fail codes: A, B, D, E, F, G, H.
     */
    class CreditCheckException extends Exception {
        private final String failCode;

        public CreditCheckException(String failCode, String message) {
            super(message);
            this.failCode = failCode;
        }

        public String getFailCode() {
            return failCode;
        }
    }
}
