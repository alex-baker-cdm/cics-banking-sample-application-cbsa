/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: CREACC.cbl FIND-NEXT-ACCOUNT SECTION
 * (DB2 CONTROL table access)
 */

package com.ibm.cics.cip.bank.migrated.creacc.repository;

/**
 * Repository for the DB2 CONTROL table which manages named counters.
 * Replaces the SQL SELECT/UPDATE operations on the CONTROL table
 * in the FIND-NEXT-ACCOUNT section of CREACC.cbl.
 */
public interface AccountControlRepository {

    /**
     * Retrieves the current numeric value for a control record.
     *
     * @param controlName the control record name
     *                    (e.g., "{sortcode}-ACCOUNT-LAST")
     * @return the current value, or -1 if not found
     */
    long getControlValue(String controlName);

    /**
     * Updates the numeric value of a control record.
     *
     * @param controlName the control record name
     * @param newValue    the new value to set
     * @return true if the update was successful
     */
    boolean updateControlValue(String controlName, long newValue);
}
