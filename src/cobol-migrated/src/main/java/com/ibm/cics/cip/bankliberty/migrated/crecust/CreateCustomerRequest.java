/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.crecust;

import java.time.LocalDate;

/**
 * Input data for the Create Customer operation.
 * Mirrors the DFHCOMMAREA input fields from CRECUST.cpy.
 *
 * <pre>
 * COBOL COMMAREA fields (input):
 *   COMM-NAME           PIC X(60)
 *   COMM-ADDRESS        PIC X(160)
 *   COMM-DATE-OF-BIRTH  PIC 9(8)  (DDMMYYYY)
 *   COMM-SORTCODE       PIC 9(6)
 * </pre>
 */
public record CreateCustomerRequest(
        String sortCode,
        String name,
        String address,
        LocalDate dateOfBirth
) {
    public CreateCustomerRequest {
        if (sortCode == null || sortCode.isBlank()) {
            throw new IllegalArgumentException("sortCode must not be null or blank");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (address == null) {
            throw new IllegalArgumentException("address must not be null");
        }
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("dateOfBirth must not be null");
        }
    }
}
