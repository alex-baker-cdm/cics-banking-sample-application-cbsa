/**
 * Java 21 migration of COBOL program BNK1DAC — Display Account screen
 * for the CICS Banking Sample Application (CBSA).
 *
 * <p>This package contains the migrated business logic originally
 * implemented in {@code src/base/cobol_src/BNK1DAC.cbl} with its
 * associated BMS map {@code BNK1DAM} and copybooks {@code INQACC},
 * {@code ABNDINFO}, and {@code DFHAID}.
 *
 * <h2>Class Mapping</h2>
 * <table>
 *   <tr><th>COBOL Artifact</th><th>Java Class</th></tr>
 *   <tr><td>BNK1DAC.cbl (main program)</td>
 *       <td>{@link DisplayAccountController}</td></tr>
 *   <tr><td>DFHCOMMAREA / WS-COMM-AREA</td>
 *       <td>{@link CommArea}</td></tr>
 *   <tr><td>INQACC.cpy (inquiry commarea)</td>
 *       <td>{@link AccountInquiryData}</td></tr>
 *   <tr><td>PARMS-SUBPGM-DEL-* fields</td>
 *       <td>{@link AccountDeleteResult}</td></tr>
 *   <tr><td>BNK1DAM.bms (screen fields)</td>
 *       <td>{@link ScreenField}</td></tr>
 *   <tr><td>EXEC CICS LINK PROGRAM('INQACC'/'DELACC')</td>
 *       <td>{@link AccountService}</td></tr>
 *   <tr><td>CICS ABEND scenarios</td>
 *       <td>{@link AccountServiceException}</td></tr>
 * </table>
 */
package com.ibm.cics.cip.bank.migrated.bnk1dac;
