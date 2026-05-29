/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.crecust;

/**
 * Interface for the named counter service.
 * Abstracts CICS ENQ/DEQ operations used for concurrency control
 * during customer number assignment.
 *
 * In COBOL, the named counter resource was 'CBSACUST' + sortcode,
 * and ENQ/DEQ were used to serialize access to the customer number counter.
 */
public interface NamedCounterService {

    /**
     * The base name for the customer counter resource (COBOL: 'CBSACUST').
     */
    String CUSTOMER_COUNTER_NAME = "CBSACUST";

    /**
     * Enqueues (locks) the named counter for the given sort code.
     * Mirrors COBOL EXEC CICS ENQ RESOURCE(NCS-CUST-NO-NAME).
     *
     * @param sortCode the sort code to include in the resource name
     * @throws NamedCounterException if the enqueue fails (COBOL fail code '3')
     */
    void enqueue(String sortCode) throws NamedCounterException;

    /**
     * Dequeues (unlocks) the named counter for the given sort code.
     * Mirrors COBOL EXEC CICS DEQ RESOURCE(NCS-CUST-NO-NAME).
     *
     * @param sortCode the sort code to include in the resource name
     * @throws NamedCounterException if the dequeue fails (COBOL fail code '5')
     */
    void dequeue(String sortCode) throws NamedCounterException;

    /**
     * Exception thrown when a named counter operation fails.
     */
    class NamedCounterException extends Exception {
        private final String failCode;

        public NamedCounterException(String failCode, String message) {
            super(message);
            this.failCode = failCode;
        }

        public String getFailCode() {
            return failCode;
        }
    }
}
