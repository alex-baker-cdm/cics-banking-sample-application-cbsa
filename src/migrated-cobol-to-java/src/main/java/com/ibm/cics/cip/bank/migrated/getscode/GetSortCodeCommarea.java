/*
 *
 *    Copyright IBM Corp. 2023
 *
 *    Migrated from COBOL to Java 21.
 *    Original COBOL copybook: GETSCODE.cpy
 *
 *    COBOL structure:
 *        01 DFHCOMMAREA.
 *            03 GETSORTCODEOperation.
 *                06 SORTCODE pic xXXXXX.
 *
 */

package com.ibm.cics.cip.bank.migrated.getscode;

import java.util.Objects;

public final class GetSortCodeCommarea {

    public static final int SORT_CODE_LENGTH = 6;

    private String sortCode;

    public GetSortCodeCommarea() {
        this.sortCode = " ".repeat(SORT_CODE_LENGTH);
    }

    public GetSortCodeCommarea(String sortCode) {
        setSortCode(sortCode);
    }

    public String getSortCode() {
        return sortCode;
    }

    public void setSortCode(String sortCode) {
        Objects.requireNonNull(sortCode, "sortCode must not be null");
        if (sortCode.length() != SORT_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "sortCode must be exactly " + SORT_CODE_LENGTH
                            + " characters, got " + sortCode.length());
        }
        this.sortCode = sortCode;
    }

    public byte[] toByteBuffer() {
        byte[] buffer = new byte[SORT_CODE_LENGTH];
        byte[] sortCodeBytes = sortCode.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        System.arraycopy(sortCodeBytes, 0, buffer, 0,
                Math.min(sortCodeBytes.length, SORT_CODE_LENGTH));
        return buffer;
    }

    public static GetSortCodeCommarea fromByteBuffer(byte[] buffer) {
        Objects.requireNonNull(buffer, "buffer must not be null");
        if (buffer.length < SORT_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "buffer must be at least " + SORT_CODE_LENGTH
                            + " bytes, got " + buffer.length);
        }
        String sortCode = new String(buffer, 0, SORT_CODE_LENGTH,
                java.nio.charset.StandardCharsets.UTF_8);
        return new GetSortCodeCommarea(sortCode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GetSortCodeCommarea other)) {
            return false;
        }
        return Objects.equals(sortCode, other.sortCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sortCode);
    }

    @Override
    public String toString() {
        return "GetSortCodeCommarea[sortCode=" + sortCode + "]";
    }
}
