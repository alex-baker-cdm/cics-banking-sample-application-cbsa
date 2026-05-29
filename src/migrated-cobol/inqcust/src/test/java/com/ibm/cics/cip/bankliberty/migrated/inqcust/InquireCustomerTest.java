/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bankliberty.migrated.inqcust;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InquireCustomerTest {

    private static final int SORT_CODE = 987654;

    @Mock
    private CustomerDao customerDao;

    private InquireCustomer inquireCustomer;

    private static CustomerRecord sampleCustomer(long customerNumber) {
        return new CustomerRecord(
                "CUST",
                SORT_CODE,
                customerNumber,
                "Mr John Smith",
                "123 High Street, London, UK",
                LocalDate.of(1990, 5, 15),
                750,
                LocalDate.of(2024, 1, 10)
        );
    }

    @Nested
    @DisplayName("Direct Customer Lookup")
    class DirectLookup {

        @BeforeEach
        void setUp() {
            inquireCustomer = new InquireCustomer(customerDao, SORT_CODE);
        }

        @Test
        @DisplayName("should return customer data when customer exists")
        void shouldReturnCustomerWhenFound() throws Exception {
            long custNo = 1234567890L;
            CustomerRecord record = sampleCustomer(custNo);
            when(customerDao.readCustomer(SORT_CODE, custNo))
                    .thenReturn(Optional.of(record));

            InquireCustomerResponse response = inquireCustomer.inquireCustomer(
                    new InquireCustomerRequest(custNo));

            assertTrue(response.inquirySuccess());
            assertEquals('0', response.failureCode());
            assertEquals("CUST", response.eyecatcher());
            assertEquals(String.valueOf(SORT_CODE), response.sortCode());
            assertEquals(custNo, response.customerNumber());
            assertEquals("Mr John Smith", response.name());
            assertEquals("123 High Street, London, UK", response.address());
            assertEquals(LocalDate.of(1990, 5, 15), response.dateOfBirth());
            assertEquals(750, response.creditScore());
            assertEquals(LocalDate.of(2024, 1, 10),
                    response.creditScoreReviewDate());
        }

        @Test
        @DisplayName("should return not-found when customer does not exist")
        void shouldReturnNotFoundWhenCustomerMissing() throws Exception {
            long custNo = 5555555555L;
            when(customerDao.readCustomer(SORT_CODE, custNo))
                    .thenReturn(Optional.empty());

            InquireCustomerResponse response = inquireCustomer.inquireCustomer(
                    new InquireCustomerRequest(custNo));

            assertFalse(response.inquirySuccess());
            assertEquals('1', response.failureCode());
            assertEquals(custNo, response.customerNumber());
        }

        @Test
        @DisplayName("should return data-access-error on storm-drain abend (AFCR)")
        void shouldHandleStormDrainAfcr() throws Exception {
            long custNo = 1000L;
            when(customerDao.readCustomer(SORT_CODE, custNo))
                    .thenThrow(new CustomerDataAccessException(
                            "VSAM RLS abend", "AFCR"));

            InquireCustomerResponse response = inquireCustomer.inquireCustomer(
                    new InquireCustomerRequest(custNo));

            assertFalse(response.inquirySuccess());
            assertEquals('2', response.failureCode());
        }

        @Test
        @DisplayName("should return data-access-error on storm-drain abend (AFCS)")
        void shouldHandleStormDrainAfcs() throws Exception {
            long custNo = 1000L;
            when(customerDao.readCustomer(SORT_CODE, custNo))
                    .thenThrow(new CustomerDataAccessException(
                            "VSAM RLS abend", "AFCS"));

            InquireCustomerResponse response = inquireCustomer.inquireCustomer(
                    new InquireCustomerRequest(custNo));

            assertFalse(response.inquirySuccess());
            assertEquals('2', response.failureCode());
        }

        @Test
        @DisplayName("should return data-access-error on storm-drain abend (AFCT)")
        void shouldHandleStormDrainAfct() throws Exception {
            long custNo = 1000L;
            when(customerDao.readCustomer(SORT_CODE, custNo))
                    .thenThrow(new CustomerDataAccessException(
                            "VSAM RLS abend", "AFCT"));

            InquireCustomerResponse response = inquireCustomer.inquireCustomer(
                    new InquireCustomerRequest(custNo));

            assertFalse(response.inquirySuccess());
            assertEquals('2', response.failureCode());
        }

        @Test
        @DisplayName("should return data-access-error on non-storm-drain abend")
        void shouldHandleNonStormDrainAbend() throws Exception {
            long custNo = 1000L;
            when(customerDao.readCustomer(SORT_CODE, custNo))
                    .thenThrow(new CustomerDataAccessException(
                            "VSAM error", "CVR1"));

            InquireCustomerResponse response = inquireCustomer.inquireCustomer(
                    new InquireCustomerRequest(custNo));

            assertFalse(response.inquirySuccess());
            assertEquals('9', response.failureCode());
        }
    }

    @Nested
    @DisplayName("Last Customer Lookup (9999999999)")
    class LastCustomerLookup {

        @BeforeEach
        void setUp() {
            inquireCustomer = new InquireCustomer(customerDao, SORT_CODE);
        }

        @Test
        @DisplayName("should return the last customer record")
        void shouldReturnLastCustomer() throws Exception {
            long lastCustNo = 42L;
            CustomerRecord lastRecord = sampleCustomer(lastCustNo);
            when(customerDao.readLastCustomer(SORT_CODE))
                    .thenReturn(Optional.of(lastRecord));
            when(customerDao.readCustomer(SORT_CODE, lastCustNo))
                    .thenReturn(Optional.of(lastRecord));

            InquireCustomerResponse response = inquireCustomer.inquireCustomer(
                    new InquireCustomerRequest(
                            InquireCustomerRequest.LAST_CUSTOMER));

            assertTrue(response.inquirySuccess());
            assertEquals(lastCustNo, response.customerNumber());
            assertEquals("Mr John Smith", response.name());
        }

        @Test
        @DisplayName("should retry when last customer is not found on first read")
        void shouldRetryWhenLastCustomerNotFoundInitially() throws Exception {
            long lastCustNo = 50L;
            long refreshedCustNo = 49L;
            CustomerRecord lastRecord = sampleCustomer(lastCustNo);
            CustomerRecord refreshedRecord = sampleCustomer(refreshedCustNo);

            when(customerDao.readLastCustomer(SORT_CODE))
                    .thenReturn(Optional.of(lastRecord))
                    .thenReturn(Optional.of(refreshedRecord));
            when(customerDao.readCustomer(SORT_CODE, lastCustNo))
                    .thenReturn(Optional.empty());
            when(customerDao.readCustomer(SORT_CODE, refreshedCustNo))
                    .thenReturn(Optional.of(refreshedRecord));

            InquireCustomerResponse response = inquireCustomer.inquireCustomer(
                    new InquireCustomerRequest(
                            InquireCustomerRequest.LAST_CUSTOMER));

            assertTrue(response.inquirySuccess());
            assertEquals(refreshedCustNo, response.customerNumber());
            verify(customerDao, times(2)).readLastCustomer(SORT_CODE);
        }

        @Test
        @DisplayName("should return not-found when last customer cannot be found after retry")
        void shouldReturnNotFoundAfterRetryExhausted() throws Exception {
            long lastCustNo = 50L;
            long refreshedCustNo = 49L;

            when(customerDao.readLastCustomer(SORT_CODE))
                    .thenReturn(Optional.of(sampleCustomer(lastCustNo)))
                    .thenReturn(Optional.of(sampleCustomer(refreshedCustNo)));
            when(customerDao.readCustomer(SORT_CODE, lastCustNo))
                    .thenReturn(Optional.empty());
            when(customerDao.readCustomer(SORT_CODE, refreshedCustNo))
                    .thenReturn(Optional.empty());

            InquireCustomerResponse response = inquireCustomer.inquireCustomer(
                    new InquireCustomerRequest(
                            InquireCustomerRequest.LAST_CUSTOMER));

            assertFalse(response.inquirySuccess());
            assertEquals('1', response.failureCode());
        }

        @Test
        @DisplayName("should return data-access-error when no customers exist at all")
        void shouldReturnErrorWhenNoCustomersExist() throws Exception {
            when(customerDao.readLastCustomer(SORT_CODE))
                    .thenReturn(Optional.empty());

            InquireCustomerResponse response = inquireCustomer.inquireCustomer(
                    new InquireCustomerRequest(
                            InquireCustomerRequest.LAST_CUSTOMER));

            assertFalse(response.inquirySuccess());
            assertEquals('9', response.failureCode());
        }

        @Test
        @DisplayName("should return data-access-error when readLastCustomer throws")
        void shouldReturnErrorOnException() throws Exception {
            when(customerDao.readLastCustomer(SORT_CODE))
                    .thenThrow(new CustomerDataAccessException(
                            "I/O error", "CVR1"));

            InquireCustomerResponse response = inquireCustomer.inquireCustomer(
                    new InquireCustomerRequest(
                            InquireCustomerRequest.LAST_CUSTOMER));

            assertFalse(response.inquirySuccess());
            assertEquals('9', response.failureCode());
        }
    }

    @Nested
    @DisplayName("Random Customer Lookup (0)")
    class RandomCustomerLookup {

        @Test
        @DisplayName("should return a random customer on first attempt")
        void shouldReturnRandomCustomerOnFirstTry() throws Exception {
            Random fixedRandom = new Random(42);
            inquireCustomer = new InquireCustomer(customerDao, SORT_CODE,
                    fixedRandom);

            long highestCustNo = 100L;
            when(customerDao.readLastCustomer(SORT_CODE))
                    .thenReturn(Optional.of(sampleCustomer(highestCustNo)));

            Random predictRandom = new Random(42);
            long expectedCustNo = (long) (((highestCustNo - 1)
                    * predictRandom.nextDouble()) + 1);

            CustomerRecord record = sampleCustomer(expectedCustNo);
            when(customerDao.readCustomer(SORT_CODE, expectedCustNo))
                    .thenReturn(Optional.of(record));

            InquireCustomerResponse response = inquireCustomer.inquireCustomer(
                    new InquireCustomerRequest(
                            InquireCustomerRequest.RANDOM_CUSTOMER));

            assertTrue(response.inquirySuccess());
            assertEquals(expectedCustNo, response.customerNumber());
        }

        @Test
        @DisplayName("should retry with new random number when first random not found")
        void shouldRetryWithNewRandomNumber() throws Exception {
            Random fixedRandom = new Random(123);
            inquireCustomer = new InquireCustomer(customerDao, SORT_CODE,
                    fixedRandom);

            long highestCustNo = 500L;
            when(customerDao.readLastCustomer(SORT_CODE))
                    .thenReturn(Optional.of(sampleCustomer(highestCustNo)));

            Random predictRandom = new Random(123);
            long firstCustNo = (long) (((highestCustNo - 1)
                    * predictRandom.nextDouble()) + 1);
            long secondCustNo = (long) (((highestCustNo - 1)
                    * predictRandom.nextDouble()) + 1);

            when(customerDao.readCustomer(SORT_CODE, firstCustNo))
                    .thenReturn(Optional.empty());
            when(customerDao.readCustomer(SORT_CODE, secondCustNo))
                    .thenReturn(Optional.of(sampleCustomer(secondCustNo)));

            InquireCustomerResponse response = inquireCustomer.inquireCustomer(
                    new InquireCustomerRequest(
                            InquireCustomerRequest.RANDOM_CUSTOMER));

            assertTrue(response.inquirySuccess());
            assertEquals(secondCustNo, response.customerNumber());
        }

        @Test
        @DisplayName("should return not-found after exhausting all random retries")
        void shouldReturnNotFoundAfterMaxRetries() throws Exception {
            inquireCustomer = new InquireCustomer(customerDao, SORT_CODE,
                    new Random(0));

            long highestCustNo = 10L;
            when(customerDao.readLastCustomer(SORT_CODE))
                    .thenReturn(Optional.of(sampleCustomer(highestCustNo)));
            when(customerDao.readCustomer(eq(SORT_CODE), anyLong()))
                    .thenReturn(Optional.empty());

            InquireCustomerResponse response = inquireCustomer.inquireCustomer(
                    new InquireCustomerRequest(
                            InquireCustomerRequest.RANDOM_CUSTOMER));

            assertFalse(response.inquirySuccess());
            assertEquals('1', response.failureCode());
            verify(customerDao, atLeast(InquireCustomer.MAX_RANDOM_RETRIES))
                    .readCustomer(eq(SORT_CODE), anyLong());
        }

        @Test
        @DisplayName("should return data-access-error when highest customer lookup fails")
        void shouldReturnErrorWhenHighestCustomerFails() throws Exception {
            inquireCustomer = new InquireCustomer(customerDao, SORT_CODE);
            when(customerDao.readLastCustomer(SORT_CODE))
                    .thenReturn(Optional.empty());

            InquireCustomerResponse response = inquireCustomer.inquireCustomer(
                    new InquireCustomerRequest(
                            InquireCustomerRequest.RANDOM_CUSTOMER));

            assertFalse(response.inquirySuccess());
            assertEquals('9', response.failureCode());
        }

        @Test
        @DisplayName("should handle storm-drain abend during random lookup")
        void shouldHandleStormDrainDuringRandomLookup() throws Exception {
            inquireCustomer = new InquireCustomer(customerDao, SORT_CODE,
                    new Random(99));

            long highestCustNo = 100L;
            when(customerDao.readLastCustomer(SORT_CODE))
                    .thenReturn(Optional.of(sampleCustomer(highestCustNo)));
            when(customerDao.readCustomer(eq(SORT_CODE), anyLong()))
                    .thenThrow(new CustomerDataAccessException(
                            "VSAM RLS abend", "AFCR"));

            InquireCustomerResponse response = inquireCustomer.inquireCustomer(
                    new InquireCustomerRequest(
                            InquireCustomerRequest.RANDOM_CUSTOMER));

            assertFalse(response.inquirySuccess());
            assertEquals('2', response.failureCode());
        }
    }

    @Nested
    @DisplayName("Random Number Generation")
    class RandomNumberGeneration {

        @Test
        @DisplayName("should generate customer number in valid range")
        void shouldGenerateInValidRange() {
            Random fixedRandom = new Random(42);
            inquireCustomer = new InquireCustomer(customerDao, SORT_CODE,
                    fixedRandom);

            long highestCustomer = 1000L;
            for (int i = 0; i < 100; i++) {
                long generated = inquireCustomer
                        .generateRandomCustomerNumber(highestCustomer);
                assertTrue(generated >= 1,
                        "Generated number should be >= 1, was " + generated);
                assertTrue(generated < highestCustomer,
                        "Generated number should be < " + highestCustomer
                                + ", was " + generated);
            }
        }

        @Test
        @DisplayName("should return 1 when highest customer is 1")
        void shouldReturnOneWhenHighestIsOne() {
            inquireCustomer = new InquireCustomer(customerDao, SORT_CODE);
            assertEquals(1L,
                    inquireCustomer.generateRandomCustomerNumber(1));
        }

        @Test
        @DisplayName("should return 1 when highest customer is 0")
        void shouldReturnOneWhenHighestIsZero() {
            inquireCustomer = new InquireCustomer(customerDao, SORT_CODE);
            assertEquals(1L,
                    inquireCustomer.generateRandomCustomerNumber(0));
        }
    }

    @Nested
    @DisplayName("Response Factory Methods")
    class ResponseFactoryMethods {

        @Test
        @DisplayName("success response should carry all customer fields")
        void successResponseShouldCarryAllFields() {
            CustomerRecord record = sampleCustomer(12345L);
            InquireCustomerResponse response =
                    InquireCustomerResponse.success(record);

            assertTrue(response.inquirySuccess());
            assertEquals('0', response.failureCode());
            assertEquals("CUST", response.eyecatcher());
            assertEquals(String.valueOf(SORT_CODE), response.sortCode());
            assertEquals(12345L, response.customerNumber());
            assertEquals("Mr John Smith", response.name());
            assertEquals("123 High Street, London, UK", response.address());
            assertEquals(LocalDate.of(1990, 5, 15), response.dateOfBirth());
            assertEquals(750, response.creditScore());
            assertEquals(LocalDate.of(2024, 1, 10),
                    response.creditScoreReviewDate());
        }

        @Test
        @DisplayName("not-found response should preserve customer number")
        void notFoundResponseShouldPreserveCustomerNumber() {
            InquireCustomerResponse response =
                    InquireCustomerResponse.notFound(99999L);

            assertFalse(response.inquirySuccess());
            assertEquals('1', response.failureCode());
            assertEquals(99999L, response.customerNumber());
        }

        @Test
        @DisplayName("storm-drain response should have failure code 2")
        void stormDrainResponseShouldHaveCode2() {
            InquireCustomerResponse response =
                    InquireCustomerResponse.stormDrain();

            assertFalse(response.inquirySuccess());
            assertEquals('2', response.failureCode());
        }

        @Test
        @DisplayName("data-access-error response should have failure code 9")
        void dataAccessErrorResponseShouldHaveCode9() {
            InquireCustomerResponse response =
                    InquireCustomerResponse.dataAccessError();

            assertFalse(response.inquirySuccess());
            assertEquals('9', response.failureCode());
        }
    }

    @Nested
    @DisplayName("CustomerRecord validation")
    class CustomerRecordValidation {

        @Test
        @DisplayName("should create a valid customer record")
        void shouldCreateValidRecord() {
            CustomerRecord record = sampleCustomer(1L);
            assertEquals("CUST", record.eyecatcher());
            assertEquals(SORT_CODE, record.sortCode());
            assertEquals(1L, record.customerNumber());
        }

        @Test
        @DisplayName("should reject null name")
        void shouldRejectNullName() {
            assertThrows(NullPointerException.class, () ->
                    new CustomerRecord("CUST", SORT_CODE, 1L, null,
                            "addr", LocalDate.now(), 100, LocalDate.now()));
        }

        @Test
        @DisplayName("should reject null address")
        void shouldRejectNullAddress() {
            assertThrows(NullPointerException.class, () ->
                    new CustomerRecord("CUST", SORT_CODE, 1L, "name",
                            null, LocalDate.now(), 100, LocalDate.now()));
        }

        @Test
        @DisplayName("should reject null dateOfBirth")
        void shouldRejectNullDateOfBirth() {
            assertThrows(NullPointerException.class, () ->
                    new CustomerRecord("CUST", SORT_CODE, 1L, "name",
                            "addr", null, 100, LocalDate.now()));
        }

        @Test
        @DisplayName("should reject null creditScoreReviewDate")
        void shouldRejectNullReviewDate() {
            assertThrows(NullPointerException.class, () ->
                    new CustomerRecord("CUST", SORT_CODE, 1L, "name",
                            "addr", LocalDate.now(), 100, null));
        }
    }

    @Nested
    @DisplayName("InquireCustomerRequest constants")
    class RequestConstants {

        @Test
        @DisplayName("RANDOM_CUSTOMER should be 0")
        void randomCustomerShouldBeZero() {
            assertEquals(0L, InquireCustomerRequest.RANDOM_CUSTOMER);
        }

        @Test
        @DisplayName("LAST_CUSTOMER should be 9999999999")
        void lastCustomerShouldBe9999999999() {
            assertEquals(9_999_999_999L, InquireCustomerRequest.LAST_CUSTOMER);
        }
    }

    @Nested
    @DisplayName("CustomerDataAccessException")
    class ExceptionTests {

        @Test
        @DisplayName("should carry abend code")
        void shouldCarryAbendCode() {
            CustomerDataAccessException e =
                    new CustomerDataAccessException("fail", "CVR1");
            assertEquals("CVR1", e.getAbendCode());
            assertEquals("fail", e.getMessage());
        }

        @Test
        @DisplayName("should carry cause and abend code")
        void shouldCarryCauseAndAbendCode() {
            RuntimeException cause = new RuntimeException("root");
            CustomerDataAccessException e =
                    new CustomerDataAccessException("fail", "HROL", cause);
            assertEquals("HROL", e.getAbendCode());
            assertSame(cause, e.getCause());
        }
    }

    @Nested
    @DisplayName("Default Sort Code")
    class DefaultSortCode {

        @Test
        @DisplayName("should use 987654 as default sort code from SORTCODE copybook")
        void shouldUseDefaultSortCode() {
            assertEquals(987654, InquireCustomer.DEFAULT_SORT_CODE);
        }

        @Test
        @DisplayName("should use default sort code when constructed with single-arg")
        void shouldUseDefaultSortCodeInConstructor() throws Exception {
            inquireCustomer = new InquireCustomer(customerDao);

            long custNo = 1L;
            CustomerRecord record = new CustomerRecord(
                    "CUST", 987654, custNo, "Test Name", "Test Address",
                    LocalDate.of(2000, 1, 1), 500, LocalDate.of(2024, 6, 1));
            when(customerDao.readCustomer(987654, custNo))
                    .thenReturn(Optional.of(record));

            InquireCustomerResponse response = inquireCustomer.inquireCustomer(
                    new InquireCustomerRequest(custNo));

            assertTrue(response.inquirySuccess());
            verify(customerDao).readCustomer(987654, custNo);
        }
    }
}
