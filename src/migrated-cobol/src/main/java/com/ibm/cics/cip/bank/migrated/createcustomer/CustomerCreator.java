/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.migrated.createcustomer;

/**
 * Interface for the backend customer-creation service, replacing the
 * {@code EXEC CICS LINK PROGRAM('CRECUST')} call in the original COBOL.
 *
 * <p>Implementations connect to the actual datastore (DB2/VSAM) or can be
 * stubbed for testing.
 */
@FunctionalInterface
public interface CustomerCreator {

    CustomerCreationResult createCustomer(CustomerCreationRequest request);
}
