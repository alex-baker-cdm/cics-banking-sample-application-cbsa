package com.ibm.cics.cip.bank.migrated.displaycustomer;

/**
 * Abstracts the CICS LINK calls to sub-programs INQCUST, DELCUS, and UPDCUST.
 * In the original COBOL, these were EXEC CICS LINK PROGRAM calls.
 * Implementations can delegate to REST APIs, direct DB access, or CICS channels.
 */
public interface CustomerService {

    InqCustCommarea inquireCustomer(InqCustCommarea commarea);

    DelCusCommarea deleteCustomer(DelCusCommarea commarea);

    UpdCustCommarea updateCustomer(UpdCustCommarea commarea);
}
