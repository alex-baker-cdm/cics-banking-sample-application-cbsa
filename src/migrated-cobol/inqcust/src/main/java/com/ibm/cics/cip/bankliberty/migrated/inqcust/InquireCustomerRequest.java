/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bankliberty.migrated.inqcust;

/**
 * Input for the Inquire Customer operation.
 * Maps to the input portion of the INQCUST DFHCOMMAREA.
 *
 * <p>Special customer number values:
 * <ul>
 *   <li>{@code 0} — request a random customer</li>
 *   <li>{@code 9999999999} — request the last (highest-numbered) customer</li>
 *   <li>Any other value — look up that specific customer</li>
 * </ul>
 *
 * @param customerNumber  the customer number to inquire (0, 9999999999, or a specific value)
 */
public record InquireCustomerRequest(
        long customerNumber
) {
    public static final long RANDOM_CUSTOMER = 0L;
    public static final long LAST_CUSTOMER = 9_999_999_999L;
}
