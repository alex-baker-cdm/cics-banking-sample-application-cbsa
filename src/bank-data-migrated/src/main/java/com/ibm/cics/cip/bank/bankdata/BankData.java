/*
 * Copyright IBM Corp. 2023
 *
 * Java 21 migration of BANKDATA.cbl — Batch program to initialise the data
 * used in the bank application.
 *
 * Datastores populated:
 *   - CUSTOMER (originally VSAM, now via CustomerDataStore)
 *   - ACCOUNT  (originally DB2, now via AccountDataStore)
 *   - CONTROL  (originally DB2, now via AccountDataStore)
 *
 * Input parameters: "startKey,endKey,step,randomSeed"
 *   - startKey:    first customer key to generate
 *   - endKey:      last customer key to generate
 *   - step:        increment between customer keys
 *   - randomSeed:  seed for the pseudo-random number generator
 */
package com.ibm.cics.cip.bank.bankdata;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.JulianFields;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BankData {

    private static final Logger LOGGER = Logger.getLogger(BankData.class.getName());
    private static final int COMMIT_INTERVAL = 1000;
    private static final int MAX_OPENED_DATE_ATTEMPTS = 100;

    private final CustomerDataStore customerDataStore;
    private final AccountDataStore accountDataStore;

    private Random random;
    private int lastCustomerNumber;
    private int numberOfCustomers;
    private int lastAccountNumber;
    private int numberOfAccounts;
    private int wsAccountNumber;

    public BankData(CustomerDataStore customerDataStore,
            AccountDataStore accountDataStore) {
        this.customerDataStore = customerDataStore;
        this.accountDataStore = accountDataStore;
    }

    public void execute(long startKey, long endKey, long step,
            long randomSeed) throws BankDataException {
        LOGGER.info("Starting BANKDATA");

        validateParameters(startKey, endKey, step);

        LocalDate today = LocalDate.now();
        long todayInt = today.getLong(JulianFields.JULIAN_DAY);

        deleteDatabaseRows();

        random = new Random(randomSeed);

        try {
            customerDataStore.open();
        } catch (IOException e) {
            throw new BankDataException(
                    "Error opening CUSTOMER data store", e);
        }

        LOGGER.info("Populating Customer + Account files");

        int commitCount = 0;
        lastCustomerNumber = 0;
        numberOfCustomers = 0;
        lastAccountNumber = 0;
        numberOfAccounts = 0;
        wsAccountNumber = 1;

        for (long nextKey = startKey; nextKey <= endKey; nextKey += step) {
            CustomerRecord customer = generateCustomerRecord(nextKey, today,
                    todayInt);

            try {
                customerDataStore.writeCustomer(customer);
            } catch (IOException e) {
                throw new BankDataException(
                        "Error writing to CUSTOMER data store", e);
            }

            generateAccountsForCustomer(customer);

            commitCount++;
            if (commitCount > COMMIT_INTERVAL) {
                commitAccountData();
                commitCount = 0;
            }
        }

        writeCustomerControlRecord();
        insertAccountControlRecords();
        closeCustomerFile();

        LOGGER.info("Finishing BANKDATA");
    }

    private void validateParameters(long startKey, long endKey, long step) {
        if (endKey < startKey) {
            throw new IllegalArgumentException(
                    "Final customer number cannot be smaller than first "
                            + "customer number");
        }
        if (step == 0) {
            throw new IllegalArgumentException(
                    "Gap between customers cannot be zero");
        }
    }

    private void deleteDatabaseRows() {
        String sortCode = ReferenceData.SORT_CODE;
        LOGGER.info("Deleting from ACCOUNT table");

        try {
            accountDataStore.deleteAccountsBySortCode(sortCode);
        } catch (SQLException e) {
            throw new BankDataException(
                    "Error deleting rows from ACCOUNT table for SORTCODE="
                            + sortCode,
                    e);
        }
        LOGGER.info("Deleting from ACCOUNT table COMPLETE");

        String accountLastName = sortCode + "-ACCOUNT-LAST";
        LOGGER.info("Deleting from CONTROL table");
        try {
            accountDataStore.deleteControlRecord(accountLastName);
        } catch (SQLException e) {
            throw new BankDataException(
                    "Error deleting rows from CONTROL table for CONTROL_NAME="
                            + accountLastName,
                    e);
        }
        LOGGER.info("Deleting from CONTROL table COMPLETE");

        String accountCountName = sortCode + "-ACCOUNT-COUNT";
        LOGGER.info("Deleting from CONTROL table");
        try {
            accountDataStore.deleteControlRecord(accountCountName);
        } catch (SQLException e) {
            throw new BankDataException(
                    "Error deleting rows from CONTROL table for CONTROL_NAME="
                            + accountCountName,
                    e);
        }
        LOGGER.info("Deleting from CONTROL table COMPLETE");

        try {
            accountDataStore.commit();
        } catch (SQLException e) {
            throw new BankDataException("Error committing delete operations",
                    e);
        }
    }

    CustomerRecord generateCustomerRecord(long customerKey, LocalDate today,
            long todayInt) {
        lastCustomerNumber = (int) customerKey;
        numberOfCustomers++;

        int titleIndex = randomInt(1, ReferenceData.TITLES.size());
        int forenameIndex = randomInt(1, ReferenceData.FORENAMES.size());
        int initialIndex = randomInt(1, ReferenceData.INITIALS_STRING.length());
        int surnameIndex = randomInt(1, ReferenceData.SURNAMES.size());
        int houseNumber = randomInt(1, 99);
        int streetTreeIndex = randomInt(1,
                ReferenceData.STREET_NAMES_TREE.size());
        int streetRoadIndex = randomInt(1,
                ReferenceData.STREET_NAMES_ROAD.size());
        int townIndex = randomInt(1, ReferenceData.TOWNS.size());

        String title = ReferenceData.TITLES.get(titleIndex - 1);
        String forename = ReferenceData.FORENAMES.get(forenameIndex - 1);
        char initial = ReferenceData.INITIALS_STRING.charAt(initialIndex - 1);
        String surname = ReferenceData.SURNAMES.get(surnameIndex - 1);

        String customerName = title + " " + forename + " " + initial + " "
                + surname;

        String streetTree = ReferenceData.STREET_NAMES_TREE
                .get(streetTreeIndex - 1);
        String streetRoad = ReferenceData.STREET_NAMES_ROAD
                .get(streetRoadIndex - 1);
        String town = ReferenceData.TOWNS.get(townIndex - 1);

        String customerAddress = String.format("%02d", houseNumber) + " "
                + streetTree + " " + streetRoad + ", " + town;

        int birthDay = randomInt(1, 28);
        int birthMonth = randomInt(1, 12);
        int birthYear = randomInt(1900, 2000);

        int creditScore = randomInt(1, 999);

        int reviewDateAdd = randomInt(1, 21);
        long newReviewDateInt = todayInt + reviewDateAdd;
        LocalDate reviewDate = LocalDate.MIN.with(
                JulianFields.JULIAN_DAY, newReviewDateInt);

        return new CustomerRecord(
                CustomerRecord.EYECATCHER_VALUE,
                ReferenceData.SORT_CODE,
                String.format("%010d", customerKey),
                customerName,
                customerAddress,
                birthDay,
                birthMonth,
                birthYear,
                creditScore,
                reviewDate.getDayOfMonth(),
                reviewDate.getMonthValue(),
                reviewDate.getYear());
    }

    void generateAccountsForCustomer(CustomerRecord customer) {
        int numAccounts = randomInt(1, 5);

        for (int i = 1; i <= numAccounts; i++) {
            AccountRecord account = generateAccountRecord(customer, i);

            try {
                accountDataStore.insertAccount(account);
            } catch (SQLException e) {
                throw new BankDataException(
                        "Error inserting rows on ACCOUNT table for ACC no="
                                + account.accountNumber() + " for SORTCODE="
                                + account.sortCode(),
                        e);
            }

            lastAccountNumber = wsAccountNumber;
            numberOfAccounts++;
            wsAccountNumber++;
        }

        commitAccountData();
    }

    AccountRecord generateAccountRecord(CustomerRecord customer, int typeIndex) {
        String openedDate = generateOpenedDate(customer.birthYear());

        String accountType = ReferenceData.ACCOUNT_TYPES.get(typeIndex - 1);
        BigDecimal interestRate = ReferenceData.ACCOUNT_INTEREST_RATES
                .get(typeIndex - 1);
        int overdraftLimit = ReferenceData.ACCOUNT_OVERDRAFT_LIMITS
                .get(typeIndex - 1);

        BigDecimal balance = BigDecimal
                .valueOf(randomInt(1, 999999));

        if ("LOAN".equals(accountType) || "MORTGAGE".equals(accountType)) {
            balance = balance.negate();
        }

        return new AccountRecord(
                AccountRecord.EYECATCHER_VALUE,
                customer.customerNumber(),
                ReferenceData.SORT_CODE,
                String.format("%08d", wsAccountNumber),
                accountType,
                interestRate,
                openedDate,
                overdraftLimit,
                ReferenceData.LAST_STATEMENT_DATE,
                ReferenceData.NEXT_STATEMENT_DATE,
                balance,
                balance);
    }

    String generateOpenedDate(int customerBirthYear) {
        int attempts = 0;
        int openedDay;
        int openedMonth;
        int openedYear;

        while (true) {
            openedDay = randomInt(1, 28);
            openedMonth = randomInt(1, 12);
            openedYear = randomInt(customerBirthYear, 2014);

            attempts++;

            if (openedYear > customerBirthYear) {
                return String.format("%02d.%02d.%04d", openedDay, openedMonth,
                        openedYear);
            }

            if (attempts > MAX_OPENED_DATE_ATTEMPTS) {
                return String.format("%02d.%02d.%04d", openedDay, openedMonth,
                        openedYear);
            }
        }
    }

    private void writeCustomerControlRecord() {
        CustomerControlRecord controlRecord = new CustomerControlRecord(
                CustomerControlRecord.EYECATCHER_VALUE,
                "000000",
                CustomerControlRecord.CONTROL_NUMBER,
                numberOfCustomers,
                lastCustomerNumber);

        try {
            customerDataStore.writeControlRecord(controlRecord);
        } catch (IOException e) {
            throw new BankDataException(
                    "Error writing CUSTOMER-CONTROL-RECORD", e);
        }
    }

    private void insertAccountControlRecords() {
        String sortCode = ReferenceData.SORT_CODE;

        String accountLastName = sortCode + "-ACCOUNT-LAST";
        try {
            accountDataStore.insertControlRecord(accountLastName,
                    lastAccountNumber, "");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                    "Error inserting last account control record", e);
        }

        String accountCountName = sortCode + "-ACCOUNT-COUNT";
        try {
            accountDataStore.insertControlRecord(accountCountName,
                    numberOfAccounts, "");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                    "Error inserting account count control record", e);
        }
    }

    private void commitAccountData() {
        try {
            accountDataStore.commit();
        } catch (SQLException e) {
            throw new BankDataException("Error committing account data", e);
        }
    }

    private void closeCustomerFile() {
        try {
            customerDataStore.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing customer data store", e);
        }
    }

    /**
     * Generates a random integer in the range [min, max] (inclusive),
     * matching the COBOL pattern: COMPUTE result = ((max - min) * FUNCTION
     * RANDOM) + min
     */
    int randomInt(int min, int max) {
        return (int) ((max - min) * random.nextDouble()) + min;
    }

    int getLastCustomerNumber() {
        return lastCustomerNumber;
    }

    int getNumberOfCustomers() {
        return numberOfCustomers;
    }

    int getLastAccountNumber() {
        return lastAccountNumber;
    }

    int getNumberOfAccounts() {
        return numberOfAccounts;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println(
                    "Usage: BankData <startKey>,<endKey>,<step>,<randomSeed>");
            System.exit(12);
        }

        String paramString = String.join(" ", args);
        String[] params = paramString.split("[,\\s]+");

        if (params.length < 4) {
            System.err.println(
                    "Usage: BankData <startKey>,<endKey>,<step>,<randomSeed>");
            System.exit(12);
        }

        long startKey = Long.parseLong(params[0].trim());
        long endKey = Long.parseLong(params[1].trim());
        long step = Long.parseLong(params[2].trim());
        long randomSeed = Long.parseLong(params[3].trim());

        System.out.println("BANKDATA: This program requires configured "
                + "CustomerDataStore and AccountDataStore implementations.");
        System.out.println("Parameters parsed: startKey=" + startKey
                + ", endKey=" + endKey + ", step=" + step
                + ", randomSeed=" + randomSeed);
        System.out.println("Provide JDBC connection and VSAM file path "
                + "to run data generation.");
    }
}
