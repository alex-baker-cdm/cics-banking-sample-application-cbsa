/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.migrated.createcustomer;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Valid customer titles, migrated from BNK1CCS.cbl EDIT-DATA title validation.
 *
 * <p>The COBOL program accepted titles in various casings (upper, lower, mixed)
 * with trailing underscores or spaces. This enum centralises the canonical
 * title values and provides case-insensitive lookup.
 */
public enum CustomerTitle {

    MR("Mr"),
    MRS("Mrs"),
    MISS("Miss"),
    MS("Ms"),
    DR("Dr"),
    DRS("Drs"),
    PROFESSOR("Professor"),
    LORD("Lord"),
    SIR("Sir"),
    LADY("Lady");

    private final String displayName;

    private static final Map<String, CustomerTitle> LOOKUP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(
                            t -> t.displayName.toUpperCase(),
                            t -> t));

    CustomerTitle(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * Resolve a raw title string to a {@link CustomerTitle}, stripping
     * underscores and whitespace and performing a case-insensitive match.
     *
     * @param raw the input title (may contain underscores/spaces/mixed case)
     * @return the matching title, or {@code null} if no match
     */
    public static CustomerTitle fromInput(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replace('_', ' ').strip().toUpperCase();
        return LOOKUP.get(cleaned);
    }

    /**
     * Returns a comma-separated list of all valid display names.
     */
    public static String validTitlesList() {
        return Arrays.stream(values())
                .map(CustomerTitle::displayName)
                .collect(Collectors.joining(","));
    }
}
