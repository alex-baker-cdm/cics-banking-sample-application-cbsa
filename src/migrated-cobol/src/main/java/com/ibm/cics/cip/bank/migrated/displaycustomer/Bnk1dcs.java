package com.ibm.cics.cip.bank.migrated.displaycustomer;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Display Customer program for the CICS Banking Sample Application.
 * Displays customer details, supports deleting a customer (PF5),
 * and updating a customer record (PF10).
 *
 * Migrated from: BNK1DCS.cbl (COBOL CICS BMS program)
 * Original author: Jon Collett
 *
 * The COBOL program was a pseudo-conversational CICS transaction (ODCS)
 * that used BMS maps for 3270 terminal I/O and EXEC CICS LINK calls
 * to sub-programs INQCUST, DELCUS, and UPDCUST. This Java migration
 * preserves all business logic and validation while abstracting
 * the CICS-specific I/O into injectable dependencies.
 */
public class Bnk1dcs {

    private static final Logger logger = Logger.getLogger(Bnk1dcs.class.getName());

    private static final String PROGRAM_NAME = "BNK1DCS";

    private static final Set<String> VALID_TITLES = Set.of(
            "Professor", "Mr", "Mrs", "Miss", "Ms", "Dr", "Drs",
            "Lord", "Sir", "Lady"
    );

    private final CustomerService customerService;

    private boolean validData;

    private DisplayCustomerScreenData screenData;
    private InqCustCommarea inqCustCommarea;

    public Bnk1dcs(CustomerService customerService) {
        this.customerService = customerService;
        this.screenData = new DisplayCustomerScreenData();
        this.inqCustCommarea = new InqCustCommarea();
    }

    /**
     * Main entry point — replaces the PREMIERE SECTION of the COBOL program.
     *
     * @param aidKey    the attention identifier key pressed by the user
     * @param commarea  the communication area from the previous interaction (null on first call)
     * @param input     the screen data entered by the user
     * @return the result containing screen data, commarea, and action to take
     */
    public DisplayCustomerResult processTransaction(
            AidKey aidKey,
            DisplayCustomerCommarea commarea,
            DisplayCustomerScreenData input) {

        this.screenData = (input != null) ? input : new DisplayCustomerScreenData();
        this.validData = true;

        boolean firstTime = (commarea == null);

        if (firstTime) {
            commarea = new DisplayCustomerCommarea();
            return handleFirstTime(commarea);
        }

        DisplayCustomerResult result;

        switch (aidKey) {
            case PA1, PA2, PA3 -> {
                result = buildMapResult(commarea, false, false);
            }

            case PF3 -> {
                result = new DisplayCustomerResult(DisplayCustomerResult.Action.RETURN_TO_MENU);
                result.setCommarea(commarea);
                return result;
            }

            case PF5 -> {
                processMap(aidKey, commarea, input);
                result = buildMapResult(commarea, false, true);
            }

            case PF10 -> {
                processMap(aidKey, commarea, input);
                result = buildMapResult(commarea, false, true);
            }

            case PF12 -> {
                result = new DisplayCustomerResult(DisplayCustomerResult.Action.SESSION_ENDED);
                result.setCommarea(commarea);
                return result;
            }

            case CLEAR -> {
                result = new DisplayCustomerResult(DisplayCustomerResult.Action.CLEAR_SCREEN);
                result.setCommarea(commarea);
                return result;
            }

            case ENTER -> {
                processMap(aidKey, commarea, input);
                result = buildMapResult(commarea, false, true);
            }

            default -> {
                screenData.setMessage("Invalid key pressed.");
                result = buildMapResult(commarea, false, true);
            }
        }

        copyScreenToCommarea(commarea);

        return result;
    }

    private DisplayCustomerResult handleFirstTime(DisplayCustomerCommarea commarea) {
        screenData.clear();
        commarea.initialize();

        DisplayCustomerResult result = buildMapResult(commarea, true, false);
        return result;
    }

    private DisplayCustomerResult buildMapResult(
            DisplayCustomerCommarea commarea,
            boolean erase,
            boolean alarm) {

        DisplayCustomerResult result = new DisplayCustomerResult(
                DisplayCustomerResult.Action.SEND_MAP);
        result.setScreenData(screenData);
        result.setCommarea(commarea);
        result.setEraseScreen(erase);
        result.setAlarm(alarm);
        return result;
    }

    /**
     * PROCESS-MAP SECTION — routes to appropriate handler based on key + state.
     */
    void processMap(AidKey aidKey, DisplayCustomerCommarea commarea,
                    DisplayCustomerScreenData input) {

        if (input != null) {
            this.screenData = input;
        }

        // Equivalent to RECEIVE-MAP: MOVE SORTCI TO INQCUST-SCODE
        if (screenData.getSortCode() != null) {
            inqCustCommarea.setSortCode(screenData.getSortCode());
        }

        if (aidKey == AidKey.ENTER && !"Y".equals(commarea.getUpdateFlag())) {
            editData();
            if (validData) {
                getCustomerData();
            } else {
                inqCustCommarea = new InqCustCommarea();
            }
        }

        if (aidKey == AidKey.ENTER && "Y".equals(commarea.getUpdateFlag())) {
            editData2();
            if (validData) {
                updateCustomerData(commarea);
            }
        }

        if (aidKey == AidKey.PF5) {
            editData();
            validateData();
            if (validData) {
                deleteCustomerData();
            }
        }

        if (aidKey == AidKey.PF10) {
            editData();
            validateData();
            if (validData) {
                unprotectCustomerData(commarea);
                screenData.setMessage(
                        "Amend data then press <ENTER>.");
            }
        }
    }

    /**
     * EDIT-DATA SECTION — validates the customer number input.
     * Corresponds to ED010 in BNK1DCS.cbl
     */
    void editData() {
        String custNo = screenData.getCustomerNumberInput();

        if (custNo == null || custNo.isBlank()) {
            screenData.setMessage("Please enter a customer number.");
            validData = false;
            return;
        }

        String deEdited = deEditField(custNo);

        if (!isNumeric(deEdited)) {
            screenData.setMessage("Please enter a customer number.");
            validData = false;
            return;
        }

        screenData.setCustomerNumberInput(deEdited);
        validData = true;
    }

    /**
     * EDIT-DATA2 SECTION — validates update fields (name title, address).
     * Corresponds to ED2010 in BNK1DCS.cbl
     */
    void editData2() {
        String customerName = screenData.getCustomerName();

        if (customerName != null && !customerName.isBlank()) {
            String title = extractTitle(customerName);
            if (!VALID_TITLES.contains(title)) {
                screenData.setMessage(
                        "Valid titles are: Mr,Mrs,Miss,Ms,Dr,Professor,"
                                + "Drs,Lord,Sir,Lady");
                validData = false;
                return;
            }
        } else {
            screenData.setMessage(
                    "Valid titles are: Mr,Mrs,Miss,Ms,Dr,Professor,"
                            + "Drs,Lord,Sir,Lady");
            validData = false;
            return;
        }

        String addr1 = screenData.getCustomerAddress1();
        String addr2 = screenData.getCustomerAddress2();
        String addr3 = screenData.getCustomerAddress3();

        if (isBlankOrEmpty(addr1) && isBlankOrEmpty(addr2) && isBlankOrEmpty(addr3)) {
            screenData.setMessage(
                    "Address must not be all spaces - please reenter");
            validData = false;
            return;
        }

        validData = true;
    }

    /**
     * VALIDATE-DATA SECTION — additional validation on sort code and customer number.
     * Corresponds to VD010 in BNK1DCS.cbl
     */
    void validateData() {
        String sortCode = inqCustCommarea.getSortCode();
        if ("000000".equals(sortCode)) {
            validData = false;
            screenData.setMessage(
                    "The Sort code / Customer number combination is not VALID.");
            return;
        }

        String custNo = screenData.getCustomerNumberInput();
        if ("0".equals(custNo) || "0000000000".equals(custNo)
                || "9999999999".equals(custNo)) {
            validData = false;
            screenData.setMessage("The customer number is not VALID.");
        }
    }

    /**
     * GET-CUST-DATA SECTION — inquires on a customer via the INQCUST sub-program.
     * Corresponds to GCD010 in BNK1DCS.cbl
     */
    void getCustomerData() {
        inqCustCommarea = new InqCustCommarea();

        String custNoStr = screenData.getCustomerNumberInput();
        try {
            inqCustCommarea.setCustomerNumber(Long.parseLong(custNoStr));
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING,
                    "Invalid customer number format: {0}", custNoStr);
            screenData.setMessage("Please enter a customer number.");
            validData = false;
            return;
        }

        InqCustCommarea response = customerService.inquireCustomer(inqCustCommarea);
        if (response == null) {
            screenData.setMessage("Sorry, but that customer number was not found.");
            validData = false;
            clearCustomerFields();
            return;
        }
        inqCustCommarea = response;

        if (isBlankOrEmpty(inqCustCommarea.getName())
                && isBlankOrEmpty(inqCustCommarea.getAddress())) {
            screenData.setMessage("Sorry, but that customer number was not found.");
            validData = false;
            clearCustomerFields();
            return;
        }

        mapInquiryToScreen();

        String custNo = screenData.getCustomerNumberInput();
        if ("0".equals(custNo) || "0000000000".equals(custNo)
                || "9999999999".equals(custNo)) {
            screenData.setMessage("Customer lookup successful.");
        } else {
            screenData.setMessage(
                    "Customer lookup successful. <PF5> to Delete. "
                            + "<PF10> to Update.");
        }
    }

    /**
     * DEL-CUST-DATA SECTION — deletes a customer via the DELCUS sub-program.
     * Corresponds to DCD010 in BNK1DCS.cbl
     */
    void deleteCustomerData() {
        DelCusCommarea delCommarea = new DelCusCommarea();
        delCommarea.setCustomerNumber(screenData.getCustomerNumber2());

        DelCusCommarea response = customerService.deleteCustomer(delCommarea);
        if (response == null) {
            screenData.setMessage(
                    "Sorry but an error occurred. Customer NOT deleted.");
            validData = false;
            return;
        }

        if ("N".equals(response.getDeleteSuccess())) {
            handleDeleteFailure(response);
            return;
        }

        clearCustomerFields();
        screenData.setMessage(
                "Customer " + response.getCustomerNumber()
                        + " and associated accounts were successfully deleted.");
    }

    private void handleDeleteFailure(DelCusCommarea response) {
        validData = false;
        String failCode = response.getDeleteFailCode();

        switch (failCode) {
            case "1" -> screenData.setMessage(
                    "Sorry but that Cust no was not found."
                            + " Customer NOT deleted.");
            case "2" -> screenData.setMessage(
                    "Sorry but a datastore error occurred."
                            + " Action NOT applied.");
            case "3" -> screenData.setMessage(
                    "Sorry but a delete error occurred."
                            + " Customer NOT deleted.");
            default -> screenData.setMessage(
                    "Sorry but an error occurred."
                            + " Customer NOT deleted.");
        }

        screenData.setSortCode(response.getSortCode());
    }

    /**
     * UPDATE-CUST-DATA SECTION — updates a customer via the UPDCUST sub-program.
     * Corresponds to UPDCD010 in BNK1DCS.cbl
     */
    void updateCustomerData(DisplayCustomerCommarea commarea) {
        UpdCustCommarea updCommarea = new UpdCustCommarea();

        updCommarea.setSortCode(screenData.getSortCode());
        updCommarea.setCustomerNumber(screenData.getCustomerNumber2());
        updCommarea.setName(screenData.getCustomerName());

        String combinedAddress = padRight(screenData.getCustomerAddress1(), 60)
                + padRight(screenData.getCustomerAddress2(), 60)
                + padRight(screenData.getCustomerAddress3(), 40);
        updCommarea.setAddress(combinedAddress);

        int dobValue = buildDateValue(
                screenData.getDobDay(),
                screenData.getDobMonth(),
                screenData.getDobYear());
        updCommarea.setDob(dobValue);

        int creditScoreValue = parseIntSafe(screenData.getCreditScore(), 0);
        updCommarea.setCreditScore(creditScoreValue);

        int csReviewDateValue = buildDateValue(
                screenData.getCsReviewDateDay(),
                screenData.getCsReviewDateMonth(),
                screenData.getCsReviewDateYear());
        updCommarea.setCsReviewDate(csReviewDateValue);

        updCommarea.setUpdateSuccess(" ");
        updCommarea.setUpdateFailCode(" ");

        UpdCustCommarea response = customerService.updateCustomer(updCommarea);
        if (response == null) {
            screenData.setMessage(
                    "Sorry but an unknown error occurred."
                            + " Customer NOT updated.");
            validData = false;
            return;
        }

        if ("N".equals(response.getUpdateSuccess())) {
            handleUpdateFailure(response);
            return;
        }

        mapUpdateResponseToScreen(response);
        screenData.setMessage(
                "Customer " + response.getCustomerNumber()
                        + " was updated successfully");

        commarea.setUpdateFlag("N");
    }

    private void handleUpdateFailure(UpdCustCommarea response) {
        validData = false;
        String failCode = response.getUpdateFailCode();

        switch (failCode) {
            case "1" -> screenData.setMessage(
                    "Sorry but that Cust no was not found."
                            + " Customer NOT updated.");
            case "2" -> screenData.setMessage(
                    "Sorry but a datastore error occurred."
                            + " Customer NOT updated.");
            case "3" -> screenData.setMessage(
                    "Sorry but an update error occurred."
                            + " Customer NOT updated.");
            default -> screenData.setMessage(
                    "Sorry but an unknown error occurred."
                            + " Customer NOT updated.");
        }

        screenData.setSortCode(response.getSortCode());
    }

    /**
     * UNPROT-CUST-DATA SECTION — prepares for update mode by preserving
     * current data in the commarea and marking fields as editable.
     * Corresponds to UCD010 in BNK1DCS.cbl
     */
    void unprotectCustomerData(DisplayCustomerCommarea commarea) {
        commarea.setEye("CUST");
        commarea.setSortCode(screenData.getSortCode());
        commarea.setCustomerNumber(screenData.getCustomerNumber2());
        commarea.setName(screenData.getCustomerName());

        String combinedAddress = padRight(screenData.getCustomerAddress1(), 60)
                + padRight(screenData.getCustomerAddress2(), 60)
                + padRight(screenData.getCustomerAddress3(), 40);
        commarea.setAddress(combinedAddress);

        int dobValue = buildDateValue(
                screenData.getDobDay(),
                screenData.getDobMonth(),
                screenData.getDobYear());
        commarea.setDob(dobValue);

        int creditScoreValue = parseIntSafe(screenData.getCreditScore(), 0);
        commarea.setCreditScore(creditScoreValue);

        int csReviewDateValue = buildDateValue(
                screenData.getCsReviewDateDay(),
                screenData.getCsReviewDateMonth(),
                screenData.getCsReviewDateYear());
        commarea.setCsReviewDate(csReviewDateValue);

        commarea.setUpdateFlag("Y");
    }

    /**
     * Copies screen data back to the commarea for the next pseudo-conversational
     * interaction. Corresponds to the data movement at the end of A010.
     */
    void copyScreenToCommarea(DisplayCustomerCommarea commarea) {
        if (screenData.getSortCode() != null && !screenData.getSortCode().isEmpty()) {
            commarea.setSortCode(screenData.getSortCode());
        }
        if (screenData.getCustomerNumber2() != null
                && !screenData.getCustomerNumber2().isEmpty()) {
            commarea.setCustomerNumber(screenData.getCustomerNumber2());
        }
        if (screenData.getCustomerName() != null
                && !screenData.getCustomerName().isEmpty()) {
            commarea.setName(screenData.getCustomerName());
        }
    }

    // --- Helper methods ---

    private void mapInquiryToScreen() {
        screenData.setSortCode(inqCustCommarea.getSortCode());
        screenData.setCustomerNumber2(
                String.valueOf(inqCustCommarea.getCustomerNumber()));
        screenData.setCustomerName(inqCustCommarea.getName());

        splitAddressToScreen(inqCustCommarea.getAddress());

        screenData.setDobDay(String.valueOf(inqCustCommarea.getDobDay()));
        screenData.setDobMonth(String.valueOf(inqCustCommarea.getDobMonth()));
        screenData.setDobYear(String.valueOf(inqCustCommarea.getDobYear()));

        screenData.setCreditScore(
                String.valueOf(inqCustCommarea.getCreditScore()));
        screenData.setCsReviewDateDay(
                String.valueOf(inqCustCommarea.getCsReviewDay()));
        screenData.setCsReviewDateMonth(
                String.valueOf(inqCustCommarea.getCsReviewMonth()));
        screenData.setCsReviewDateYear(
                String.valueOf(inqCustCommarea.getCsReviewYear()));
    }

    private void mapUpdateResponseToScreen(UpdCustCommarea response) {
        screenData.setSortCode(response.getSortCode());
        screenData.setCustomerNumber2(response.getCustomerNumber());
        screenData.setCustomerName(response.getName());

        splitAddressToScreen(response.getAddress());

        int dob = response.getDob();
        screenData.setDobDay(String.valueOf(dob / 1000000));
        screenData.setDobMonth(String.valueOf((dob / 10000) % 100));
        screenData.setDobYear(String.valueOf(dob % 10000));

        screenData.setCreditScore(String.valueOf(response.getCreditScore()));
        screenData.setCsReviewDateDay(
                String.valueOf(response.getCsReviewDay()));
        screenData.setCsReviewDateMonth(
                String.valueOf(response.getCsReviewMonth()));
        screenData.setCsReviewDateYear(
                String.valueOf(response.getCsReviewYear()));
    }

    private void splitAddressToScreen(String address) {
        if (address == null) {
            address = "";
        }
        String padded = padRight(address, 160);
        screenData.setCustomerAddress1(padded.substring(0, 60).stripTrailing());
        screenData.setCustomerAddress2(padded.substring(60, 120).stripTrailing());
        screenData.setCustomerAddress3(padded.substring(120, 160).stripTrailing());
    }

    private void clearCustomerFields() {
        screenData.setSortCode("");
        screenData.setCustomerNumber2("");
        screenData.setCustomerName("");
        screenData.setCustomerAddress1("");
        screenData.setCustomerAddress2("");
        screenData.setCustomerAddress3("");
        screenData.setDobDay("");
        screenData.setDobMonth("");
        screenData.setDobYear("");
        screenData.setCreditScore("");
        screenData.setCsReviewDateDay("");
        screenData.setCsReviewDateMonth("");
        screenData.setCsReviewDateYear("");
    }

    /**
     * Removes formatting characters (commas, currency symbols, etc.)
     * from a numeric field. Equivalent to EXEC CICS BIF DEEDIT.
     */
    static String deEditField(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^0-9]", "");
    }

    static boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (char c : value.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    static String extractTitle(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String[] parts = name.stripLeading().split("\\s+", 2);
        return parts[0];
    }

    static boolean isBlankOrEmpty(String value) {
        return value == null || value.isBlank();
    }

    static String padRight(String value, int length) {
        if (value == null) {
            return " ".repeat(length);
        }
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        return value + " ".repeat(length - value.length());
    }

    static int buildDateValue(String day, String month, String year) {
        int dd = parseIntSafe(day, 0);
        int mm = parseIntSafe(month, 0);
        int yyyy = parseIntSafe(year, 0);
        return dd * 1000000 + mm * 10000 + yyyy;
    }

    static int parseIntSafe(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean isValidData() {
        return validData;
    }

    public DisplayCustomerScreenData getScreenData() {
        return screenData;
    }

    public InqCustCommarea getInqCustCommarea() {
        return inqCustCommarea;
    }
}
