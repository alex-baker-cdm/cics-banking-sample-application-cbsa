/*
 * Copyright IBM Corp. 2023
 *
 * In-memory implementation of {@link AbndFileStore} backed by a
 * {@link java.util.concurrent.ConcurrentHashMap}.
 *
 * This serves as a drop-in replacement for the VSAM KSDS file
 * used by the original COBOL ABNDPROC program, suitable for
 * development, testing, and non-mainframe deployments.
 */
package com.ibm.cics.cip.bank.migrated.abndproc;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAbndFileStore implements AbndFileStore {

    private final ConcurrentHashMap<String, AbndRecord> store = new ConcurrentHashMap<>();

    @Override
    public WriteResult write(AbndRecord record) {
        String key = record.vsamKey().toKeyString();
        AbndRecord existing = store.putIfAbsent(key, record);
        if (existing != null) {
            return new WriteResult.Failure(16, 104);
        }
        return new WriteResult.Success();
    }

    public AbndRecord get(AbndVsamKey key) {
        return store.get(key.toKeyString());
    }

    public int size() {
        return store.size();
    }

    public Map<String, AbndRecord> getAll() {
        return Collections.unmodifiableMap(store);
    }

    public void clear() {
        store.clear();
    }
}
