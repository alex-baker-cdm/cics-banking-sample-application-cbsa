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
 * Tests for the GetCompanyRequest DTO, which represents the COMMAREA
 * structure from the original GETCOMPY COBOL copybook.
 */
class GetCompanyRequestTest {

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("Default constructor should initialize with empty company name")
        void defaultConstructorShouldInitializeEmpty() {
            GetCompanyRequest request = new GetCompanyRequest();
            assertEquals("", request.getCompanyName());
        }

        @Test
        @DisplayName("Parameterized constructor should set company name")
        void parameterizedConstructorShouldSetName() {
            GetCompanyRequest request = new GetCompanyRequest("Test Company");
            assertEquals("Test Company", request.getCompanyName());
        }

        @Test
        @DisplayName("Parameterized constructor with null should default to empty")
        void parameterizedConstructorWithNullShouldDefaultToEmpty() {
            GetCompanyRequest request = new GetCompanyRequest(null);
            assertEquals("", request.getCompanyName());
        }
    }

    @Nested
    @DisplayName("Getter and Setter")
    class GetterSetterTests {

        @Test
        @DisplayName("setCompanyName should update the value")
        void setCompanyNameShouldUpdate() {
            GetCompanyRequest request = new GetCompanyRequest();
            request.setCompanyName("New Name");
            assertEquals("New Name", request.getCompanyName());
        }

        @Test
        @DisplayName("setCompanyName with null should default to empty")
        void setCompanyNameWithNullShouldDefaultToEmpty() {
            GetCompanyRequest request = new GetCompanyRequest("Initial");
            request.setCompanyName(null);
            assertEquals("", request.getCompanyName());
        }

        @Test
        @DisplayName("setCompanyName should handle special characters")
        void setCompanyNameShouldHandleSpecialChars() {
            GetCompanyRequest request = new GetCompanyRequest();
            request.setCompanyName("Bank & Trust Co.");
            assertEquals("Bank & Trust Co.", request.getCompanyName());
        }
    }

    @Nested
    @DisplayName("Fixed Length Output - COBOL PIC X(40) Compatibility")
    class FixedLengthTests {

        @Test
        @DisplayName("Short name should be right-padded with spaces to 40 chars")
        void shortNameShouldBePadded() {
            GetCompanyRequest request = new GetCompanyRequest("Hello");
            String fixed = request.getCompanyNameFixedLength();

            assertEquals(40, fixed.length());
            assertTrue(fixed.startsWith("Hello"));
            assertEquals("Hello" + " ".repeat(35), fixed);
        }

        @Test
        @DisplayName("Exact 40-char name should not be padded")
        void exactLengthShouldNotBePadded() {
            String name = "A".repeat(40);
            GetCompanyRequest request = new GetCompanyRequest(name);
            String fixed = request.getCompanyNameFixedLength();

            assertEquals(40, fixed.length());
            assertEquals(name, fixed);
        }

        @Test
        @DisplayName("Name exceeding 40 chars should be truncated")
        void oversizedNameShouldBeTruncated() {
            String longName = "B".repeat(50);
            GetCompanyRequest request = new GetCompanyRequest(longName);
            String fixed = request.getCompanyNameFixedLength();

            assertEquals(40, fixed.length());
            assertEquals("B".repeat(40), fixed);
        }

        @Test
        @DisplayName("Empty name should produce 40 spaces")
        void emptyNameShouldProduce40Spaces() {
            GetCompanyRequest request = new GetCompanyRequest("");
            String fixed = request.getCompanyNameFixedLength();

            assertEquals(40, fixed.length());
            assertEquals(" ".repeat(40), fixed);
        }

        @Test
        @DisplayName("Default CBSA company name fixed length should match COBOL output")
        void defaultNameFixedLengthShouldMatchCobol() {
            GetCompanyRequest request = new GetCompanyRequest(
                    "CICS Bank Sample Application");
            String fixed = request.getCompanyNameFixedLength();

            assertEquals(40, fixed.length());
            assertEquals("CICS Bank Sample Application            ", fixed);
        }
    }

    @Nested
    @DisplayName("Constants")
    class ConstantsTests {

        @Test
        @DisplayName("COMPANY_NAME_LENGTH should be 40")
        void companyNameLengthShouldBe40() {
            assertEquals(40, GetCompanyRequest.COMPANY_NAME_LENGTH);
        }
    }
}
