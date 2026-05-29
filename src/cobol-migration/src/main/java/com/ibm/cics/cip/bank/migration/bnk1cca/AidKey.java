/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL COPY DFHAID - CICS Attention Identifier keys
 * used by BNK1CCA.cbl to determine user actions.
 */
package com.ibm.cics.cip.bank.migration.bnk1cca;

/**
 * Enumerates the CICS terminal AID keys handled by the BNK1CCA program.
 * Maps to the DFHAID copybook constants used in the EVALUATE TRUE block.
 */
public enum AidKey {
    ENTER,
    PF3,
    PF12,
    PA1,
    PA2,
    PA3,
    CLEAR,
    AID
}
