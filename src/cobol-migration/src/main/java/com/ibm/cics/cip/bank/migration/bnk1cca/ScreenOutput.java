/*
 * Copyright IBM Corp. 2023
 *
 * Represents the output data to be rendered on the BNK1ACC BMS map.
 * Maps to the BNK1ACCO output fields in BNK1CCA.cbl.
 */
package com.ibm.cics.cip.bank.migration.bnk1cca;

import java.util.List;

/**
 * The output payload for the BNK1ACC screen, containing the message line
 * and up to 10 formatted account display lines.
 *
 * @param message       the status/error message shown on line 23 (MESSAGEO)
 * @param accountLines  formatted account strings shown on lines 9-18 (ACCOUNTO array)
 * @param screenAction  what the presentation layer should do with this output
 */
public record ScreenOutput(
        String message,
        List<String> accountLines,
        ScreenAction screenAction
) {

    public static final int MAX_DISPLAY_ACCOUNTS = 10;
    public static final String END_OF_SESSION_MESSAGE = "Session Ended";
}
