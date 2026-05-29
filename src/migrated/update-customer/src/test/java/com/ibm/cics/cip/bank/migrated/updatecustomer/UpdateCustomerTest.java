/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.updatecustomer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCustomerTest {

    private InMemoryCustomerDataStore dataStore;
    private UpdateCustomer updateCustomer;

    private static final String SORT_CODE = UpdateCustomer.DEFAULT_SORT_CODE;
    private static final String CUST_NUM = "0000000001";

    @BeforeEach
    void setUp() {
        dataStore = new InMemoryCustomerDataStore();
        updateCustomer = new UpdateCustomer(dataStore);
    }

    private CustomerRecord seedCustomer() {
        CustomerRecord record = new CustomerRecord(
                "CUST", SORT_CODE, CUST_NUM,
                "Mr John Smith",
                "123 Main Street, London",
                "15051990", 750, "01012024"
        );
        dataStore.put(SORT_CODE, CUST_NUM, record);
        return record;
    }

    private UpdateCustomerRequest makeRequest(String name, String address) {
        return new UpdateCustomerRequest(
                "CUST", SORT_CODE, CUST_NUM,
                name, address,
                "15051990", 750, "01012024"
        );
    }

    // ---------------------------------------------------------------
    // Title validation tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Title validation")
    class TitleValidation {

        @ParameterizedTest(name = "Valid title: \"{0}\"")
        @ValueSource(strings = {
                "Professor Albert Einstein",
                "Mr John Smith",
                "Mrs Jane Doe",
                "Miss Sarah Connor",
                "Ms Robin Banks",
                "Dr Who",
                "Drs Van Der Berg",
                "Lord Byron",
                "Sir Lancelot",
                "Lady Gaga"
        })
        void shouldAcceptValidTitles(String name) {
            assertTrue(updateCustomer.isTitleValid(name));
        }

        @Test
        @DisplayName("Empty/blank name is a valid title")
        void shouldAcceptEmptyName() {
            assertTrue(updateCustomer.isTitleValid(""));
            assertTrue(updateCustomer.isTitleValid("         "));
            assertTrue(updateCustomer.isTitleValid(null));
        }

        @ParameterizedTest(name = "Invalid title: \"{0}\"")
        @ValueSource(strings = {
                "King Arthur",
                "Queen Elizabeth",
                "Rev Martin Luther",
                "Mx Taylor Swift",
                "Captain Kirk"
        })
        void shouldRejectInvalidTitles(String name) {
            assertFalse(updateCustomer.isTitleValid(name));
        }

        @Test
        @DisplayName("Invalid title returns failure code 'T'")
        void shouldReturnFailCodeTForInvalidTitle() {
            seedCustomer();
            UpdateCustomerRequest request = makeRequest(
                    "King Arthur", "Camelot Castle");

            UpdateCustomerResult result = updateCustomer.execute(request);

            assertInstanceOf(UpdateCustomerResult.Failure.class, result);
            assertFalse(result.isSuccess());
            assertEquals(UpdateCustomer.FAIL_INVALID_TITLE,
                    ((UpdateCustomerResult.Failure) result).failureCode());
        }
    }

    // ---------------------------------------------------------------
    // Extract title tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Extract title from name")
    class ExtractTitle {

        @Test
        void shouldExtractFirstWord() {
            assertEquals("Mr", UpdateCustomer.extractTitle("Mr Smith"));
        }

        @Test
        void shouldReturnWholeStringIfNoSpace() {
            assertEquals("Professor",
                    UpdateCustomer.extractTitle("Professor"));
        }

        @Test
        void shouldReturnEmptyForEmptyInput() {
            assertEquals("", UpdateCustomer.extractTitle(""));
            assertEquals("", UpdateCustomer.extractTitle(null));
        }
    }

    // ---------------------------------------------------------------
    // Customer not found
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Customer not found")
    class CustomerNotFound {

        @Test
        @DisplayName("Returns failure code '1' when customer does not exist")
        void shouldReturnNotFoundFailCode() {
            UpdateCustomerRequest request = makeRequest(
                    "Mr Updated Name", "New Address");

            UpdateCustomerResult result = updateCustomer.execute(request);

            assertInstanceOf(UpdateCustomerResult.Failure.class, result);
            assertEquals(UpdateCustomer.FAIL_NOT_FOUND,
                    ((UpdateCustomerResult.Failure) result).failureCode());
        }
    }

    // ---------------------------------------------------------------
    // Data store error on read
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Data store read error")
    class ReadError {

        @Test
        @DisplayName("Returns failure code '2' on data store read exception")
        void shouldReturnReadErrorFailCode() {
            dataStore.setFailOnRead(true);
            UpdateCustomerRequest request = makeRequest(
                    "Mr Updated Name", "New Address");

            UpdateCustomerResult result = updateCustomer.execute(request);

            assertInstanceOf(UpdateCustomerResult.Failure.class, result);
            assertEquals(UpdateCustomer.FAIL_READ_ERROR,
                    ((UpdateCustomerResult.Failure) result).failureCode());
        }
    }

    // ---------------------------------------------------------------
    // Data store error on rewrite
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Data store write error")
    class WriteError {

        @Test
        @DisplayName("Returns failure code '3' on data store rewrite exception")
        void shouldReturnWriteErrorFailCode() {
            seedCustomer();
            dataStore.setFailOnWrite(true);
            UpdateCustomerRequest request = makeRequest(
                    "Mr Updated Name", "New Address");

            UpdateCustomerResult result = updateCustomer.execute(request);

            assertInstanceOf(UpdateCustomerResult.Failure.class, result);
            assertEquals(UpdateCustomer.FAIL_WRITE_ERROR,
                    ((UpdateCustomerResult.Failure) result).failureCode());
        }
    }

    // ---------------------------------------------------------------
    // Empty name and address validation (fail code '4')
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Both name and address empty/blank")
    class EmptyNameAndAddress {

        @Test
        @DisplayName("Both null")
        void shouldRejectBothNull() {
            seedCustomer();
            UpdateCustomerRequest request = new UpdateCustomerRequest(
                    "CUST", SORT_CODE, CUST_NUM,
                    null, null,
                    "15051990", 750, "01012024"
            );

            UpdateCustomerResult result = updateCustomer.execute(request);

            assertInstanceOf(UpdateCustomerResult.Failure.class, result);
            assertEquals(UpdateCustomer.FAIL_EMPTY_NAME_AND_ADDRESS,
                    ((UpdateCustomerResult.Failure) result).failureCode());
        }

        @Test
        @DisplayName("Both empty strings")
        void shouldRejectBothEmpty() {
            seedCustomer();
            UpdateCustomerResult result =
                    updateCustomer.execute(makeRequest("", ""));

            assertInstanceOf(UpdateCustomerResult.Failure.class, result);
            assertEquals(UpdateCustomer.FAIL_EMPTY_NAME_AND_ADDRESS,
                    ((UpdateCustomerResult.Failure) result).failureCode());
        }

        @Test
        @DisplayName("Both start with space")
        void shouldRejectBothLeadingSpace() {
            seedCustomer();
            UpdateCustomerResult result =
                    updateCustomer.execute(makeRequest(" name", " addr"));

            assertInstanceOf(UpdateCustomerResult.Failure.class, result);
            assertEquals(UpdateCustomer.FAIL_EMPTY_NAME_AND_ADDRESS,
                    ((UpdateCustomerResult.Failure) result).failureCode());
        }

        @Test
        @DisplayName("All-spaces strings")
        void shouldRejectAllSpaces() {
            seedCustomer();
            UpdateCustomerResult result =
                    updateCustomer.execute(makeRequest("   ", "   "));

            assertInstanceOf(UpdateCustomerResult.Failure.class, result);
            assertEquals(UpdateCustomer.FAIL_EMPTY_NAME_AND_ADDRESS,
                    ((UpdateCustomerResult.Failure) result).failureCode());
        }
    }

    // ---------------------------------------------------------------
    // Partial updates (name only, address only)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Partial field updates")
    class PartialUpdates {

        @Test
        @DisplayName("Only address is updated when name is empty")
        void shouldUpdateOnlyAddressWhenNameEmpty() {
            CustomerRecord original = seedCustomer();
            String originalName = original.getCustomerName();

            UpdateCustomerResult result = updateCustomer.execute(
                    makeRequest("", "456 New Avenue, Manchester"));

            assertInstanceOf(UpdateCustomerResult.Success.class, result);
            UpdateCustomerResult.Success success =
                    (UpdateCustomerResult.Success) result;
            assertEquals(originalName, success.customerName());
            assertEquals("456 New Avenue, Manchester",
                    success.customerAddress());
        }

        @Test
        @DisplayName("Only address is updated when name starts with space")
        void shouldUpdateOnlyAddressWhenNameLeadingSpace() {
            CustomerRecord original = seedCustomer();
            String originalName = original.getCustomerName();

            UpdateCustomerResult result = updateCustomer.execute(
                    makeRequest(" some name", "456 New Avenue"));

            assertInstanceOf(UpdateCustomerResult.Success.class, result);
            UpdateCustomerResult.Success success =
                    (UpdateCustomerResult.Success) result;
            assertEquals(originalName, success.customerName());
            assertEquals("456 New Avenue", success.customerAddress());
        }

        @Test
        @DisplayName("Only name is updated when address is empty")
        void shouldUpdateOnlyNameWhenAddressEmpty() {
            CustomerRecord original = seedCustomer();
            String originalAddr = original.getCustomerAddress();

            UpdateCustomerResult result = updateCustomer.execute(
                    makeRequest("Mrs Jane Doe", ""));

            assertInstanceOf(UpdateCustomerResult.Success.class, result);
            UpdateCustomerResult.Success success =
                    (UpdateCustomerResult.Success) result;
            assertEquals("Mrs Jane Doe", success.customerName());
            assertEquals(originalAddr, success.customerAddress());
        }

        @Test
        @DisplayName("Only name is updated when address starts with space")
        void shouldUpdateOnlyNameWhenAddressLeadingSpace() {
            CustomerRecord original = seedCustomer();
            String originalAddr = original.getCustomerAddress();

            UpdateCustomerResult result = updateCustomer.execute(
                    makeRequest("Dr Watson", " 221B Baker Street"));

            assertInstanceOf(UpdateCustomerResult.Success.class, result);
            UpdateCustomerResult.Success success =
                    (UpdateCustomerResult.Success) result;
            assertEquals("Dr Watson", success.customerName());
            assertEquals(originalAddr, success.customerAddress());
        }
    }

    // ---------------------------------------------------------------
    // Full update (both name and address)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Full update (name and address)")
    class FullUpdate {

        @Test
        @DisplayName("Both name and address are updated")
        void shouldUpdateBothNameAndAddress() {
            seedCustomer();

            UpdateCustomerResult result = updateCustomer.execute(
                    makeRequest("Mrs Jane Doe", "789 Oak Lane, Bristol"));

            assertInstanceOf(UpdateCustomerResult.Success.class, result);
            UpdateCustomerResult.Success success =
                    (UpdateCustomerResult.Success) result;
            assertEquals("Mrs Jane Doe", success.customerName());
            assertEquals("789 Oak Lane, Bristol",
                    success.customerAddress());
        }

        @Test
        @DisplayName("Success result contains all record fields")
        void shouldReturnCompleteRecordOnSuccess() {
            seedCustomer();

            UpdateCustomerResult result = updateCustomer.execute(
                    makeRequest("Sir Galahad", "Round Table, Camelot"));

            assertInstanceOf(UpdateCustomerResult.Success.class, result);
            UpdateCustomerResult.Success success =
                    (UpdateCustomerResult.Success) result;

            assertTrue(result.isSuccess());
            assertEquals("CUST", success.eyecatcher());
            assertEquals(SORT_CODE, success.sortCode());
            assertEquals(CUST_NUM, success.customerNumber());
            assertEquals("Sir Galahad", success.customerName());
            assertEquals("Round Table, Camelot",
                    success.customerAddress());
            assertEquals("15051990", success.dateOfBirth());
            assertEquals(750, success.creditScore());
            assertEquals("01012024", success.creditScoreReviewDate());
        }

        @Test
        @DisplayName("Data store record is persisted after update")
        void shouldPersistRecordInDataStore() {
            seedCustomer();

            updateCustomer.execute(
                    makeRequest("Lady Diana", "Kensington Palace, London"));

            CustomerRecord stored = dataStore.getLastRewritten();
            assertEquals("Lady Diana", stored.getCustomerName());
            assertEquals("Kensington Palace, London",
                    stored.getCustomerAddress());
        }
    }

    // ---------------------------------------------------------------
    // Default sort code
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Default sort code")
    class SortCode {

        @Test
        @DisplayName("Uses the default sort code from SORTCODE copybook")
        void shouldUseDefaultSortCode() {
            assertEquals("987654", UpdateCustomer.DEFAULT_SORT_CODE);
        }
    }

    // ---------------------------------------------------------------
    // isFieldEmptyOrLeadingSpace utility
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("isFieldEmptyOrLeadingSpace")
    class FieldCheck {

        @Test
        void shouldReturnTrueForNull() {
            assertTrue(UpdateCustomer.isFieldEmptyOrLeadingSpace(null));
        }

        @Test
        void shouldReturnTrueForEmpty() {
            assertTrue(UpdateCustomer.isFieldEmptyOrLeadingSpace(""));
        }

        @Test
        void shouldReturnTrueForBlank() {
            assertTrue(UpdateCustomer.isFieldEmptyOrLeadingSpace("   "));
        }

        @Test
        void shouldReturnTrueForLeadingSpace() {
            assertTrue(UpdateCustomer.isFieldEmptyOrLeadingSpace(" hello"));
        }

        @Test
        void shouldReturnFalseForNonEmptyNonLeadingSpace() {
            assertFalse(UpdateCustomer.isFieldEmptyOrLeadingSpace("hello"));
        }

        @Test
        void shouldReturnFalseForTrailingSpaceOnly() {
            assertFalse(
                    UpdateCustomer.isFieldEmptyOrLeadingSpace("hello   "));
        }
    }

    // ---------------------------------------------------------------
    // In-memory test double for CustomerDataStore
    // ---------------------------------------------------------------

    static class InMemoryCustomerDataStore implements CustomerDataStore {

        private final Map<String, CustomerRecord> store = new HashMap<>();
        private boolean failOnRead;
        private boolean failOnWrite;
        private CustomerRecord lastRewritten;

        void put(String sortCode, String customerNumber,
                 CustomerRecord record) {
            store.put(key(sortCode, customerNumber), record);
        }

        CustomerRecord getLastRewritten() {
            return lastRewritten;
        }

        void setFailOnRead(boolean fail) {
            this.failOnRead = fail;
        }

        void setFailOnWrite(boolean fail) {
            this.failOnWrite = fail;
        }

        @Override
        public Optional<CustomerRecord> readForUpdate(
                String sortCode, String customerNumber)
                throws DataStoreException {
            if (failOnRead) {
                throw new DataStoreException("Simulated read failure");
            }
            return Optional.ofNullable(store.get(key(sortCode, customerNumber)));
        }

        @Override
        public void rewrite(CustomerRecord record)
                throws DataStoreException {
            if (failOnWrite) {
                throw new DataStoreException("Simulated write failure");
            }
            lastRewritten = record;
            store.put(key(record.getSortCode(), record.getCustomerNumber()),
                    record);
        }

        private static String key(String sortCode, String customerNumber) {
            return sortCode + "-" + customerNumber;
        }
    }
}
