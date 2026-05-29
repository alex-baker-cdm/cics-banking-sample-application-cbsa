/*
 *
 *    Copyright IBM Corp. 2023
 *
 *    Migrated from COBOL to Java 21.
 *    Original source: src/base/cobol_src/GETCOMPY.cbl
 *    Original copybook: src/base/cobol_copy/GETCOMPY.cpy
 *
 */

package com.ibm.cics.cip.bank.getcompany;

/**
 * Data transfer object representing the COMMAREA (communication area)
 * used by the original GETCOMPY COBOL program.
 *
 * <p>Maps to the COBOL copybook structure:
 * <pre>
 *   01 DFHCOMMAREA.
 *       03 GETCompanyOperation.
 *         06 company-name pic x(40).
 * </pre>
 *
 * <p>The COMMAREA is a 40-byte fixed-length area where the company name
 * is returned as a left-justified, space-padded string.
 */
public class GetCompanyRequest {

    /**
     * Maximum length of the company name field, matching the COBOL PIC X(40) definition.
     */
    public static final int COMPANY_NAME_LENGTH = 40;

    private String companyName;

    public GetCompanyRequest() {
        this.companyName = "";
    }

    public GetCompanyRequest(String companyName) {
        this.companyName = companyName != null ? companyName : "";
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName != null ? companyName : "";
    }

    /**
     * Returns the company name formatted as a fixed-length field,
     * left-justified and space-padded to 40 characters, matching
     * the original COBOL PIC X(40) behavior.
     *
     * @return the company name padded to exactly 40 characters
     */
    public String getCompanyNameFixedLength() {
        String name = companyName != null ? companyName : "";
        if (name.length() > COMPANY_NAME_LENGTH) {
            return name.substring(0, COMPANY_NAME_LENGTH);
        }
        return String.format("%-" + COMPANY_NAME_LENGTH + "s", name);
    }
}
