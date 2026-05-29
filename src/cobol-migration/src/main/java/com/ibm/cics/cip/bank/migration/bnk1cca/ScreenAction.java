/*
 * Copyright IBM Corp. 2023
 *
 * Represents the action the BMS screen should take after processing input.
 * Maps to the SEND-FLAG and RETURN behavior in BNK1CCA.cbl.
 */
package com.ibm.cics.cip.bank.migration.bnk1cca;

/**
 * The action the presentation layer should perform after the service processes
 * a user interaction.
 */
public enum ScreenAction {

    /** Send the map with all fields erased (first display). */
    SEND_ERASE,

    /** Resend only the data portion of the map. */
    SEND_DATAONLY,

    /** Resend data with an audible alarm (validation error or results). */
    SEND_DATAONLY_ALARM,

    /** Return to the main menu (PF3 pressed). */
    RETURN_TO_MENU,

    /** Display the end-of-session message and return (PF12/AID). */
    TERMINATE_SESSION,

    /** Erase the screen and return (CLEAR pressed). */
    CLEAR_SCREEN,

    /** No action required (PA key pressed). */
    NO_ACTION
}
