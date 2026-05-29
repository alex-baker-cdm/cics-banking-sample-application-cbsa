/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

/**
 * Java 21 migration of the GETCOMPY COBOL program.
 *
 * <p>This package contains the migrated implementation of the GETCOMPY
 * CICS COBOL program, which provides company name information for the
 * CICS Banking Sample Application (CBSA).
 *
 * <h2>Original COBOL Program</h2>
 * <ul>
 *   <li>Source: {@code src/base/cobol_src/GETCOMPY.cbl}</li>
 *   <li>Copybook: {@code src/base/cobol_copy/GETCOMPY.cpy}</li>
 *   <li>Author: James O'Grady</li>
 *   <li>CICS Transaction: Linked via EXEC CICS LINK</li>
 * </ul>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link com.ibm.cics.cip.bank.getcompany.GetCompanyService} - Main service (replaces COBOL PROCEDURE DIVISION)</li>
 *   <li>{@link com.ibm.cics.cip.bank.getcompany.GetCompanyRequest} - Data structure (replaces COBOL COMMAREA/copybook)</li>
 * </ul>
 */
package com.ibm.cics.cip.bank.getcompany;
