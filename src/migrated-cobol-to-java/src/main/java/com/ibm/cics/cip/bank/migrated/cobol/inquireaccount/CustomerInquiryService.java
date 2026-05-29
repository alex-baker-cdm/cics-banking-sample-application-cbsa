/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.migrated.cobol.inquireaccount;

/**
 * Abstraction for verifying that a customer exists.
 *
 * <p>In the original COBOL program (INQACCCU), the customer check was
 * performed via {@code EXEC CICS LINK PROGRAM('INQCUST ')}. This
 * interface decouples the account inquiry from the specific customer
 * lookup implementation so it can be backed by JDBC, a REST call, or a
 * test stub.</p>
 */
@FunctionalInterface
public interface CustomerInquiryService {

    boolean isCustomerFound(long customerNumber);
}
