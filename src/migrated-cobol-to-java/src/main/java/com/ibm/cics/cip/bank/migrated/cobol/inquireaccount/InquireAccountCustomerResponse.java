/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.migrated.cobol.inquireaccount;

import java.util.Collections;
import java.util.List;

/**
 * Result of the Inquire Account for Customer operation.
 * Corresponds to the full INQACCCU COMMAREA (copybook INQACCCU.cpy).
 *
 * <p>Fail codes mirror the original COBOL COMM-FAIL-CODE values:</p>
 * <ul>
 *   <li>{@code "0"} - no failure (initial state)</li>
 *   <li>{@code "1"} - customer not found</li>
 *   <li>{@code "2"} - account query failed (cursor open)</li>
 *   <li>{@code "3"} - account fetch failed</li>
 *   <li>{@code "4"} - cursor close failed</li>
 * </ul>
 */
public record InquireAccountCustomerResponse(
        boolean success,
        String failCode,
        boolean customerFound,
        int numberOfAccounts,
        List<AccountDetail> accountDetails
) {

    public static final String FAIL_CODE_NONE = "0";
    public static final String FAIL_CODE_CUSTOMER_NOT_FOUND = "1";
    public static final String FAIL_CODE_CURSOR_OPEN = "2";
    public static final String FAIL_CODE_FETCH = "3";
    public static final String FAIL_CODE_CURSOR_CLOSE = "4";

    public InquireAccountCustomerResponse {
        accountDetails = accountDetails == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(accountDetails);
    }

    static InquireAccountCustomerResponse customerNotFound() {
        return new InquireAccountCustomerResponse(
                false, FAIL_CODE_CUSTOMER_NOT_FOUND, false,
                0, Collections.emptyList());
    }

    static InquireAccountCustomerResponse queryFailed(String failCode) {
        return new InquireAccountCustomerResponse(
                false, failCode, false,
                0, Collections.emptyList());
    }

    static InquireAccountCustomerResponse ok(List<AccountDetail> accounts) {
        return new InquireAccountCustomerResponse(
                true, FAIL_CODE_NONE, true,
                accounts.size(), accounts);
    }
}
