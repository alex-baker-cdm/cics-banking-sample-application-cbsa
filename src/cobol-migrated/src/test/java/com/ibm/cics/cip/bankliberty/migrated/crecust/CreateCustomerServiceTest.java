/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.crecust;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CreateCustomerServiceTest {

    private StubCreditCheckService creditCheckService;
    private StubCustomerDataStore customerDataStore;
    private StubProcessedTransactionDataStore processedTransactionDataStore;
    private StubNamedCounterService namedCounterService;
    private CreateCustomerService service;

    @BeforeEach
    void setUp() {
        creditCheckService = new StubCreditCheckService();
        customerDataStore = new StubCustomerDataStore();
        processedTransactionDataStore = new StubProcessedTransactionDataStore();
        namedCounterService = new StubNamedCounterService();
        service = new CreateCustomerService(
                creditCheckService,
                customerDataStore,
                processedTransactionDataStore,
                namedCounterService
        );
        service.setRandom(new Random(42));
    }

    private CreateCustomerRequest validRequest() {
        return new CreateCustomerRequest(
                "987654",
                "John Smith",
                "123 Main Street, Springfield, IL 62701",
                LocalDate.of(1990, 3, 15)
        );
    }

    @Nested
    @DisplayName("Successful Customer Creation")
    class SuccessfulCreation {

        @Test
        @DisplayName("creates customer with valid input and returns success")
        void createCustomerSuccess() {
            creditCheckService.setScores(List.of(700, 750, 680));

            CreateCustomerResponse response =
                    service.createCustomer(validRequest());

            assertTrue(response.success());
            assertEquals("CUST", response.eyecatcher());
            assertEquals("987654", response.sortCode());
            assertEquals(1L, response.customerNumber());
            assertEquals("John Smith", response.name());
            assertEquals(" ", response.failCode());
        }

        @Test
        @DisplayName("assigns sequential customer numbers")
        void assignsSequentialCustomerNumbers() {
            creditCheckService.setScores(List.of(700));

            CreateCustomerResponse first =
                    service.createCustomer(validRequest());
            CreateCustomerResponse second =
                    service.createCustomer(validRequest());

            assertEquals(1L, first.customerNumber());
            assertEquals(2L, second.customerNumber());
        }

        @Test
        @DisplayName("computes average credit score from multiple agencies")
        void computesAverageCreditScore() {
            creditCheckService.setScores(List.of(600, 700, 800));

            CreateCustomerResponse response =
                    service.createCustomer(validRequest());

            assertTrue(response.success());
            assertEquals(700, response.creditScore());
        }

        @Test
        @DisplayName("computes average with integer truncation like COBOL")
        void computesAverageWithTruncation() {
            creditCheckService.setScores(List.of(601, 700));

            CreateCustomerResponse response =
                    service.createCustomer(validRequest());

            assertTrue(response.success());
            assertEquals(650, response.creditScore());
        }

        @Test
        @DisplayName("writes customer record with correct data")
        void writesCustomerRecord() {
            creditCheckService.setScores(List.of(750));

            service.createCustomer(validRequest());

            assertEquals(1, customerDataStore.writtenCustomers.size());
            CustomerRecord written = customerDataStore.writtenCustomers.get(0);
            assertEquals("CUST", written.eyecatcher());
            assertEquals("987654", written.sortCode());
            assertEquals(1L, written.customerNumber());
            assertEquals("John Smith", written.name());
            assertEquals(LocalDate.of(1990, 3, 15), written.dateOfBirth());
            assertEquals(750, written.creditScore());
        }

        @Test
        @DisplayName("writes processed transaction record")
        void writesProcessedTransaction() {
            creditCheckService.setScores(List.of(750));

            service.createCustomer(validRequest());

            assertEquals(1, processedTransactionDataStore.writtenRecords.size());
            ProcessedTransactionRecord txn =
                    processedTransactionDataStore.writtenRecords.get(0);
            assertEquals("PRTR", txn.eyecatcher());
            assertEquals("987654", txn.sortCode());
            assertEquals("00000000", txn.accountNumber());
            assertEquals("OCC", txn.type());
            assertEquals(BigDecimal.ZERO, txn.amount());
        }

        @Test
        @DisplayName("processed transaction description contains customer data")
        void processedTransactionDescriptionFormat() {
            creditCheckService.setScores(List.of(750));

            service.createCustomer(validRequest());

            ProcessedTransactionRecord txn =
                    processedTransactionDataStore.writtenRecords.get(0);
            String desc = txn.description();
            assertTrue(desc.startsWith("987654"),
                    "Description should start with sort code");
            assertTrue(desc.contains("0000000001"),
                    "Description should contain customer number");
            assertTrue(desc.contains("John Smith"),
                    "Description should contain name");
            assertTrue(desc.contains("15/03/1990"),
                    "Description should contain DOB");
        }

        @Test
        @DisplayName("enqueues and dequeues named counter")
        void enqueuesAndDequeuesNamedCounter() {
            creditCheckService.setScores(List.of(750));

            service.createCustomer(validRequest());

            assertTrue(namedCounterService.enqueueCount > 0);
            assertTrue(namedCounterService.dequeueCount > 0);
        }

        @Test
        @DisplayName("updates customer control record")
        void updatesCustomerControlRecord() {
            creditCheckService.setScores(List.of(750));

            service.createCustomer(validRequest());

            assertTrue(customerDataStore.controlRecordReadCount >= 1);
            assertTrue(customerDataStore.controlRecordRewriteCount >= 1);
        }

        @Test
        @DisplayName("credit score review date is within 1-20 days from today")
        void creditScoreReviewDateInRange() {
            creditCheckService.setScores(List.of(750));

            CreateCustomerResponse response =
                    service.createCustomer(validRequest());

            LocalDate today = LocalDate.now();
            assertNotNull(response.creditScoreReviewDate());
            assertTrue(
                    !response.creditScoreReviewDate().isBefore(
                            today.plusDays(1)),
                    "Review date should be at least 1 day from today");
            assertTrue(
                    !response.creditScoreReviewDate().isAfter(
                            today.plusDays(20)),
                    "Review date should be at most 20 days from today");
        }
    }

    @Nested
    @DisplayName("Credit Check Failures")
    class CreditCheckFailures {

        @Test
        @DisplayName("returns failure when no credit agencies respond")
        void noCreditAgenciesRespond() {
            creditCheckService.setScores(Collections.emptyList());

            CreateCustomerResponse response =
                    service.createCustomer(validRequest());

            assertFalse(response.success());
            assertEquals("C", response.failCode());
            assertEquals(0, response.creditScore());
        }

        @Test
        @DisplayName("returns failure with null scores from credit service")
        void nullScoresFromCreditService() {
            creditCheckService.setScores(null);

            CreateCustomerResponse response =
                    service.createCustomer(validRequest());

            assertFalse(response.success());
            assertEquals("C", response.failCode());
            assertEquals(0, response.creditScore());
        }

        @Test
        @DisplayName("returns failure on credit check exception with fail code")
        void creditCheckExceptionPropagatesFailCode() {
            creditCheckService.setException(
                    new CreditCheckService.CreditCheckException(
                            "F", "Credit agency abended"));

            CreateCustomerResponse response =
                    service.createCustomer(validRequest());

            assertFalse(response.success());
            assertEquals("F", response.failCode());
            assertEquals(0, response.creditScore());
        }

        @Test
        @DisplayName("sets review date to today on credit check error")
        void setsReviewDateToTodayOnError() {
            creditCheckService.setScores(Collections.emptyList());

            CreateCustomerResponse response =
                    service.createCustomer(validRequest());

            assertEquals(LocalDate.now(), response.creditScoreReviewDate());
        }

        @Test
        @DisplayName("does not write customer record on credit check failure")
        void noCustomerWriteOnCreditCheckFailure() {
            creditCheckService.setScores(Collections.emptyList());

            service.createCustomer(validRequest());

            assertTrue(customerDataStore.writtenCustomers.isEmpty());
        }
    }

    @Nested
    @DisplayName("Date of Birth Validation")
    class DateOfBirthValidation {

        @Test
        @DisplayName("rejects DOB with year before 1601 with fail code O")
        void rejectsDobBefore1601() {
            creditCheckService.setScores(List.of(750));

            CreateCustomerRequest request = new CreateCustomerRequest(
                    "987654", "Ancient Person", "Address",
                    LocalDate.of(1600, 1, 1));

            CreateCustomerResponse response = service.createCustomer(request);

            assertFalse(response.success());
            assertEquals("O", response.failCode());
        }

        @Test
        @DisplayName("accepts DOB with year exactly 1601")
        void acceptsDobYear1601() {
            creditCheckService.setScores(List.of(750));

            CreateCustomerRequest request = new CreateCustomerRequest(
                    "987654", "Old Person", "Address",
                    LocalDate.of(1601, 6, 15));

            CreateCustomerResponse response = service.createCustomer(request);

            // Year 1601 is valid but age > 150 so fail code O
            assertFalse(response.success());
            assertEquals("O", response.failCode());
        }

        @Test
        @DisplayName("rejects DOB when customer age exceeds 150 with fail code O")
        void rejectsAgeOver150() {
            creditCheckService.setScores(List.of(750));

            LocalDate ancientDob = LocalDate.now().minusYears(151);
            CreateCustomerRequest request = new CreateCustomerRequest(
                    "987654", "Very Old Person", "Address",
                    ancientDob);

            CreateCustomerResponse response = service.createCustomer(request);

            assertFalse(response.success());
            assertEquals("O", response.failCode());
        }

        @Test
        @DisplayName("accepts customer age exactly 150")
        void acceptsAge150() {
            creditCheckService.setScores(List.of(750));

            LocalDate dob = LocalDate.now().minusYears(150);
            CreateCustomerRequest request = new CreateCustomerRequest(
                    "987654", "Old Person", "Address", dob);

            CreateCustomerResponse response = service.createCustomer(request);

            assertTrue(response.success());
        }

        @Test
        @DisplayName("rejects DOB in the future with fail code Y")
        void rejectsFutureDob() {
            creditCheckService.setScores(List.of(750));

            CreateCustomerRequest request = new CreateCustomerRequest(
                    "987654", "Future Person", "Address",
                    LocalDate.now().plusDays(1));

            CreateCustomerResponse response = service.createCustomer(request);

            assertFalse(response.success());
            assertEquals("Y", response.failCode());
        }

        @Test
        @DisplayName("accepts DOB of today (newborn)")
        void acceptsTodayDob() {
            creditCheckService.setScores(List.of(750));

            CreateCustomerRequest request = new CreateCustomerRequest(
                    "987654", "Newborn Baby", "Address",
                    LocalDate.now());

            CreateCustomerResponse response = service.createCustomer(request);

            assertTrue(response.success());
        }

        @Test
        @DisplayName("does not write customer record on DOB validation failure")
        void noCustomerWriteOnDobFailure() {
            creditCheckService.setScores(List.of(750));

            CreateCustomerRequest request = new CreateCustomerRequest(
                    "987654", "Future Person", "Address",
                    LocalDate.now().plusDays(1));

            service.createCustomer(request);

            assertTrue(customerDataStore.writtenCustomers.isEmpty());
        }
    }

    @Nested
    @DisplayName("Named Counter Failures")
    class NamedCounterFailures {

        @Test
        @DisplayName("returns failure with code 3 when ENQ fails")
        void enqueueFailure() {
            creditCheckService.setScores(List.of(750));
            namedCounterService.failEnqueue = true;

            CreateCustomerResponse response =
                    service.createCustomer(validRequest());

            assertFalse(response.success());
            assertEquals("3", response.failCode());
        }
    }

    @Nested
    @DisplayName("Data Store Failures")
    class DataStoreFailures {

        @Test
        @DisplayName("returns failure with code 1 when customer write fails")
        void customerWriteFailure() {
            creditCheckService.setScores(List.of(750));
            customerDataStore.failWrite = true;

            CreateCustomerResponse response =
                    service.createCustomer(validRequest());

            assertFalse(response.success());
            assertEquals("1", response.failCode());
        }

        @Test
        @DisplayName("returns failure with code 4 when control record read fails")
        void controlRecordReadFailure() {
            creditCheckService.setScores(List.of(750));
            customerDataStore.failControlRead = true;

            CreateCustomerResponse response =
                    service.createCustomer(validRequest());

            assertFalse(response.success());
            assertEquals("4", response.failCode());
        }

        @Test
        @DisplayName("returns failure with code 4 when control record rewrite fails")
        void controlRecordRewriteFailure() {
            creditCheckService.setScores(List.of(750));
            customerDataStore.failControlRewrite = true;

            CreateCustomerResponse response =
                    service.createCustomer(validRequest());

            assertFalse(response.success());
            assertEquals("4", response.failCode());
        }

        @Test
        @DisplayName("throws ABEND exception when PROCTRAN write fails")
        void proctranWriteFailure() {
            creditCheckService.setScores(List.of(750));
            processedTransactionDataStore.failWrite = true;

            assertThrows(
                    CreateCustomerService.CreateCustomerAbendException.class,
                    () -> service.createCustomer(validRequest()));
        }

        @Test
        @DisplayName("ABEND exception has correct abend code HWPT")
        void proctranAbendCode() {
            creditCheckService.setScores(List.of(750));
            processedTransactionDataStore.failWrite = true;

            CreateCustomerService.CreateCustomerAbendException ex =
                    assertThrows(
                            CreateCustomerService
                                    .CreateCustomerAbendException.class,
                            () -> service.createCustomer(validRequest()));

            assertEquals("HWPT", ex.getAbendCode());
        }

        @Test
        @DisplayName("dequeues named counter on customer write failure")
        void dequeuesOnCustomerWriteFailure() {
            creditCheckService.setScores(List.of(750));
            customerDataStore.failWrite = true;

            service.createCustomer(validRequest());

            assertTrue(namedCounterService.dequeueCount > 0);
        }

        @Test
        @DisplayName("dequeues named counter on PROCTRAN write failure")
        void dequeuesOnProctranWriteFailure() {
            creditCheckService.setScores(List.of(750));
            processedTransactionDataStore.failWrite = true;

            try {
                service.createCustomer(validRequest());
            } catch (CreateCustomerService.CreateCustomerAbendException ignored) {
            }

            assertTrue(namedCounterService.dequeueCount > 0);
        }
    }

    @Nested
    @DisplayName("Request Validation")
    class RequestValidation {

        @Test
        @DisplayName("rejects null sort code")
        void rejectsNullSortCode() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CreateCustomerRequest(
                            null, "Name", "Address",
                            LocalDate.of(1990, 1, 1)));
        }

        @Test
        @DisplayName("rejects blank sort code")
        void rejectsBlankSortCode() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CreateCustomerRequest(
                            "  ", "Name", "Address",
                            LocalDate.of(1990, 1, 1)));
        }

        @Test
        @DisplayName("rejects null name")
        void rejectsNullName() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CreateCustomerRequest(
                            "987654", null, "Address",
                            LocalDate.of(1990, 1, 1)));
        }

        @Test
        @DisplayName("rejects null address")
        void rejectsNullAddress() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CreateCustomerRequest(
                            "987654", "Name", null,
                            LocalDate.of(1990, 1, 1)));
        }

        @Test
        @DisplayName("rejects null date of birth")
        void rejectsNullDob() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CreateCustomerRequest(
                            "987654", "Name", "Address",
                            null));
        }
    }

    @Nested
    @DisplayName("Credit Score Review Date Computation")
    class ReviewDateComputation {

        @Test
        @DisplayName("review date is deterministic with fixed random seed")
        void deterministicWithSeed() {
            service.setRandom(new Random(12345));

            LocalDate today = LocalDate.of(2024, 1, 1);
            LocalDate reviewDate1 = service.computeReviewDate(today);

            service.setRandom(new Random(12345));
            LocalDate reviewDate2 = service.computeReviewDate(today);

            assertEquals(reviewDate1, reviewDate2);
        }

        @Test
        @DisplayName("review date is always between 1 and 20 days ahead")
        void reviewDateRange() {
            LocalDate today = LocalDate.of(2024, 6, 15);
            for (int seed = 0; seed < 100; seed++) {
                service.setRandom(new Random(seed));
                LocalDate reviewDate = service.computeReviewDate(today);
                long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(
                        today, reviewDate);
                assertTrue(daysDiff >= 1 && daysDiff <= 20,
                        "Days difference " + daysDiff
                                + " should be between 1 and 20 (seed="
                                + seed + ")");
            }
        }
    }

    @Nested
    @DisplayName("Date of Birth Validation (Direct)")
    class DateOfBirthValidationDirect {

        @Test
        @DisplayName("accepts valid recent date of birth")
        void validRecentDob() {
            LocalDate today = LocalDate.of(2024, 6, 15);
            assertNull(service.validateDateOfBirth(
                    LocalDate.of(2000, 5, 20), today));
        }

        @Test
        @DisplayName("rejects year 1600")
        void rejectsYear1600() {
            LocalDate today = LocalDate.of(2024, 6, 15);
            assertEquals("O", service.validateDateOfBirth(
                    LocalDate.of(1600, 12, 31), today));
        }

        @Test
        @DisplayName("rejects age over 150")
        void rejectsAgeOver150Direct() {
            LocalDate today = LocalDate.of(2024, 6, 15);
            assertEquals("O", service.validateDateOfBirth(
                    LocalDate.of(1873, 6, 14), today));
        }

        @Test
        @DisplayName("rejects future date")
        void rejectsFutureDate() {
            LocalDate today = LocalDate.of(2024, 6, 15);
            assertEquals("Y", service.validateDateOfBirth(
                    LocalDate.of(2024, 6, 16), today));
        }

        @Test
        @DisplayName("accepts today as DOB")
        void acceptsToday() {
            LocalDate today = LocalDate.of(2024, 6, 15);
            assertNull(service.validateDateOfBirth(today, today));
        }
    }

    @Nested
    @DisplayName("Response Factory Methods")
    class ResponseFactoryMethods {

        @Test
        @DisplayName("success response has correct eyecatcher")
        void successEyecatcher() {
            CreateCustomerResponse response =
                    CreateCustomerResponse.success(
                            "987654", 1L, "Name", "Address",
                            LocalDate.of(1990, 1, 1), 750,
                            LocalDate.of(2024, 7, 1));

            assertEquals("CUST", response.eyecatcher());
            assertTrue(response.success());
            assertEquals(" ", response.failCode());
        }

        @Test
        @DisplayName("failure response has correct structure")
        void failureStructure() {
            CreateCustomerResponse response =
                    CreateCustomerResponse.failure(
                            "C", 0, LocalDate.of(2024, 6, 15));

            assertFalse(response.success());
            assertEquals("C", response.failCode());
            assertEquals(0, response.creditScore());
            assertNull(response.eyecatcher());
        }
    }

    @Nested
    @DisplayName("Customer Record")
    class CustomerRecordTests {

        @Test
        @DisplayName("customer record eyecatcher constant is CUST")
        void eyecatcherConstant() {
            assertEquals("CUST", CustomerRecord.EYECATCHER_VALUE);
        }

        @Test
        @DisplayName("customer record preserves all fields")
        void preservesFields() {
            CustomerRecord record = new CustomerRecord(
                    "CUST", "987654", 42L, "Test Name",
                    "Test Address", LocalDate.of(1985, 12, 25),
                    800, LocalDate.of(2024, 7, 10));

            assertEquals("CUST", record.eyecatcher());
            assertEquals("987654", record.sortCode());
            assertEquals(42L, record.customerNumber());
            assertEquals("Test Name", record.name());
            assertEquals("Test Address", record.address());
            assertEquals(LocalDate.of(1985, 12, 25), record.dateOfBirth());
            assertEquals(800, record.creditScore());
            assertEquals(LocalDate.of(2024, 7, 10),
                    record.creditScoreReviewDate());
        }
    }

    @Nested
    @DisplayName("Processed Transaction Record")
    class ProcessedTransactionRecordTests {

        @Test
        @DisplayName("eyecatcher constant is PRTR")
        void eyecatcherConstant() {
            assertEquals("PRTR",
                    ProcessedTransactionRecord.EYECATCHER_VALUE);
        }

        @Test
        @DisplayName("branch create customer type is OCC")
        void typeConstant() {
            assertEquals("OCC",
                    ProcessedTransactionRecord
                            .TYPE_BRANCH_CREATE_CUSTOMER);
        }
    }

    @Nested
    @DisplayName("Customer Control Record")
    class CustomerControlRecordTests {

        @Test
        @DisplayName("eyecatcher constant is CTRL")
        void eyecatcherConstant() {
            assertEquals("CTRL",
                    CustomerControlRecord.EYECATCHER_VALUE);
        }

        @Test
        @DisplayName("control number constant is 9999999999")
        void controlNumberConstant() {
            assertEquals("9999999999",
                    CustomerControlRecord.CONTROL_NUMBER);
        }

        @Test
        @DisplayName("getters and setters work correctly")
        void gettersAndSetters() {
            CustomerControlRecord record = new CustomerControlRecord();
            record.setEyecatcher("CTRL");
            record.setSortCode("000000");
            record.setControlNumber("9999999999");
            record.setNumberOfCustomers(100);
            record.setLastCustomerNumber(42);
            record.setSuccessFlag("Y");
            record.setFailCode(" ");

            assertEquals("CTRL", record.getEyecatcher());
            assertEquals("000000", record.getSortCode());
            assertEquals("9999999999", record.getControlNumber());
            assertEquals(100, record.getNumberOfCustomers());
            assertEquals(42, record.getLastCustomerNumber());
            assertEquals("Y", record.getSuccessFlag());
            assertEquals(" ", record.getFailCode());
        }
    }

    @Nested
    @DisplayName("Named Counter Service")
    class NamedCounterServiceTests {

        @Test
        @DisplayName("customer counter name constant is CBSACUST")
        void counterNameConstant() {
            assertEquals("CBSACUST",
                    NamedCounterService.CUSTOMER_COUNTER_NAME);
        }
    }

    @Nested
    @DisplayName("End-to-End Scenarios")
    class EndToEndScenarios {

        @Test
        @DisplayName("full flow with single credit agency response")
        void singleAgencyResponse() {
            creditCheckService.setScores(List.of(650));

            CreateCustomerResponse response =
                    service.createCustomer(validRequest());

            assertTrue(response.success());
            assertEquals(650, response.creditScore());
            assertEquals(1L, response.customerNumber());
            assertEquals(1, customerDataStore.writtenCustomers.size());
            assertEquals(1,
                    processedTransactionDataStore.writtenRecords.size());
        }

        @Test
        @DisplayName("full flow with all five agencies responding")
        void allFiveAgenciesRespond() {
            creditCheckService.setScores(List.of(700, 720, 680, 750, 690));

            CreateCustomerResponse response =
                    service.createCustomer(validRequest());

            assertTrue(response.success());
            assertEquals(708, response.creditScore());
        }

        @Test
        @DisplayName("multiple customers created sequentially")
        void multipleCustomersSequential() {
            creditCheckService.setScores(List.of(750));

            for (int i = 0; i < 5; i++) {
                CreateCustomerRequest request = new CreateCustomerRequest(
                        "987654",
                        "Customer " + (i + 1),
                        "Address " + (i + 1),
                        LocalDate.of(1990 + i, 1, 1));

                CreateCustomerResponse response =
                        service.createCustomer(request);

                assertTrue(response.success());
                assertEquals(i + 1, response.customerNumber());
            }

            assertEquals(5, customerDataStore.writtenCustomers.size());
            assertEquals(5,
                    processedTransactionDataStore.writtenRecords.size());
        }

        @Test
        @DisplayName("failure does not increment customer number")
        void failureDoesNotIncrementCustomerNumber() {
            creditCheckService.setScores(List.of(750));

            CreateCustomerResponse success1 =
                    service.createCustomer(validRequest());
            assertTrue(success1.success());
            assertEquals(1L, success1.customerNumber());

            creditCheckService.setScores(Collections.emptyList());
            CreateCustomerResponse failure =
                    service.createCustomer(validRequest());
            assertFalse(failure.success());

            creditCheckService.setScores(List.of(750));
            CreateCustomerResponse success2 =
                    service.createCustomer(validRequest());
            assertTrue(success2.success());
            assertEquals(2L, success2.customerNumber());
        }

        @Test
        @DisplayName("preserves customer address in written record")
        void preservesAddress() {
            creditCheckService.setScores(List.of(750));

            String longAddress = "123 Very Long Street Name, " +
                    "Suite 456, Building Complex Alpha, " +
                    "Springfield, IL 62701, USA";
            CreateCustomerRequest request = new CreateCustomerRequest(
                    "987654", "Test Customer", longAddress,
                    LocalDate.of(1985, 6, 15));

            service.createCustomer(request);

            assertEquals(longAddress,
                    customerDataStore.writtenCustomers.get(0).address());
        }

        @Test
        @DisplayName("handles name longer than 14 chars in description")
        void handlesLongNameInDescription() {
            creditCheckService.setScores(List.of(750));

            CreateCustomerRequest request = new CreateCustomerRequest(
                    "987654",
                    "A Very Long Customer Name That Exceeds Fourteen Characters",
                    "Address",
                    LocalDate.of(1990, 3, 15));

            service.createCustomer(request);

            ProcessedTransactionRecord txn =
                    processedTransactionDataStore.writtenRecords.get(0);
            assertTrue(txn.description().contains("A Very Long Cu"));
        }
    }

    // --- Stub implementations ---

    static class StubCreditCheckService implements CreditCheckService {
        private List<Integer> scores = List.of(750);
        private CreditCheckException exception;

        void setScores(List<Integer> scores) {
            this.scores = scores;
            this.exception = null;
        }

        void setException(CreditCheckException exception) {
            this.exception = exception;
            this.scores = null;
        }

        @Override
        public List<Integer> performCreditChecks(
                CreateCustomerRequest request)
                throws CreditCheckException {
            if (exception != null) {
                throw exception;
            }
            return scores;
        }
    }

    static class StubCustomerDataStore implements CustomerDataStore {
        final List<CustomerRecord> writtenCustomers = new ArrayList<>();
        int controlRecordReadCount = 0;
        int controlRecordRewriteCount = 0;
        boolean failWrite = false;
        boolean failControlRead = false;
        boolean failControlRewrite = false;
        private long lastCustomerNumber = 0;

        @Override
        public void writeCustomer(CustomerRecord record)
                throws DataStoreException {
            if (failWrite) {
                throw new DataStoreException("1",
                        "WRITE to CUSTOMER file failed");
            }
            writtenCustomers.add(record);
        }

        @Override
        public CustomerControlRecord readControlRecordForUpdate(
                String sortCode) throws DataStoreException {
            if (failControlRead) {
                throw new DataStoreException("4",
                        "READ of customer control record failed");
            }
            controlRecordReadCount++;
            return new CustomerControlRecord(
                    "CTRL", "000000", "9999999999",
                    writtenCustomers.size(), lastCustomerNumber,
                    "Y", " ");
        }

        @Override
        public void rewriteControlRecord(
                CustomerControlRecord controlRecord)
                throws DataStoreException {
            if (failControlRewrite) {
                throw new DataStoreException("4",
                        "REWRITE of customer control record failed");
            }
            controlRecordRewriteCount++;
            lastCustomerNumber = controlRecord.getLastCustomerNumber();
        }
    }

    static class StubProcessedTransactionDataStore
            implements ProcessedTransactionDataStore {
        final List<ProcessedTransactionRecord> writtenRecords =
                new ArrayList<>();
        boolean failWrite = false;

        @Override
        public void writeTransaction(ProcessedTransactionRecord record)
                throws ProcessedTransactionException {
            if (failWrite) {
                throw new ProcessedTransactionException("HWPT",
                        "INSERT INTO PROCTRAN failed");
            }
            writtenRecords.add(record);
        }
    }

    static class StubNamedCounterService implements NamedCounterService {
        int enqueueCount = 0;
        int dequeueCount = 0;
        boolean failEnqueue = false;
        boolean failDequeue = false;

        @Override
        public void enqueue(String sortCode) throws NamedCounterException {
            if (failEnqueue) {
                throw new NamedCounterException("3",
                        "ENQ failed for " + sortCode);
            }
            enqueueCount++;
        }

        @Override
        public void dequeue(String sortCode) throws NamedCounterException {
            if (failDequeue) {
                throw new NamedCounterException("5",
                        "DEQ failed for " + sortCode);
            }
            dequeueCount++;
        }
    }
}
