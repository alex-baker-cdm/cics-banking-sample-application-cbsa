/*
 * Copyright IBM Corp. 2023
 *
 * Data-access abstraction for the ACCOUNT table.
 * Mirrors the SQL operations in DELACC.cbl (READ-ACCOUNT-DB2, DEL-ACCOUNT-DB2).
 */
package com.ibm.cics.cip.bank.delacc;

import java.util.Optional;

public interface AccountDao {

    Optional<AccountRecord> findByAccountNumberAndSortCode(
        String accountNumber, String sortCode);

    boolean deleteByAccountNumberAndSortCode(
        String accountNumber, String sortCode);
}
