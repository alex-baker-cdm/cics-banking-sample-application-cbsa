/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.crecust;

/**
 * Represents the customer control record in the CUSTOMER data store.
 * Mirrors the CUSTCTRL.cpy copybook layout.
 *
 * <pre>
 * COBOL layout:
 *   CUSTOMER-CONTROL-EYECATCHER    PIC X(4)      "CTRL"
 *   CUSTOMER-CONTROL-SORTCODE      PIC 9(6)
 *   CUSTOMER-CONTROL-NUMBER        PIC 9(10)     (always 9999999999)
 *   NUMBER-OF-CUSTOMERS            PIC 9(10)
 *   LAST-CUSTOMER-NUMBER           PIC 9(10)
 *   CUSTOMER-CONTROL-SUCCESS-FLAG  PIC X
 *   CUSTOMER-CONTROL-FAIL-CODE     PIC X
 * </pre>
 */
public class CustomerControlRecord {

    public static final String EYECATCHER_VALUE = "CTRL";
    public static final String CONTROL_NUMBER = "9999999999";

    private String eyecatcher;
    private String sortCode;
    private String controlNumber;
    private long numberOfCustomers;
    private long lastCustomerNumber;
    private String successFlag;
    private String failCode;

    public CustomerControlRecord() {
    }

    public CustomerControlRecord(String eyecatcher, String sortCode,
                                 String controlNumber,
                                 long numberOfCustomers,
                                 long lastCustomerNumber,
                                 String successFlag, String failCode) {
        this.eyecatcher = eyecatcher;
        this.sortCode = sortCode;
        this.controlNumber = controlNumber;
        this.numberOfCustomers = numberOfCustomers;
        this.lastCustomerNumber = lastCustomerNumber;
        this.successFlag = successFlag;
        this.failCode = failCode;
    }

    public String getEyecatcher() {
        return eyecatcher;
    }

    public void setEyecatcher(String eyecatcher) {
        this.eyecatcher = eyecatcher;
    }

    public String getSortCode() {
        return sortCode;
    }

    public void setSortCode(String sortCode) {
        this.sortCode = sortCode;
    }

    public String getControlNumber() {
        return controlNumber;
    }

    public void setControlNumber(String controlNumber) {
        this.controlNumber = controlNumber;
    }

    public long getNumberOfCustomers() {
        return numberOfCustomers;
    }

    public void setNumberOfCustomers(long numberOfCustomers) {
        this.numberOfCustomers = numberOfCustomers;
    }

    public long getLastCustomerNumber() {
        return lastCustomerNumber;
    }

    public void setLastCustomerNumber(long lastCustomerNumber) {
        this.lastCustomerNumber = lastCustomerNumber;
    }

    public String getSuccessFlag() {
        return successFlag;
    }

    public void setSuccessFlag(String successFlag) {
        this.successFlag = successFlag;
    }

    public String getFailCode() {
        return failCode;
    }

    public void setFailCode(String failCode) {
        this.failCode = failCode;
    }
}
