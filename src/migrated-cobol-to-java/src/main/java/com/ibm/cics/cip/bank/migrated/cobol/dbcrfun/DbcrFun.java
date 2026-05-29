/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.dbcrfun;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java 21 migration of DBCRFUN.cbl — Credit/Debit Account business logic.
 *
 * <p>This program handles cash deposits (credits) and withdrawals (debits)
 * for bank accounts. It takes an account number and amount, retrieves the
 * account from the data store, applies the amount, and returns the updated
 * balances. On success it writes a record to the PROCTRAN (Processed
 * Transaction) data store.</p>
 *
 * <h3>COBOL-to-Java mapping</h3>
 * <ul>
 *   <li>PREMIERE SECTION (A010)          &rarr; {@link #process(CreditDebitRequest)}</li>
 *   <li>UPDATE-ACCOUNT-DB2 SECTION       &rarr; {@link #updateAccountDb2(CreditDebitRequest)}</li>
 *   <li>WRITE-TO-PROCTRAN-DB2 SECTION    &rarr; {@link #writeToProctranDb2(CreditDebitRequest, AccountRecord)}</li>
 *   <li>CHECK-FOR-STORM-DRAIN-DB2        &rarr; {@link #checkForStormDrainDb2(int)}</li>
 * </ul>
 *
 * <h3>Fail codes (COMM-FAIL-CODE)</h3>
 * <ul>
 *   <li>{@code 0} — success</li>
 *   <li>{@code 1} — account not found (SQLCODE +100)</li>
 *   <li>{@code 2} — database error</li>
 *   <li>{@code 3} — insufficient funds (debit via PAYMENT only)</li>
 *   <li>{@code 4} — operation not allowed on MORTGAGE/LOAN via PAYMENT</li>
 * </ul>
 */
public class DbcrFun {

    private static final Logger logger =
            Logger.getLogger(DbcrFun.class.getName());

    static final String SORT_CODE = "987654";
    private static final int ORIGIN_DESC_MAX_LENGTH = 14;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HHmmss");

    private final AccountRepository accountRepository;
    private final ProcessedTransactionRepository proctranRepository;
    private final TransactionReferenceProvider referenceProvider;
    private final DateTimeProvider dateTimeProvider;

    public DbcrFun(AccountRepository accountRepository,
            ProcessedTransactionRepository proctranRepository,
            TransactionReferenceProvider referenceProvider,
            DateTimeProvider dateTimeProvider) {
        this.accountRepository = accountRepository;
        this.proctranRepository = proctranRepository;
        this.referenceProvider = referenceProvider;
        this.dateTimeProvider = dateTimeProvider;
    }

    public DbcrFun(AccountRepository accountRepository,
            ProcessedTransactionRepository proctranRepository) {
        this(accountRepository, proctranRepository,
                new DefaultTransactionReferenceProvider(),
                LocalDateTime::now);
    }

    /**
     * Processes a credit or debit request.
     * This is the entry point equivalent to the PREMIERE SECTION (A010)
     * in DBCRFUN.cbl.
     *
     * @param request the credit/debit request
     * @return the result of the operation
     */
    public CreditDebitResult process(CreditDebitRequest request) {
        logger.entering(DbcrFun.class.getName(), "process");

        CreditDebitResult result = updateAccountDb2(request);

        logger.exiting(DbcrFun.class.getName(), "process");
        return result;
    }

    /**
     * Retrieves the account, validates the operation, applies the
     * amount, updates the account, and writes to PROCTRAN.
     * Maps to UPDATE-ACCOUNT-DB2 SECTION (UAD010) in DBCRFUN.cbl.
     */
    private CreditDebitResult updateAccountDb2(CreditDebitRequest request) {
        logger.entering(DbcrFun.class.getName(), "updateAccountDb2");

        Optional<AccountRecord> accountOpt;
        try {
            accountOpt = accountRepository.findBySortCodeAndAccountNumber(
                    SORT_CODE, request.accountNumber());
        } catch (DataAccessException e) {
            checkForStormDrainDb2(e.getSqlCode());
            return CreditDebitResult.failure(FailCode.DB_ERROR, SORT_CODE);
        }

        if (accountOpt.isEmpty()) {
            return CreditDebitResult.failure(
                    FailCode.ACCOUNT_NOT_FOUND, SORT_CODE);
        }

        AccountRecord account = accountOpt.get();

        if (request.isDebit()) {
            if (isRestrictedAccountType(account) && request.isFromPayment()) {
                return CreditDebitResult.failure(
                        FailCode.OPERATION_NOT_ALLOWED, SORT_CODE);
            }

            BigDecimal difference = account.availableBalance()
                    .add(request.amount());
            if (difference.signum() < 0 && request.isFromPayment()) {
                return CreditDebitResult.failure(
                        FailCode.INSUFFICIENT_FUNDS, SORT_CODE);
            }
        }

        if (isRestrictedAccountType(account) && request.isFromPayment()) {
            return CreditDebitResult.failure(
                    FailCode.OPERATION_NOT_ALLOWED, SORT_CODE);
        }

        BigDecimal newAvailableBalance = account.availableBalance()
                .add(request.amount());
        BigDecimal newActualBalance = account.actualBalance()
                .add(request.amount());

        AccountRecord updatedAccount = account.withUpdatedBalances(
                newAvailableBalance, newActualBalance);

        try {
            accountRepository.updateAccount(updatedAccount);
        } catch (DataAccessException e) {
            checkForStormDrainDb2(e.getSqlCode());
            return CreditDebitResult.failure(FailCode.DB_ERROR, SORT_CODE);
        }

        CreditDebitResult proctranResult = writeToProctranDb2(
                request, updatedAccount);

        if (!proctranResult.success()) {
            return proctranResult;
        }

        return CreditDebitResult.success(
                newAvailableBalance, newActualBalance, SORT_CODE);
    }

    /**
     * Writes a processed transaction record to the PROCTRAN data store.
     * Maps to WRITE-TO-PROCTRAN-DB2 SECTION (WTPD010) in DBCRFUN.cbl.
     */
    private CreditDebitResult writeToProctranDb2(CreditDebitRequest request,
            AccountRecord account) {
        logger.entering(DbcrFun.class.getName(), "writeToProctranDb2");

        LocalDateTime now = dateTimeProvider.now();
        String dateStr = now.format(DATE_FORMATTER);
        String timeStr = now.format(TIME_FORMATTER);
        String reference = referenceProvider.getReference();

        String type;
        String description;

        if (request.isDebit()) {
            type = TransactionType.DEBIT.getCode();
            description = "COUNTER WTHDRW";

            if (request.isFromPayment()) {
                type = TransactionType.PAYMENT_DEBIT.getCode();
                description = truncateOriginDescription(
                        request.originDescription());
            }
        } else {
            type = TransactionType.CREDIT.getCode();
            description = "COUNTER RECVED";

            if (request.isFromPayment()) {
                type = TransactionType.PAYMENT_CREDIT.getCode();
                description = truncateOriginDescription(
                        request.originDescription());
            }
        }

        ProcessedTransactionRecord record = new ProcessedTransactionRecord(
                ProcessedTransactionRecord.EYECATCHER_VALUE,
                SORT_CODE,
                request.accountNumber(),
                dateStr,
                timeStr,
                reference,
                type,
                description,
                request.amount()
        );

        try {
            proctranRepository.insertTransaction(record);
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE,
                    "Unable to write to PROCTRAN DB2 data store, sqlCode={0}",
                    e.getSqlCode());
            checkForStormDrainDb2(e.getSqlCode());
            return CreditDebitResult.failure(FailCode.DB_ERROR, SORT_CODE);
        }

        logger.exiting(DbcrFun.class.getName(), "writeToProctranDb2");
        return CreditDebitResult.success(
                account.availableBalance(), account.actualBalance(), SORT_CODE);
    }

    /**
     * Checks whether the DB2 SQLCODE triggers a storm drain condition.
     * Maps to CHECK-FOR-STORM-DRAIN-DB2 SECTION (CFSDD010) in DBCRFUN.cbl.
     */
    private void checkForStormDrainDb2(int sqlCode) {
        String condition = switch (sqlCode) {
            case 923 -> "DB2 Connection lost";
            default -> "Not Storm Drain";
        };

        if (!"Not Storm Drain".equals(condition)) {
            logger.log(Level.WARNING,
                    "DBCRFUN: Check-For-Storm-Drain-DB2: Storm Drain "
                    + "condition ({0}) has been met ({1}).",
                    new Object[]{condition, sqlCode});
        }
    }

    private boolean isRestrictedAccountType(AccountRecord account) {
        return account.isMortgage() || account.isLoan();
    }

    private String truncateOriginDescription(String origin) {
        if (origin == null) {
            return "";
        }
        if (origin.length() <= ORIGIN_DESC_MAX_LENGTH) {
            return origin;
        }
        return origin.substring(0, ORIGIN_DESC_MAX_LENGTH);
    }

    /**
     * Provides the current date/time. Abstracted for testability,
     * replacing EXEC CICS ASKTIME / FORMATTIME.
     */
    @FunctionalInterface
    public interface DateTimeProvider {
        LocalDateTime now();
    }

    /**
     * Provides a transaction reference number. In the original COBOL,
     * this was EIBTASKN (CICS task number) zero-padded to 12 digits.
     */
    public interface TransactionReferenceProvider {
        String getReference();
    }

    private static final class DefaultTransactionReferenceProvider
            implements TransactionReferenceProvider {

        @Override
        public String getReference() {
            long threadId = Thread.currentThread().threadId();
            return String.format("%012d", threadId % 1_000_000_000_000L);
        }
    }
}
