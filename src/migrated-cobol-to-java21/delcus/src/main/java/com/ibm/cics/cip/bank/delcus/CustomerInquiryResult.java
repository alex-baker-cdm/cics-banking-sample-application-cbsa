/*
 *    Copyright IBM Corp. 2023
 *
 *    Migrated from COBOL copybook INQCUST.cpy
 */
package com.ibm.cics.cip.bank.delcus;

/**
 * Result of the INQCUST program invocation.
 * Maps the INQCUST COMMAREA output fields.
 *
 * @param success   true if the inquiry succeeded (INQCUST-INQ-SUCCESS = 'Y')
 * @param failCode  failure code character when success is false
 */
public record CustomerInquiryResult(
        boolean success,
        String failCode
) {
}
