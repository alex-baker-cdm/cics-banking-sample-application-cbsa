/*
 * Copyright IBM Corp. 2023
 *
 * Abstraction for the VSAM CUSTOMER file operations. In the original COBOL,
 * records were written to an indexed VSAM file. This interface allows
 * different backing implementations (file-based, in-memory, database).
 */
package com.ibm.cics.cip.bank.bankdata;

import java.io.IOException;

public interface CustomerDataStore extends AutoCloseable {

    void open() throws IOException;

    void writeCustomer(CustomerRecord record) throws IOException;

    void writeControlRecord(CustomerControlRecord record) throws IOException;

    @Override
    void close() throws IOException;
}
