/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNK1CRA.cbl
 * Original Author: Jon Collett
 */
package com.ibm.cics.cip.bank.migrated.bnk1cra;

import java.math.BigDecimal;

/**
 * Interface representing the DBCRFUN sub-program functionality.
 * In the original COBOL, this was called via EXEC CICS LINK PROGRAM('DBCRFUN').
 * Implementations provide the actual account debit/credit persistence logic.
 */
public interface AccountOperationService {

    /**
     * Applies a debit or credit to the specified account.
     *
     * @param request the operation request containing account and amount details
     * @return the result of the operation
     */
    AccountOperationResponse applyDebitCredit(AccountOperationRequest request);

    /**
     * Request parameters for the debit/credit operation.
     * Maps to the COBOL SUBPGM-PARMS structure.
     *
     * @param accountNumber the 8-character account number (SUBPGM-ACCNO)
     * @param amount        the signed amount to apply (SUBPGM-AMT, S9(10)V99)
     * @param applId        originating application ID (SUBPGM-APPLID)
     * @param userId        originating user ID (SUBPGM-USERID)
     * @param facilityName  originating facility name (SUBPGM-FACILITY-NAME)
     * @param networkId     originating network ID (SUBPGM-NETWRK-ID)
     * @param facilityType  originating facility type (SUBPGM-FACILTYPE)
     */
    record AccountOperationRequest(
            String accountNumber,
            BigDecimal amount,
            String applId,
            String userId,
            String facilityName,
            String networkId,
            int facilityType
    ) {
    }

    /**
     * Response from the debit/credit operation.
     * Maps to the output fields in SUBPGM-PARMS.
     *
     * @param success          whether the operation succeeded (SUBPGM-SUCCESS)
     * @param failCode         failure reason code (SUBPGM-FAIL-CODE), null if success
     * @param sortCode         the account sort code (SUBPGM-SORTC)
     * @param availableBalance available balance after operation (SUBPGM-AV-BAL)
     * @param actualBalance    actual balance after operation (SUBPGM-ACT-BAL)
     */
    record AccountOperationResponse(
            boolean success,
            Character failCode,
            String sortCode,
            BigDecimal availableBalance,
            BigDecimal actualBalance
    ) {
    }
}
