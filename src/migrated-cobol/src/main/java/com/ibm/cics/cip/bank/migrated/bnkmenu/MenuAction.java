/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNKMENU.cbl - EDIT-MENU-DATA / INVOKE-OTHER-TXNS
 */
package com.ibm.cics.cip.bank.migrated.bnkmenu;

import java.util.Map;

public enum MenuAction {

    DISPLAY_CUSTOMER("1", "ODCS", "Display/Delete/Update CUSTOMER information"),
    DISPLAY_ACCOUNT("2", "ODAC", "Display/Delete ACCOUNT information"),
    CREATE_CUSTOMER("3", "OCCS", "Create CUSTOMER"),
    CREATE_ACCOUNT("4", "OCAC", "Create ACCOUNT"),
    UPDATE_ACCOUNT("5", "OUAC", "Update ACCOUNT"),
    CREDIT_DEBIT("6", "OCRA", "Credit/Debit funds to an ACCOUNT"),
    TRANSFER_FUNDS("7", "OTFN", "Transfer funds"),
    LOOKUP_ACCOUNTS("A", "OCCA", "Look up Accounts with Customer Number");

    private static final Map<String, MenuAction> BY_CODE;

    static {
        BY_CODE = Map.of(
                "1", DISPLAY_CUSTOMER,
                "2", DISPLAY_ACCOUNT,
                "3", CREATE_CUSTOMER,
                "4", CREATE_ACCOUNT,
                "5", UPDATE_ACCOUNT,
                "6", CREDIT_DEBIT,
                "7", TRANSFER_FUNDS,
                "A", LOOKUP_ACCOUNTS
        );
    }

    private final String code;
    private final String transactionId;
    private final String description;

    MenuAction(String code, String transactionId, String description) {
        this.code = code;
        this.transactionId = transactionId;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getDescription() {
        return description;
    }

    public static MenuAction fromCode(String code) {
        return BY_CODE.get(code);
    }

    public static boolean isValidCode(String code) {
        return BY_CODE.containsKey(code);
    }
}
