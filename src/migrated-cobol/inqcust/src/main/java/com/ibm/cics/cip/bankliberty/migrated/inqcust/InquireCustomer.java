/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bankliberty.migrated.inqcust;

import java.util.Optional;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java 21 migration of the INQCUST.cbl COBOL program.
 *
 * <p>This program takes a customer number as input and returns
 * the corresponding customer information. It supports three modes
 * of operation:
 * <ul>
 *   <li><b>Direct lookup</b> — a specific customer number is provided</li>
 *   <li><b>Random customer</b> — customer number 0 triggers random selection</li>
 *   <li><b>Last customer</b> — customer number 9999999999 retrieves the
 *       highest-numbered customer</li>
 * </ul>
 *
 * <p>The original COBOL retry semantics (SYSIDERR retries, random-regeneration
 * retries) are preserved. VSAM RLS abend "storm drain" handling is mapped to
 * a typed {@link CustomerDataAccessException}.
 */
public class InquireCustomer {

    private static final Logger logger =
            Logger.getLogger(InquireCustomer.class.getName());

    static final int DEFAULT_SORT_CODE = 987654;

    static final int MAX_RANDOM_RETRIES = 1000;

    private final CustomerDao customerDao;
    private final int sortCode;
    private final Random random;

    public InquireCustomer(CustomerDao customerDao) {
        this(customerDao, DEFAULT_SORT_CODE, new Random());
    }

    public InquireCustomer(CustomerDao customerDao, int sortCode) {
        this(customerDao, sortCode, new Random());
    }

    public InquireCustomer(CustomerDao customerDao, int sortCode,
            Random random) {
        this.customerDao = customerDao;
        this.sortCode = sortCode;
        this.random = random;
    }

    /**
     * Inquire about a customer.
     *
     * @param request the inquiry request containing the customer number
     * @return the inquiry response with customer data or failure information
     */
    public InquireCustomerResponse inquireCustomer(
            InquireCustomerRequest request) {
        long requestedCustomerNumber = request.customerNumber();

        logger.log(Level.FINE, () -> "inquireCustomer called with customerNumber="
                + requestedCustomerNumber);

        boolean isRandom = requestedCustomerNumber
                == InquireCustomerRequest.RANDOM_CUSTOMER;
        boolean isLast = requestedCustomerNumber
                == InquireCustomerRequest.LAST_CUSTOMER;

        long highestCustomerNumber = 0;

        if (isRandom || isLast) {
            Optional<Long> lastCustOpt = getLastCustomerNumber();
            if (lastCustOpt.isEmpty()) {
                logger.warning("Failed to retrieve last customer number");
                return InquireCustomerResponse.dataAccessError();
            }
            highestCustomerNumber = lastCustOpt.get();
            logger.log(Level.FINE, () -> "Highest customer number from data store: "
                    + lastCustOpt.get());
        }

        long customerNumberToLookUp;

        if (isLast) {
            customerNumberToLookUp = highestCustomerNumber;
        } else if (isRandom) {
            customerNumberToLookUp = generateRandomCustomerNumber(
                    highestCustomerNumber);
        } else {
            customerNumberToLookUp = requestedCustomerNumber;
        }

        return readCustomerWithRetry(customerNumberToLookUp, isRandom, isLast,
                highestCustomerNumber);
    }

    private InquireCustomerResponse readCustomerWithRetry(
            long customerNumber, boolean isRandom, boolean isLast,
            long highestCustomerNumber) {
        int retryCount = 0;
        boolean lastCustomerRetried = false;
        long currentCustomerNumber = customerNumber;

        while (true) {
            try {
                Optional<CustomerRecord> result = customerDao.readCustomer(
                        sortCode, currentCustomerNumber);

                if (result.isPresent()) {
                    logger.log(Level.FINE, () -> "Customer found successfully");
                    return InquireCustomerResponse.success(result.get());
                }

                if (isRandom) {
                    if (retryCount < MAX_RANDOM_RETRIES) {
                        retryCount++;
                        currentCustomerNumber =
                                generateRandomCustomerNumberAgain(
                                        highestCustomerNumber);
                        logger.log(Level.FINE,
                                () -> "Random customer not found, retrying");
                        continue;
                    } else {
                        logger.info("Exhausted random customer retries");
                        return InquireCustomerResponse.notFound(
                                currentCustomerNumber);
                    }
                }

                if (isLast && !lastCustomerRetried) {
                    lastCustomerRetried = true;
                    Optional<Long> refreshed = getLastCustomerNumber();
                    if (refreshed.isPresent()) {
                        currentCustomerNumber = refreshed.get();
                        logger.log(Level.FINE,
                                () -> "Retrying with refreshed last customer number");
                        continue;
                    }
                }

                long finalCustNo = currentCustomerNumber;
                logger.info(() -> "Customer " + finalCustNo
                        + " not found");
                return InquireCustomerResponse.notFound(finalCustNo);

            } catch (CustomerDataAccessException e) {
                if (isStormDrainAbend(e.getAbendCode())) {
                    logger.warning(() -> "Storm drain condition met: "
                            + e.getAbendCode());
                    return InquireCustomerResponse.stormDrain();
                }
                logger.log(Level.SEVERE,
                        "Unrecoverable data access error reading customer", e);
                return InquireCustomerResponse.dataAccessError();
            }
        }
    }

    private Optional<Long> getLastCustomerNumber() {
        try {
            Optional<CustomerRecord> lastCustomer =
                    customerDao.readLastCustomer(sortCode);
            return lastCustomer.map(CustomerRecord::customerNumber);
        } catch (CustomerDataAccessException e) {
            logger.log(Level.SEVERE,
                    "Error retrieving last customer number", e);
            return Optional.empty();
        }
    }

    long generateRandomCustomerNumber(long highestCustomerNumber) {
        if (highestCustomerNumber <= 1) {
            return 1;
        }
        return (long) (((highestCustomerNumber - 1) * random.nextDouble())
                + 1);
    }

    long generateRandomCustomerNumberAgain(long highestCustomerNumber) {
        if (highestCustomerNumber <= 1) {
            return 1;
        }
        return (long) (((highestCustomerNumber - 1) * random.nextDouble())
                + 1);
    }

    private boolean isStormDrainAbend(String abendCode) {
        return "AFCR".equals(abendCode)
                || "AFCS".equals(abendCode)
                || "AFCT".equals(abendCode);
    }
}
