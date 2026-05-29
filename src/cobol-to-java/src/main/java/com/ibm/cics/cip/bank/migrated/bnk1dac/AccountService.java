/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from BNK1DAC.cbl — abstracts EXEC CICS LINK to INQACC and DELACC.
 */
package com.ibm.cics.cip.bank.migrated.bnk1dac;

/**
 * Service interface abstracting the sub-program calls that were
 * originally EXEC CICS LINK PROGRAM('INQACC') and
 * EXEC CICS LINK PROGRAM('DELACC') in the COBOL source.
 *
 * <p>Implementations may delegate to a database, REST service,
 * or a CICS program link depending on the deployment target.
 */
public interface AccountService {

    /**
     * Inquire on an account by account number.
     * Equivalent to EXEC CICS LINK PROGRAM('INQACC').
     *
     * @param accountNumber the 8-digit account number
     * @return inquiry result data
     * @throws AccountServiceException if the underlying call fails
     */
    AccountInquiryData inquireAccount(int accountNumber);

    /**
     * Delete an account by account number.
     * Equivalent to EXEC CICS LINK PROGRAM('DELACC').
     *
     * @param accountNumber the 8-digit account number
     * @return delete result indicating success or failure with a code
     * @throws AccountServiceException if the underlying call fails
     */
    AccountDeleteResult deleteAccount(int accountNumber);
}
