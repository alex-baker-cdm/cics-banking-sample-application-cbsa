/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.migrated.getscode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetSortCodeCommareaTest {

    @Test
    void sortCodeLengthConstantIsSix() {
        assertEquals(6, GetSortCodeCommarea.SORT_CODE_LENGTH);
    }

    @Test
    void defaultConstructorInitializesWithSpaces() {
        GetSortCodeCommarea commarea = new GetSortCodeCommarea();

        assertEquals("      ", commarea.getSortCode());
        assertEquals(6, commarea.getSortCode().length());
    }

    @Test
    void parameterizedConstructorSetsValue() {
        GetSortCodeCommarea commarea = new GetSortCodeCommarea("987654");

        assertEquals("987654", commarea.getSortCode());
    }

    @Test
    void setSortCodeUpdatesValue() {
        GetSortCodeCommarea commarea = new GetSortCodeCommarea();

        commarea.setSortCode("123456");

        assertEquals("123456", commarea.getSortCode());
    }

    @Test
    void setSortCodeRejectsNull() {
        GetSortCodeCommarea commarea = new GetSortCodeCommarea();

        assertThrows(NullPointerException.class,
                () -> commarea.setSortCode(null));
    }

    @Test
    void setSortCodeRejectsTooShort() {
        GetSortCodeCommarea commarea = new GetSortCodeCommarea();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> commarea.setSortCode("12345"));
        assertEquals(
                "sortCode must be exactly 6 characters, got 5",
                ex.getMessage());
    }

    @Test
    void setSortCodeRejectsTooLong() {
        GetSortCodeCommarea commarea = new GetSortCodeCommarea();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> commarea.setSortCode("1234567"));
        assertEquals(
                "sortCode must be exactly 6 characters, got 7",
                ex.getMessage());
    }

    @Test
    void setSortCodeRejectsEmptyString() {
        GetSortCodeCommarea commarea = new GetSortCodeCommarea();

        assertThrows(IllegalArgumentException.class,
                () -> commarea.setSortCode(""));
    }

    @Test
    void constructorRejectsTooShort() {
        assertThrows(IllegalArgumentException.class,
                () -> new GetSortCodeCommarea("12345"));
    }

    @Test
    void constructorRejectsTooLong() {
        assertThrows(IllegalArgumentException.class,
                () -> new GetSortCodeCommarea("1234567"));
    }

    @Test
    void toByteBufferProducesCorrectBytes() {
        GetSortCodeCommarea commarea = new GetSortCodeCommarea("987654");

        byte[] buffer = commarea.toByteBuffer();

        assertNotNull(buffer);
        assertEquals(6, buffer.length);
        assertArrayEquals(
                new byte[]{'9', '8', '7', '6', '5', '4'},
                buffer);
    }

    @Test
    void fromByteBufferParsesCorrectly() {
        byte[] buffer = {'9', '8', '7', '6', '5', '4'};

        GetSortCodeCommarea commarea =
                GetSortCodeCommarea.fromByteBuffer(buffer);

        assertEquals("987654", commarea.getSortCode());
    }

    @Test
    void fromByteBufferRejectsNull() {
        assertThrows(NullPointerException.class,
                () -> GetSortCodeCommarea.fromByteBuffer(null));
    }

    @Test
    void fromByteBufferRejectsTooSmall() {
        byte[] buffer = new byte[5];

        assertThrows(IllegalArgumentException.class,
                () -> GetSortCodeCommarea.fromByteBuffer(buffer));
    }

    @Test
    void fromByteBufferHandlesOversizedBuffer() {
        byte[] buffer = {'9', '8', '7', '6', '5', '4', 'X', 'Y'};

        GetSortCodeCommarea commarea =
                GetSortCodeCommarea.fromByteBuffer(buffer);

        assertEquals("987654", commarea.getSortCode());
    }

    @Test
    void roundTripByteBufferConversion() {
        GetSortCodeCommarea original = new GetSortCodeCommarea("987654");

        byte[] buffer = original.toByteBuffer();
        GetSortCodeCommarea restored =
                GetSortCodeCommarea.fromByteBuffer(buffer);

        assertEquals(original, restored);
    }

    @Test
    void equalsReturnsTrueForSameValues() {
        GetSortCodeCommarea a = new GetSortCodeCommarea("987654");
        GetSortCodeCommarea b = new GetSortCodeCommarea("987654");

        assertEquals(a, b);
    }

    @Test
    void equalsReturnsFalseForDifferentValues() {
        GetSortCodeCommarea a = new GetSortCodeCommarea("987654");
        GetSortCodeCommarea b = new GetSortCodeCommarea("123456");

        assertNotEquals(a, b);
    }

    @Test
    void hashCodeConsistentWithEquals() {
        GetSortCodeCommarea a = new GetSortCodeCommarea("987654");
        GetSortCodeCommarea b = new GetSortCodeCommarea("987654");

        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toStringContainsSortCode() {
        GetSortCodeCommarea commarea = new GetSortCodeCommarea("987654");

        String str = commarea.toString();

        assertEquals("GetSortCodeCommarea[sortCode=987654]", str);
    }

    @Test
    void equalsReturnsTrueForSameInstance() {
        GetSortCodeCommarea commarea = new GetSortCodeCommarea("987654");

        assertEquals(commarea, commarea);
    }

    @Test
    void equalsReturnsFalseForNull() {
        GetSortCodeCommarea commarea = new GetSortCodeCommarea("987654");

        assertNotEquals(null, commarea);
    }

    @Test
    void equalsReturnsFalseForDifferentType() {
        GetSortCodeCommarea commarea = new GetSortCodeCommarea("987654");

        assertNotEquals("987654", commarea);
    }

    @Test
    void setSortCodeAcceptsAlphanumericValue() {
        GetSortCodeCommarea commarea = new GetSortCodeCommarea();

        commarea.setSortCode("ABC123");

        assertEquals("ABC123", commarea.getSortCode());
    }

    @Test
    void toByteBufferMatchesCobolCommareaSize() {
        GetSortCodeCommarea commarea = new GetSortCodeCommarea("987654");

        byte[] buffer = commarea.toByteBuffer();

        assertEquals(GetSortCodeCommarea.SORT_CODE_LENGTH, buffer.length);
    }
}
