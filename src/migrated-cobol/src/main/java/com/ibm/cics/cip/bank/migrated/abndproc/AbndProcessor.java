/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL program ABNDPROC.cbl (src/base/cobol_src/ABNDPROC.cbl).
 *
 * This program processes application abends and writes them to a centralised
 * data store so that they can be viewed from one place, without having to
 * go hunting for them.
 *
 * Original COBOL flow:
 *   1. Receive DFHCOMMAREA containing abend details
 *   2. Copy COMMAREA into WS-ABND-AREA (ABNDINFO record)
 *   3. EXEC CICS WRITE FILE('ABNDFILE') using ABND-VSAM-KEY as RIDFLD
 *   4. If write fails  -> display error, EXEC CICS RETURN
 *   5. If write succeeds -> EXEC CICS RETURN
 *
 * Java equivalent:
 *   - DFHCOMMAREA / WS-ABND-AREA  -> {@link AbndRecord}
 *   - EXEC CICS WRITE FILE(...)   -> {@link AbndFileStore#write(AbndRecord)}
 *   - EXEC CICS RETURN            -> method returns boolean
 */
package com.ibm.cics.cip.bank.migrated.abndproc;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AbndProcessor {

    private static final Logger logger = Logger.getLogger(AbndProcessor.class.getName());

    private final AbndFileStore abndFileStore;

    public AbndProcessor(AbndFileStore abndFileStore) {
        this.abndFileStore = Objects.requireNonNull(abndFileStore, "abndFileStore must not be null");
    }

    /**
     * Processes an abend record by persisting it to the abend file store.
     * <p>
     * Mirrors the COBOL PREMIERE SECTION (paragraphs A010 through A999):
     * <ol>
     *   <li>Receives the abend information (equivalent to DFHCOMMAREA)</li>
     *   <li>Writes the record to the file store (equivalent to EXEC CICS WRITE FILE('ABNDFILE'))</li>
     *   <li>Returns success/failure status (equivalent to EXEC CICS RETURN)</li>
     * </ol>
     *
     * @param record the abend record to process; must not be {@code null}
     * @return {@code true} if the record was written successfully,
     *         {@code false} if the write failed
     */
    public boolean processAbend(AbndRecord record) {
        Objects.requireNonNull(record, "record must not be null");

        logger.log(Level.FINE, () -> "Started AbndProcessor");
        logger.log(Level.FINE, () -> "AbndRecord passed=" + record);

        WriteResult result = abndFileStore.write(record);

        return switch (result) {
            case WriteResult.Failure failure -> {
                logger.severe("*********************************************");
                logger.severe("**** Unable to write to the file ABNDFILE !!!");
                logger.severe("RESP=" + failure.respCode() + " RESP2=" + failure.resp2Code());
                logger.severe("*********************************************");
                yield false;
            }
            case WriteResult.Success ignored -> {
                logger.log(Level.FINE, () -> "ABEND record successfully written to ABNDFILE");
                logger.log(Level.FINE, () -> "AbndRecord=" + record);
                yield true;
            }
        };
    }
}
