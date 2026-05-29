/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.crecust;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents a processed transaction record for the PROCTRAN data store.
 * Mirrors the PROCTRAN.cpy copybook and DB2 host variable layout.
 *
 * <pre>
 * COBOL DB2 host variables:
 *   HV-PROCTRAN-EYECATCHER   PIC X(4)      "PRTR"
 *   HV-PROCTRAN-SORT-CODE    PIC X(6)
 *   HV-PROCTRAN-ACC-NUMBER   PIC X(8)      (zeros for customer creation)
 *   HV-PROCTRAN-DATE         PIC X(10)     DD.MM.YYYY
 *   HV-PROCTRAN-TIME         PIC X(6)      HHMMSS
 *   HV-PROCTRAN-REF          PIC X(12)     (task number)
 *   HV-PROCTRAN-TYPE         PIC X(3)      "OCC" = branch create customer
 *   HV-PROCTRAN-DESC         PIC X(40)     sortcode + custno + name + dob
 *   HV-PROCTRAN-AMOUNT       PIC S9(10)V99 (zero for customer creation)
 * </pre>
 */
public record ProcessedTransactionRecord(
        String eyecatcher,
        String sortCode,
        String accountNumber,
        LocalDate transactionDate,
        LocalTime transactionTime,
        String reference,
        String type,
        String description,
        BigDecimal amount
) {
    public static final String EYECATCHER_VALUE = "PRTR";
    public static final String TYPE_BRANCH_CREATE_CUSTOMER = "OCC";
}
