/*
 * Copyright IBM Corp. 2023
 *
 * Java 21 migration of COBOL program DELACC.cbl (Delete Account).
 *
 * Original COBOL flow:
 *   1. Read the account record from DB2 by account number + sort code.
 *   2. If not found, return failure (DELACC-DEL-FAIL-CD = '1').
 *   3. If found, delete the account row from DB2.
 *   4. If the delete fails, return failure (DELACC-DEL-FAIL-CD = '3').
 *   5. On successful delete, write an audit record to PROCTRAN.
 *   6. If PROCTRAN write fails, abend with code 'HWPT'.
 *   7. Return the deleted account data via the COMMAREA.
 *
 * This Java class preserves all business logic and error-handling behavior
 * of the original COBOL, translated into idiomatic Java 21.
 */
package com.ibm.cics.cip.bank.delacc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeleteAccountService {

    private static final Logger logger =
        Logger.getLogger(DeleteAccountService.class.getName());

    private static final String DEFAULT_SORT_CODE = "987654";

    static final String PROCTRAN_EYECATCHER = "PRTR";
    static final String TRANSACTION_TYPE_BRANCH_DELETE = "ODA";

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("HHmmss");

    private final AccountDao accountDao;
    private final ProcessedTransactionDao processedTransactionDao;
    private final String sortCode;

    public DeleteAccountService(AccountDao accountDao,
                                ProcessedTransactionDao processedTransactionDao) {
        this(accountDao, processedTransactionDao, DEFAULT_SORT_CODE);
    }

    public DeleteAccountService(AccountDao accountDao,
                                ProcessedTransactionDao processedTransactionDao,
                                String sortCode) {
        this.accountDao = Objects.requireNonNull(accountDao, "accountDao must not be null");
        this.processedTransactionDao = Objects.requireNonNull(
            processedTransactionDao, "processedTransactionDao must not be null");
        this.sortCode = Objects.requireNonNull(sortCode, "sortCode must not be null");
    }

    public DeleteAccountResult deleteAccount(DeleteAccountRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        logger.log(Level.FINE, () -> "Deleting account " + request.accountNumber()
            + " with sort code " + sortCode);

        DeleteAccountResult result = readAccount(request);

        if (result.isDeleteSuccess()) {
            result = performDelete(result, request);

            if (result.isDeleteSuccess()) {
                writeProcessedTransaction(result);
            }
        }

        return result;
    }

    private DeleteAccountResult readAccount(DeleteAccountRequest request) {
        String accountNumber = request.accountNumber();

        Optional<AccountRecord> accountOpt;
        try {
            accountOpt = accountDao.findByAccountNumberAndSortCode(
                accountNumber, sortCode);
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE,
                () -> "Database error reading ACCOUNT for account "
                    + accountNumber + " and sort code " + sortCode
                    + ": " + e.getMessage());
            throw new DeleteAccountAbendException("HRAC",
                "RAD010 - Issue with ACCOUNT row select, for Account "
                    + accountNumber + " and SORTCODE " + sortCode, e);
        }

        if (accountOpt.isEmpty()) {
            logger.log(Level.FINE,
                () -> "Account " + accountNumber + " not found");
            return DeleteAccountResult.notFound(sortCode, accountNumber);
        }

        return DeleteAccountResult.fromAccountRecord(accountOpt.get());
    }

    private DeleteAccountResult performDelete(DeleteAccountResult result,
                                              DeleteAccountRequest request) {
        String accountNumber = request.accountNumber();

        boolean deleted;
        try {
            deleted = accountDao.deleteByAccountNumberAndSortCode(
                accountNumber, sortCode);
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE,
                () -> "Database error deleting ACCOUNT for account "
                    + accountNumber + " and sort code " + sortCode
                    + ": " + e.getMessage());
            throw new DeleteAccountAbendException("HRAC",
                "DADB010 - Issue with ACCOUNT row delete, for Account "
                    + accountNumber + " and SORTCODE " + sortCode, e);
        }

        if (!deleted) {
            result.setDeleteSuccess(false);
            result.setDeleteFailCode("3");
            return result;
        }

        return result;
    }

    private void writeProcessedTransaction(DeleteAccountResult result) {
        LocalDate now = LocalDate.now();
        LocalTime timeNow = LocalTime.now();

        String description = buildDeleteDescription(result);

        try {
            processedTransactionDao.insertDeleteAccountTransaction(
                result.getSortCode(),
                result.getAccountNumber(),
                now,
                timeNow,
                generateReference(),
                TRANSACTION_TYPE_BRANCH_DELETE,
                description,
                result.getActualBalance());
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE,
                () -> "UNABLE TO WRITE TO PROCTRAN ROW DATASTORE: "
                    + e.getMessage());
            throw new DeleteAccountAbendException("HWPT",
                "WPD010 - Unable to WRITE to PROCTRAN row datastore", e);
        }
    }

    String buildDeleteDescription(DeleteAccountResult result) {
        String customerNumber = padLeft(result.getCustomerNumber(), 10, '0');
        String accountType = padRight(result.getAccountType(), 8, ' ');

        String lastDd = result.getLastStatementDate() != null
            ? String.format("%02d", result.getLastStatementDate().getDayOfMonth())
            : "00";
        String lastMm = result.getLastStatementDate() != null
            ? String.format("%02d", result.getLastStatementDate().getMonthValue())
            : "00";
        String lastYyyy = result.getLastStatementDate() != null
            ? String.format("%04d", result.getLastStatementDate().getYear())
            : "0000";

        String nextDd = result.getNextStatementDate() != null
            ? String.format("%02d", result.getNextStatementDate().getDayOfMonth())
            : "00";
        String nextMm = result.getNextStatementDate() != null
            ? String.format("%02d", result.getNextStatementDate().getMonthValue())
            : "00";
        String nextYyyy = result.getNextStatementDate() != null
            ? String.format("%04d", result.getNextStatementDate().getYear())
            : "0000";

        return customerNumber
            + accountType
            + lastDd + lastMm + lastYyyy
            + nextDd + nextMm + nextYyyy
            + "DELETE";
    }

    private String generateReference() {
        long threadId = Thread.currentThread().threadId();
        return String.format("%012d", threadId % 1_000_000_000_000L);
    }

    private static String padLeft(String value, int length, char padChar) {
        if (value == null) {
            return String.valueOf(padChar).repeat(length);
        }
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        return String.valueOf(padChar).repeat(length - value.length()) + value;
    }

    private static String padRight(String value, int length, char padChar) {
        if (value == null) {
            return String.valueOf(padChar).repeat(length);
        }
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        return value + String.valueOf(padChar).repeat(length - value.length());
    }

    String getSortCode() {
        return sortCode;
    }
}
