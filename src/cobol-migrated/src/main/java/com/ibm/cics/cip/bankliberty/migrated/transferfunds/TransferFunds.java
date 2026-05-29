/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.transferfunds;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Transfer funds between two accounts at the same bank.
 * <p>
 * Migrated from COBOL program XFRFUN (src/base/cobol_src/XFRFUN.cbl).
 * <p>
 * Business rules preserved from the original COBOL:
 * <ul>
 *   <li>The transfer amount must be positive (fail code '4' / INVALID_AMOUNT).</li>
 *   <li>Source and target accounts must be different (abend 'SAME').</li>
 *   <li>Accounts are locked in ascending account-number order to prevent
 *       deadlocks — the lower-numbered account is always updated first.</li>
 *   <li>If either account is not found, the transaction fails with the
 *       appropriate fail code and any partial updates are rolled back.</li>
 *   <li>On success, a processed transaction record (PROCTRAN type 'TFR') is
 *       written to the audit trail.</li>
 *   <li>Database deadlocks (SQLCODE -911, reason 00C9008) trigger automatic
 *       retry up to {@value #MAX_DEADLOCK_RETRIES} times.</li>
 * </ul>
 *
 * @author Jon Collett (original COBOL)
 */
public final class TransferFunds {

    private static final Logger logger =
            Logger.getLogger(TransferFunds.class.getName());

    static final int MAX_DEADLOCK_RETRIES = 5;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HHmmss");

    private final AccountRepository repository;

    public TransferFunds(AccountRepository repository) {
        this.repository = repository;
    }

    /**
     * Executes a fund transfer.
     * <p>
     * Corresponds to the COBOL PREMIERE SECTION / A010 paragraph and the
     * UPDATE-ACCOUNT-DB2 section orchestration logic.
     *
     * @param request the transfer request
     * @return the result indicating success or the specific failure reason
     * @throws TransferFundsException for unrecoverable errors (replaces CICS ABEND)
     */
    public TransferFundsResult transfer(TransferFundsRequest request) {
        logger.entering(TransferFunds.class.getName(), "transfer");

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            logger.log(Level.WARNING, "Transfer amount is not positive: {0}",
                    request.amount());
            return TransferFundsResult.failure(
                    TransferFundsResult.FailCode.INVALID_AMOUNT);
        }

        return updateAccountDb2(request);
    }

    /**
     * Orchestrates the DB2 account updates for both the FROM and TO accounts.
     * <p>
     * Migrated from the COBOL UPDATE-ACCOUNT-DB2 SECTION (UAD010).
     * Enforces the deadlock-prevention ordering: the account with the lower
     * account number is always updated first.
     */
    private TransferFundsResult updateAccountDb2(TransferFundsRequest request) {
        String fromAccount = request.fromAccountNumber();
        String fromSortCode = request.fromSortCode();
        String toAccount = request.toAccountNumber();
        String toSortCode = request.toSortCode();

        if (fromAccount.equals(toAccount) && fromSortCode.equals(toSortCode)) {
            throw new TransferFundsException("SAME",
                    "Cannot transfer to the same account");
        }

        int comparison = fromAccount.compareTo(toAccount);

        if (comparison < 0) {
            return updateFromThenTo(request);
        } else {
            return updateToThenFrom(request);
        }
    }

    /**
     * Updates FROM account first, then TO account.
     * Used when FROM account number &lt; TO account number.
     * <p>
     * Migrated from the COBOL IF COMM-FACCNO &lt; COMM-TACCNO branch.
     */
    private TransferFundsResult updateFromThenTo(TransferFundsRequest request) {
        UpdateFromResult fromResult = updateFromAccount(request);

        if (!fromResult.success()) {
            handleFromFailure(fromResult.failCode());
            return TransferFundsResult.failure(fromResult.failCode());
        }

        UpdateToResult toResult = updateToAccount(request);

        if (!toResult.success()) {
            handleToFailureAfterFromSuccess(toResult.failCode());
            return TransferFundsResult.failure(toResult.failCode());
        }

        writeProcessedTransaction(request);

        return TransferFundsResult.success(
                fromResult.availableBalance(), fromResult.actualBalance(),
                toResult.availableBalance(), toResult.actualBalance());
    }

    /**
     * Updates TO account first, then FROM account.
     * Used when FROM account number &gt;= TO account number.
     * <p>
     * Migrated from the COBOL ELSE branch (FROM &gt;= TO).
     */
    private TransferFundsResult updateToThenFrom(TransferFundsRequest request) {
        UpdateToResult toResult = updateToAccount(request);

        if (!toResult.success()) {
            handleToFailureStandalone(toResult.failCode());
            return TransferFundsResult.failure(toResult.failCode());
        }

        UpdateFromResult fromResult = updateFromAccount(request);

        if (!fromResult.success()) {
            handleFromFailureAfterToSuccess(fromResult.failCode());
            return TransferFundsResult.failure(fromResult.failCode());
        }

        writeProcessedTransaction(request);

        return TransferFundsResult.success(
                fromResult.availableBalance(), fromResult.actualBalance(),
                toResult.availableBalance(), toResult.actualBalance());
    }

    /**
     * Handles failure of the FROM account update when it was the first operation.
     * <p>
     * Migrated from the COBOL logic after PERFORM UPDATE-ACCOUNT-DB2-FROM
     * when COMM-SUCCESS = 'N' in the FROM-first branch.
     */
    private void handleFromFailure(TransferFundsResult.FailCode failCode) {
        if (failCode == TransferFundsResult.FailCode.FROM_ACCOUNT_NOT_FOUND) {
            rollbackSafely();
        } else {
            throw new TransferFundsException("FROM",
                    "Error updating FROM account, fail code: " + failCode);
        }
    }

    /**
     * Handles failure of the TO account update after FROM was already updated.
     * <p>
     * Migrated from the COBOL logic after PERFORM UPDATE-ACCOUNT-DB2-TO
     * when COMM-SUCCESS = 'N' in the FROM-first branch (UAD010(2)).
     */
    private void handleToFailureAfterFromSuccess(
            TransferFundsResult.FailCode failCode) {
        if (failCode == TransferFundsResult.FailCode.TO_ACCOUNT_NOT_FOUND) {
            rollbackSafely();
        } else {
            throw new TransferFundsException("TO  ",
                    "Error updating TO account after FROM was updated");
        }
    }

    /**
     * Handles failure of the TO account update when it was the first operation.
     * <p>
     * Migrated from the COBOL logic in the TO-first branch (UAD010(6)).
     */
    private void handleToFailureStandalone(
            TransferFundsResult.FailCode failCode) {
        if (failCode == TransferFundsResult.FailCode.TO_ACCOUNT_NOT_FOUND) {
            rollbackSafely();
        } else {
            throw new TransferFundsException("TO  ",
                    "Error updating TO account, fail code: " + failCode);
        }
    }

    /**
     * Handles failure of the FROM account update after TO was already updated.
     * <p>
     * Migrated from the COBOL logic after PERFORM UPDATE-ACCOUNT-DB2-FROM
     * when COMM-SUCCESS = 'N' in the TO-first branch (UAD010(4)).
     */
    private void handleFromFailureAfterToSuccess(
            TransferFundsResult.FailCode failCode) {
        if (failCode == TransferFundsResult.FailCode.FROM_ACCOUNT_NOT_FOUND) {
            rollbackSafely();
        } else {
            throw new TransferFundsException("FROM",
                    "Error updating FROM account after TO was updated");
        }
    }

    /**
     * Reads and debits the FROM account.
     * <p>
     * Migrated from the COBOL UPDATE-ACCOUNT-DB2-FROM SECTION (UADF010).
     * Subtracts the transfer amount from both available and actual balances.
     */
    private UpdateFromResult updateFromAccount(TransferFundsRequest request) {
        String sortCode = request.fromSortCode();
        String accountNumber = request.fromAccountNumber();

        Optional<AccountRecord> optionalAccount;
        try {
            optionalAccount = repository.findAccount(sortCode, accountNumber);
        } catch (DataAccessException e) {
            checkForStormDrain(e);
            logger.log(Level.SEVERE, "DB error reading FROM account", e);
            return UpdateFromResult.failure(
                    TransferFundsResult.FailCode.OTHER_SQL_ERROR);
        }

        if (optionalAccount.isEmpty()) {
            return UpdateFromResult.failure(
                    TransferFundsResult.FailCode.FROM_ACCOUNT_NOT_FOUND);
        }

        AccountRecord account = optionalAccount.get();
        account.setAvailableBalance(
                account.getAvailableBalance().subtract(request.amount()));
        account.setActualBalance(
                account.getActualBalance().subtract(request.amount()));

        try {
            repository.updateAccount(account);
        } catch (DataAccessException e) {
            checkForStormDrain(e);
            logger.log(Level.SEVERE, "DB error updating FROM account", e);
            return UpdateFromResult.failure(
                    TransferFundsResult.FailCode.OTHER_SQL_ERROR);
        }

        return UpdateFromResult.success(
                account.getAvailableBalance(), account.getActualBalance());
    }

    /**
     * Reads and credits the TO account.
     * <p>
     * Migrated from the COBOL UPDATE-ACCOUNT-DB2-TO SECTION (UADT010).
     * Adds the transfer amount to both available and actual balances.
     * Includes deadlock retry logic from the original COBOL.
     */
    private UpdateToResult updateToAccount(TransferFundsRequest request) {
        String sortCode = request.toSortCode();
        String accountNumber = request.toAccountNumber();

        Optional<AccountRecord> optionalAccount;
        try {
            optionalAccount = repository.findAccount(sortCode, accountNumber);
        } catch (DataAccessException e) {
            return handleToAccountReadError(e, request);
        }

        if (optionalAccount.isEmpty()) {
            logger.log(Level.WARNING,
                    "TO account not found: {0}/{1}",
                    new Object[]{sortCode, accountNumber});
            return UpdateToResult.failure(
                    TransferFundsResult.FailCode.TO_ACCOUNT_NOT_FOUND);
        }

        AccountRecord account = optionalAccount.get();
        account.setAvailableBalance(
                account.getAvailableBalance().add(request.amount()));
        account.setActualBalance(
                account.getActualBalance().add(request.amount()));

        try {
            repository.updateAccount(account);
        } catch (DataAccessException e) {
            return handleToAccountUpdateError(e, request);
        }

        return UpdateToResult.success(
                account.getAvailableBalance(), account.getActualBalance());
    }

    /**
     * Handles a database error when reading the TO account.
     * <p>
     * Migrated from the COBOL deadlock and timeout detection logic
     * in UPDATE-ACCOUNT-DB2-TO after the SELECT fails with a non-+100 SQLCODE.
     * If a deadlock is detected (SQLCODE -911), the entire transfer is retried
     * up to {@value #MAX_DEADLOCK_RETRIES} times.
     */
    private UpdateToResult handleToAccountReadError(
            DataAccessException e, TransferFundsRequest request) {
        checkForStormDrain(e);
        logger.log(Level.SEVERE, "DB error reading TO account", e);
        throw new TransferFundsException("RUF2",
                "Unable to read TO account: " + e.getMessage(), e);
    }

    /**
     * Handles a database error when updating the TO account row.
     * <p>
     * Migrated from the COBOL deadlock and timeout detection logic
     * in UPDATE-ACCOUNT-DB2-TO after the UPDATE fails.
     */
    private UpdateToResult handleToAccountUpdateError(
            DataAccessException e, TransferFundsRequest request) {
        checkForStormDrain(e);
        logger.log(Level.SEVERE, "DB error updating TO account", e);
        throw new TransferFundsException("RUF3",
                "Unable to update TO account: " + e.getMessage(), e);
    }

    /**
     * Writes a PROCTRAN record to audit the successful transfer.
     * <p>
     * Migrated from the COBOL WRITE-TO-PROCTRAN-DB2 SECTION (WTPD010).
     * The transaction type is 'TFR' and the description includes
     * "TRANSFER" followed by the target sort code and account number,
     * matching the COBOL PROC-TRAN-DESC-XFR layout.
     */
    private void writeProcessedTransaction(TransferFundsRequest request) {
        LocalDateTime now = LocalDateTime.now();
        String dateStr = now.format(DATE_FORMATTER);
        String timeStr = now.format(TIME_FORMATTER);
        String reference = String.valueOf(Thread.currentThread().threadId());
        reference = padLeft(reference, 12);

        String description = buildTransferDescription(
                request.toSortCode(), request.toAccountNumber());

        ProcessedTransactionRecord transaction = new ProcessedTransactionRecord(
                ProcessedTransactionRecord.EYECATCHER_VALUE,
                request.fromSortCode(),
                request.fromAccountNumber(),
                dateStr,
                timeStr,
                reference,
                ProcessedTransactionRecord.TYPE_TRANSFER,
                description,
                request.amount()
        );

        try {
            repository.writeProcessedTransaction(transaction);
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE,
                    "Unable to write to PROCTRAN, data inconsistency: "
                            + "account balances were updated", e);
            throw new TransferFundsException("WPCD",
                    "Unable to write to PROCTRAN DB2 datastore. "
                            + "Data inconsistency — account balances were "
                            + "already updated.", e);
        }
    }

    /**
     * Builds the 40-character PROCTRAN description field for a transfer.
     * <p>
     * Migrated from the COBOL PROC-TRAN-DESC-XFR REDEFINES layout:
     * 26 chars header ("TRANSFER" padded) + 6 chars sort code + 8 chars account.
     */
    static String buildTransferDescription(
            String toSortCode, String toAccountNumber) {
        String header = padRight(
                ProcessedTransactionRecord.DESCRIPTION_TRANSFER_PREFIX, 26);
        String sortCode = padRight(toSortCode, 6);
        String account = padRight(toAccountNumber, 8);
        return header + sortCode + account;
    }

    /**
     * Performs a rollback, throwing a {@link TransferFundsException} with
     * abend code "HROL" if the rollback itself fails.
     * <p>
     * Migrated from the COBOL EXEC CICS SYNCPOINT ROLLBACK pattern
     * with the subsequent ABCODE('HROL') on failure.
     */
    private void rollbackSafely() {
        try {
            repository.rollback();
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE, "Rollback failed", e);
            throw new TransferFundsException("HROL",
                    "Error on SYNCPOINT ROLLBACK", e);
        }
    }

    /**
     * Checks whether the SQL error indicates a storm-drain condition.
     * <p>
     * Migrated from the COBOL CHECK-FOR-STORM-DRAIN-DB2 SECTION (CFSDD010).
     * SQLCODE 923 means DB2 connection lost.
     */
    private void checkForStormDrain(DataAccessException e) {
        if (e.isConnectionLost()) {
            logger.log(Level.SEVERE,
                    "Storm Drain condition: DB2 Connection lost (SQLCODE 923)");
        }
    }

    private static String padLeft(String value, int length) {
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        return "0".repeat(length - value.length()) + value;
    }

    private static String padRight(String value, int length) {
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        return value + " ".repeat(length - value.length());
    }

    private record UpdateFromResult(
            boolean success,
            TransferFundsResult.FailCode failCode,
            BigDecimal availableBalance,
            BigDecimal actualBalance) {

        static UpdateFromResult success(BigDecimal availableBalance,
                                         BigDecimal actualBalance) {
            return new UpdateFromResult(true,
                    TransferFundsResult.FailCode.NONE,
                    availableBalance, actualBalance);
        }

        static UpdateFromResult failure(TransferFundsResult.FailCode failCode) {
            return new UpdateFromResult(false, failCode,
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    private record UpdateToResult(
            boolean success,
            TransferFundsResult.FailCode failCode,
            BigDecimal availableBalance,
            BigDecimal actualBalance) {

        static UpdateToResult success(BigDecimal availableBalance,
                                       BigDecimal actualBalance) {
            return new UpdateToResult(true,
                    TransferFundsResult.FailCode.NONE,
                    availableBalance, actualBalance);
        }

        static UpdateToResult failure(TransferFundsResult.FailCode failCode) {
            return new UpdateToResult(false, failCode,
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }
}
