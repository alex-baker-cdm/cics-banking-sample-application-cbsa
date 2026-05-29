/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.getcompany;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the migrated GETCOMPY COBOL program.
 *
 * <p>These tests verify that the Java 21 migration preserves all
 * behavior of the original COBOL program:
 * <ul>
 *   <li>Returns the correct company name</li>
 *   <li>Populates the COMMAREA equivalent correctly</li>
 *   <li>Maintains field length constraints (PIC X(40))</li>
 *   <li>Handles edge cases appropriately</li>
 * </ul>
 */
class GetCompanyServiceTest {

    @Nested
    @DisplayName("Default Construction - Matching Original COBOL Behavior")
    class DefaultBehaviorTests {

        @Test
        @DisplayName("Should return 'CICS Bank Sample Application' as the company name")
        void shouldReturnDefaultCompanyName() {
            GetCompanyService service = new GetCompanyService();
            GetCompanyRequest request = service.getCompanyInfo();

            assertEquals("CICS Bank Sample Application", request.getCompanyName());
        }

        @Test
        @DisplayName("Should populate an existing request with the company name")
        void shouldPopulateExistingRequest() {
            GetCompanyService service = new GetCompanyService();
            GetCompanyRequest request = new GetCompanyRequest();

            GetCompanyRequest result = service.getCompanyInfo(request);

            assertSame(request, result);
            assertEquals("CICS Bank Sample Application", result.getCompanyName());
        }

        @Test
        @DisplayName("Should overwrite any existing value in the request")
        void shouldOverwriteExistingValue() {
            GetCompanyService service = new GetCompanyService();
            GetCompanyRequest request = new GetCompanyRequest("Previous Value");

            service.getCompanyInfo(request);

            assertEquals("CICS Bank Sample Application", request.getCompanyName());
        }

        @Test
        @DisplayName("Default company name should match the COBOL literal exactly")
        void defaultCompanyNameShouldMatchCobolLiteral() {
            assertEquals("CICS Bank Sample Application",
                    GetCompanyService.DEFAULT_COMPANY_NAME);
        }

        @Test
        @DisplayName("Default company name should not exceed 40 characters")
        void defaultCompanyNameShouldFitInField() {
            assertTrue(GetCompanyService.DEFAULT_COMPANY_NAME.length()
                    <= GetCompanyRequest.COMPANY_NAME_LENGTH);
        }

        @Test
        @DisplayName("getCompanyName() should return the configured name")
        void getCompanyNameShouldReturnConfiguredName() {
            GetCompanyService service = new GetCompanyService();
            assertEquals("CICS Bank Sample Application", service.getCompanyName());
        }
    }

    @Nested
    @DisplayName("Custom Company Name Construction")
    class CustomCompanyNameTests {

        @Test
        @DisplayName("Should accept a custom company name")
        void shouldAcceptCustomCompanyName() {
            GetCompanyService service = new GetCompanyService("My Bank Corp");
            GetCompanyRequest request = service.getCompanyInfo();

            assertEquals("My Bank Corp", request.getCompanyName());
        }

        @Test
        @DisplayName("Should accept an empty company name")
        void shouldAcceptEmptyCompanyName() {
            GetCompanyService service = new GetCompanyService("");
            GetCompanyRequest request = service.getCompanyInfo();

            assertEquals("", request.getCompanyName());
        }

        @Test
        @DisplayName("Should accept a company name at maximum length (40 chars)")
        void shouldAcceptMaxLengthCompanyName() {
            String maxName = "A".repeat(40);
            GetCompanyService service = new GetCompanyService(maxName);
            GetCompanyRequest request = service.getCompanyInfo();

            assertEquals(maxName, request.getCompanyName());
        }

        @Test
        @DisplayName("Should reject null company name")
        void shouldRejectNullCompanyName() {
            assertThrows(IllegalArgumentException.class,
                    () -> new GetCompanyService(null));
        }

        @Test
        @DisplayName("Should reject company name exceeding 40 characters")
        void shouldRejectOversizedCompanyName() {
            String oversized = "A".repeat(41);
            assertThrows(IllegalArgumentException.class,
                    () -> new GetCompanyService(oversized));
        }
    }

    @Nested
    @DisplayName("Request Validation")
    class RequestValidationTests {

        @Test
        @DisplayName("Should reject null request")
        void shouldRejectNullRequest() {
            GetCompanyService service = new GetCompanyService();

            assertThrows(IllegalArgumentException.class,
                    () -> service.getCompanyInfo(null));
        }
    }

    @Nested
    @DisplayName("Thread Safety")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Service should be safely usable from multiple threads")
        void shouldBeThreadSafe() throws InterruptedException {
            GetCompanyService service = new GetCompanyService();
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];
            String[] results = new String[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = Thread.ofVirtual().start(() -> {
                    GetCompanyRequest request = service.getCompanyInfo();
                    results[index] = request.getCompanyName();
                });
            }

            for (Thread thread : threads) {
                thread.join();
            }

            for (String result : results) {
                assertEquals("CICS Bank Sample Application", result);
            }
        }
    }

    @Nested
    @DisplayName("COBOL Field Length Compatibility")
    class FieldLengthCompatibilityTests {

        @Test
        @DisplayName("COMPANY_NAME_LENGTH should be 40 to match PIC X(40)")
        void fieldLengthShouldBe40() {
            assertEquals(40, GetCompanyRequest.COMPANY_NAME_LENGTH);
        }

        @Test
        @DisplayName("Fixed-length output should be exactly 40 characters")
        void fixedLengthOutputShouldBe40Chars() {
            GetCompanyService service = new GetCompanyService();
            GetCompanyRequest request = service.getCompanyInfo();

            String fixedLength = request.getCompanyNameFixedLength();
            assertEquals(40, fixedLength.length());
        }

        @Test
        @DisplayName("Fixed-length output should be space-padded on the right")
        void fixedLengthOutputShouldBeSpacePadded() {
            GetCompanyService service = new GetCompanyService();
            GetCompanyRequest request = service.getCompanyInfo();

            String fixedLength = request.getCompanyNameFixedLength();
            assertEquals("CICS Bank Sample Application", fixedLength.trim());
            assertTrue(fixedLength.endsWith(" "));
        }

        @Test
        @DisplayName("Fixed-length output for max-length name should have no padding")
        void fixedLengthForMaxNameShouldHaveNoPadding() {
            String maxName = "X".repeat(40);
            GetCompanyService service = new GetCompanyService(maxName);
            GetCompanyRequest request = service.getCompanyInfo();

            String fixedLength = request.getCompanyNameFixedLength();
            assertEquals(maxName, fixedLength);
        }

        @Test
        @DisplayName("Fixed-length output for empty name should be all spaces")
        void fixedLengthForEmptyNameShouldBeAllSpaces() {
            GetCompanyService service = new GetCompanyService("");
            GetCompanyRequest request = service.getCompanyInfo();

            String fixedLength = request.getCompanyNameFixedLength();
            assertEquals(40, fixedLength.length());
            assertEquals(" ".repeat(40), fixedLength);
        }
    }
}
