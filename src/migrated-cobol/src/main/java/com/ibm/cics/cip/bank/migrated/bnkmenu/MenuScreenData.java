/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNK1MAI.bms - BNK1ME map fields
 *
 * Represents the input and output fields of the BNK1ME BMS map.
 */
package com.ibm.cics.cip.bank.migrated.bnkmenu;

public class MenuScreenData {

    private String company = "";
    private String action = "";
    private String message = "";

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void clearFields() {
        company = "";
        action = "";
        message = "";
    }

    @Override
    public String toString() {
        return "MenuScreenData{" +
                "company='" + company + '\'' +
                ", action='" + action + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
