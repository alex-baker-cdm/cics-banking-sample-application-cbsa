/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.migrated.createcustomer;

import java.util.logging.Logger;

/**
 * Java 21 migration of BNK1CCS.cbl — the Create Customer BMS screen program
 * from the CICS Banking Sample Application.
 *
 * <p>This class preserves all business logic from the original COBOL program:
 * <ul>
 *   <li>Title normalisation and validation (EDIT-DATA section, lines 576–934)</li>
 *   <li>Field-level input validation with cursor positioning</li>
 *   <li>Name and address assembly for the CRECUST subprogram
 *       (CRE-CUST-DATA section, lines 937–1096)</li>
 *   <li>Creation-result interpretation including fail-code messages</li>
 *   <li>Screen output field population</li>
 * </ul>
 *
 * <p>CICS terminal management (UCTRAN, SEND MAP, RECEIVE MAP, HANDLE ABEND)
 * is not migrated as it is infrastructure-level rather than business logic.
 * The {@link CustomerCreator} functional interface replaces the
 * {@code EXEC CICS LINK PROGRAM('CRECUST')} call.
 */
public class CreateCustomerScreen {

    private static final Logger logger =
            Logger.getLogger(CreateCustomerScreen.class.getName());

    private static final String MSG_CLEAR_SCREEN =
            "Please clear screen before creating new user";
    private static final String MSG_VALID_TITLES =
            "Valid titles are: Mr,Mrs,Miss,Ms,Dr,Drs,Professor,Lord,Sir,Lady";
    private static final String MSG_INVALID_FIRST_NAME =
            "Please supply a valid First Name";
    private static final String MSG_INVALID_SURNAME =
            "Please supply a valid Surname";
    private static final String MSG_INVALID_ADDRESS =
            "Please supply a valid Address Line 1";
    private static final String MSG_INVALID_DOB_DD =
            "Please supply a valid Date of Birth DD";
    private static final String MSG_INVALID_DOB_MM =
            "Please supply a valid Date of Birth MM";
    private static final String MSG_INVALID_DOB_YYYY =
            "Please supply a valid Date of Birth YYYY";
    private static final String MSG_NON_NUMERIC_DD =
            "Non numeric Date of Birth DD entered";
    private static final String MSG_NON_NUMERIC_MM =
            "Non numeric Date of Birth MM entered";
    private static final String MSG_NON_NUMERIC_YYYY =
            "Non numeric Date of Birth YYYY entered";
    private static final String MSG_DOB_DD_RANGE =
            "Please supply a valid Date of Birth (DD)";
    private static final String MSG_DOB_MM_RANGE =
            "Please supply a valid Date of Birth (MM)";
    private static final String MSG_MISSING_DATA =
            "Missing expected data.";
    private static final String MSG_CREATION_FAILED =
            "Sorry but unable to create Customer record";
    private static final String MSG_TOO_OLD =
            "Sorry, customer is too old. Please check D.O.B.";
    private static final String MSG_FUTURE_DOB =
            "Sorry, customer D.O.B. is in the future.";
    private static final String MSG_INVALID_DOB =
            "Sorry, customer D.O.B. is invalid.";
    private static final String MSG_CREATION_SUCCESS =
            "The Customer record has been successfully created";

    private final CustomerCreator customerCreator;

    public CreateCustomerScreen(CustomerCreator customerCreator) {
        this.customerCreator = customerCreator;
    }

    /**
     * Validates the input and, if valid, creates a customer. This corresponds
     * to the PROCESS-MAP section of BNK1CCS.cbl (pressing ENTER).
     *
     * @param input the screen input fields
     * @return the response to display on the screen
     */
    public CreateCustomerResponse processMap(CreateCustomerInput input) {
        logger.fine(() -> "processMap called with input: " + input);

        ValidationError validationError = editData(input);
        if (validationError != null) {
            logger.fine(() -> "Validation failed: " + validationError.message());
            return buildValidationErrorResponse(validationError);
        }

        return createCustomerData(input);
    }

    /**
     * Validates all input fields. Migrated from EDIT-DATA section (lines 576–934).
     *
     * <p>Validation order matches the original COBOL exactly:
     * <ol>
     *   <li>Existing customer number must not be present</li>
     *   <li>Title must be supplied and valid</li>
     *   <li>First name required</li>
     *   <li>Surname required</li>
     *   <li>Address line 1 required</li>
     *   <li>DOB day: supplied, numeric, 01–31</li>
     *   <li>DOB month: supplied, numeric, 01–12</li>
     *   <li>DOB year: supplied, numeric, 4 digits</li>
     *   <li>Final check for all mandatory fields</li>
     * </ol>
     *
     * @param input the raw screen input
     * @return the first validation error found, or {@code null} if all valid
     */
    public ValidationError editData(CreateCustomerInput input) {
        if (hasContent(input.existingCustomerNumber())) {
            return new ValidationError(
                    ValidationError.ScreenField.EXISTING_CUSTOMER_NUMBER,
                    MSG_CLEAR_SCREEN);
        }

        if (isBlankOrUnderscore(input.title())) {
            return new ValidationError(
                    ValidationError.ScreenField.TITLE,
                    MSG_VALID_TITLES);
        }

        CustomerTitle title = CustomerTitle.fromInput(input.title());
        if (title == null) {
            return new ValidationError(
                    ValidationError.ScreenField.TITLE,
                    MSG_VALID_TITLES);
        }

        if (isBlankOrUnderscore(input.firstName())) {
            return new ValidationError(
                    ValidationError.ScreenField.FIRST_NAME,
                    MSG_INVALID_FIRST_NAME);
        }

        if (isBlankOrUnderscore(input.lastName())) {
            return new ValidationError(
                    ValidationError.ScreenField.LAST_NAME,
                    MSG_INVALID_SURNAME);
        }

        if (isBlankOrUnderscore(input.addressLine1())
                || startsWithUnderscore(input.addressLine1())) {
            return new ValidationError(
                    ValidationError.ScreenField.ADDRESS_LINE_1,
                    MSG_INVALID_ADDRESS);
        }

        if (isBlankOrUnderscore(input.dobDay())) {
            return new ValidationError(
                    ValidationError.ScreenField.DOB_DAY,
                    MSG_INVALID_DOB_DD);
        }

        if (isBlankOrUnderscore(input.dobMonth())) {
            return new ValidationError(
                    ValidationError.ScreenField.DOB_MONTH,
                    MSG_INVALID_DOB_MM);
        }

        if (isBlankOrUnderscoreYear(input.dobYear())) {
            return new ValidationError(
                    ValidationError.ScreenField.DOB_YEAR,
                    MSG_INVALID_DOB_YYYY);
        }

        if (!isNumeric(input.dobDay())) {
            return new ValidationError(
                    ValidationError.ScreenField.DOB_DAY,
                    MSG_NON_NUMERIC_DD);
        }

        if (!isNumeric(input.dobMonth())) {
            return new ValidationError(
                    ValidationError.ScreenField.DOB_MONTH,
                    MSG_NON_NUMERIC_MM);
        }

        if (!isNumeric(input.dobYear())) {
            return new ValidationError(
                    ValidationError.ScreenField.DOB_YEAR,
                    MSG_NON_NUMERIC_YYYY);
        }

        int day = Integer.parseInt(input.dobDay().strip());
        if (day < 1 || day > 31) {
            return new ValidationError(
                    ValidationError.ScreenField.DOB_DAY,
                    MSG_DOB_DD_RANGE);
        }

        int month = Integer.parseInt(input.dobMonth().strip());
        if (month < 1 || month > 12) {
            return new ValidationError(
                    ValidationError.ScreenField.DOB_MONTH,
                    MSG_DOB_MM_RANGE);
        }

        if (hasMissingMandatoryField(input)) {
            return new ValidationError(
                    findFirstMissingField(input),
                    MSG_MISSING_DATA);
        }

        return null;
    }

    /**
     * Normalises a title string. Migrated from the EVALUATE block in EDIT-DATA
     * (lines 604–781).
     *
     * @param rawTitle the raw title from the input
     * @return the canonical display form, or {@code null} if invalid
     */
    public String normaliseTitle(String rawTitle) {
        CustomerTitle title = CustomerTitle.fromInput(rawTitle);
        return title != null ? title.displayName() : null;
    }

    /**
     * Assembles the full customer name from individual fields. Migrated from
     * CRE-CUST-DATA (lines 949–961).
     *
     * <p>The COBOL STRING statement concatenates: title (delimited by space),
     * a space, first name (delimited by space), a space, initials (delimited
     * by space), a space, surname (full length).
     *
     * @param title      normalised title
     * @param firstName  first name (underscores replaced with spaces)
     * @param initials   middle initials (underscores replaced with spaces)
     * @param lastName   surname (underscores replaced with spaces)
     * @return the assembled name, truncated to 60 characters
     */
    public String assembleName(String title, String firstName,
                               String initials, String lastName) {
        String cleanTitle = cleanUnderscores(title).stripTrailing();
        String cleanFirst = cleanUnderscores(firstName).stripTrailing();
        String cleanInitials = cleanUnderscores(
                initials != null ? initials : "").stripTrailing();
        String cleanLast = cleanUnderscores(lastName);

        StringBuilder sb = new StringBuilder();
        appendDelimitedBySpace(sb, cleanTitle);
        sb.append(' ');
        appendDelimitedBySpace(sb, cleanFirst);
        sb.append(' ');
        if (!cleanInitials.isEmpty()) {
            appendDelimitedBySpace(sb, cleanInitials);
            sb.append(' ');
        }
        sb.append(cleanLast);

        String result = sb.toString();
        if (result.length() > CustomerCreationRequest.NAME_MAX_LENGTH) {
            result = result.substring(0, CustomerCreationRequest.NAME_MAX_LENGTH);
        }
        return result;
    }

    /**
     * Assembles the full address from three lines. Migrated from CRE-CUST-DATA
     * (lines 963–970).
     *
     * <p>The COBOL STRING concatenates the three address fields at their full
     * lengths (60 + 60 + 40 = 160 characters). Each line is padded to its
     * fixed width.
     *
     * @param line1 address line 1 (max 60 chars)
     * @param line2 address line 2 (max 60 chars)
     * @param line3 address line 3 (max 40 chars)
     * @return the assembled address, exactly 160 characters
     */
    public String assembleAddress(String line1, String line2, String line3) {
        String clean1 = padRight(cleanUnderscores(nullToEmpty(line1)),
                CustomerCreationRequest.ADDRESS_LINE_1_LENGTH);
        String clean2 = padRight(cleanUnderscores(nullToEmpty(line2)),
                CustomerCreationRequest.ADDRESS_LINE_2_LENGTH);
        String clean3 = padRight(cleanUnderscores(nullToEmpty(line3)),
                CustomerCreationRequest.ADDRESS_LINE_3_LENGTH);
        return clean1 + clean2 + clean3;
    }

    /**
     * Interprets the creation result message. Migrated from CRE-CUST-DATA
     * (lines 1048–1075).
     *
     * @param result the result from the CRECUST subprogram
     * @return the screen message
     */
    public String interpretResultMessage(CustomerCreationResult result) {
        if (result.success()) {
            return MSG_CREATION_SUCCESS;
        }
        return switch (result.failCode()) {
            case CustomerCreationResult.FAIL_TOO_OLD -> MSG_TOO_OLD;
            case CustomerCreationResult.FAIL_FUTURE_DOB -> MSG_FUTURE_DOB;
            case CustomerCreationResult.FAIL_INVALID_DOB -> MSG_INVALID_DOB;
            default -> MSG_CREATION_FAILED;
        };
    }

    /**
     * Splits a 160-character address back into three lines. Migrated from
     * CRE-CUST-DATA (lines 1083–1086).
     */
    public String[] splitAddress(String address) {
        String padded = padRight(nullToEmpty(address),
                CustomerCreationRequest.ADDRESS_MAX_LENGTH);
        return new String[]{
                padded.substring(0, CustomerCreationRequest.ADDRESS_LINE_1_LENGTH),
                padded.substring(CustomerCreationRequest.ADDRESS_LINE_1_LENGTH,
                        CustomerCreationRequest.ADDRESS_LINE_1_LENGTH
                                + CustomerCreationRequest.ADDRESS_LINE_2_LENGTH),
                padded.substring(CustomerCreationRequest.ADDRESS_LINE_1_LENGTH
                        + CustomerCreationRequest.ADDRESS_LINE_2_LENGTH,
                        CustomerCreationRequest.ADDRESS_MAX_LENGTH)
        };
    }

    /**
     * Splits a credit-score review date (DDMMYYYY, 8 chars) into DD, MM, YYYY
     * components. Migrated from CRE-CUST-DATA (lines 1088–1093).
     */
    public String[] splitCsReviewDate(String csReviewDate) {
        String padded = padRight(nullToEmpty(csReviewDate), 8);
        return new String[]{
                padded.substring(0, 2),
                padded.substring(2, 4),
                padded.substring(4, 8)
        };
    }

    // --- Private implementation ---

    private CreateCustomerResponse createCustomerData(CreateCustomerInput input) {
        String normalisedTitle = normaliseTitle(input.title());
        String name = assembleName(normalisedTitle, input.firstName(),
                input.middleInitials(), input.lastName());
        String address = assembleAddress(input.addressLine1(),
                input.addressLine2(), input.addressLine3());

        int birthDay = Integer.parseInt(input.dobDay().strip());
        int birthMonth = Integer.parseInt(input.dobMonth().strip());
        int birthYear = Integer.parseInt(input.dobYear().strip());

        CustomerCreationRequest request = new CustomerCreationRequest(
                CustomerCreationRequest.EYECATCHER_VALUE,
                name,
                address,
                birthDay,
                birthMonth,
                birthYear);

        logger.fine(() -> "Calling CRECUST with request: " + request);
        CustomerCreationResult result = customerCreator.createCustomer(request);

        String message = interpretResultMessage(result);
        String[] addressLines = splitAddress(result.address());
        String[] reviewDateParts = splitCsReviewDate(result.csReviewDate());

        String sortCode = result.success() ? result.sortCode() : "";
        String customerNumber = result.success() ? result.customerNumber() : "";

        return new CreateCustomerResponse(
                message,
                sortCode,
                customerNumber,
                String.valueOf(result.creditScore()),
                reviewDateParts[0],
                reviewDateParts[1],
                reviewDateParts[2],
                String.valueOf(result.birthDay()),
                String.valueOf(result.birthMonth()),
                String.valueOf(result.birthYear()),
                addressLines[0],
                addressLines[1],
                addressLines[2],
                result.success());
    }

    private CreateCustomerResponse buildValidationErrorResponse(
            ValidationError error) {
        return new CreateCustomerResponse(
                error.message(),
                "", "", "", "", "", "",
                "", "", "",
                "", "", "",
                false);
    }

    private boolean hasMissingMandatoryField(CreateCustomerInput input) {
        return isBlankOrUnderscore(input.title())
                || isBlankOrUnderscore(input.firstName())
                || isBlankOrUnderscore(input.lastName())
                || isBlankOrUnderscore(input.addressLine1())
                || isBlankOrUnderscore(input.dobDay())
                || isBlankOrUnderscore(input.dobMonth())
                || isBlankOrUnderscore(input.dobYear());
    }

    private ValidationError.ScreenField findFirstMissingField(
            CreateCustomerInput input) {
        if (isBlankOrUnderscore(input.title())) {
            return ValidationError.ScreenField.TITLE;
        }
        if (isBlankOrUnderscore(input.firstName())) {
            return ValidationError.ScreenField.FIRST_NAME;
        }
        if (isBlankOrUnderscore(input.lastName())) {
            return ValidationError.ScreenField.LAST_NAME;
        }
        if (isBlankOrUnderscore(input.addressLine1())) {
            return ValidationError.ScreenField.ADDRESS_LINE_1;
        }
        if (isBlankOrUnderscore(input.dobDay())) {
            return ValidationError.ScreenField.DOB_DAY;
        }
        if (isBlankOrUnderscore(input.dobMonth())) {
            return ValidationError.ScreenField.DOB_MONTH;
        }
        if (isBlankOrUnderscore(input.dobYear())) {
            return ValidationError.ScreenField.DOB_YEAR;
        }
        return ValidationError.ScreenField.TITLE;
    }

    private static boolean isBlankOrUnderscore(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return value.replace('_', ' ').isBlank();
    }

    private static boolean isBlankOrUnderscoreYear(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String cleaned = value.replace('_', ' ').strip();
        return cleaned.length() < 4;
    }

    private static boolean startsWithUnderscore(String value) {
        return value != null && !value.isEmpty() && value.charAt(0) == '_';
    }

    private static boolean hasContent(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return !value.replace('_', ' ').isBlank();
    }

    private static boolean isNumeric(String value) {
        if (value == null) {
            return false;
        }
        String stripped = value.strip();
        if (stripped.isEmpty()) {
            return false;
        }
        for (int i = 0; i < stripped.length(); i++) {
            if (!Character.isDigit(stripped.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String cleanUnderscores(String value) {
        return value == null ? "" : value.replace('_', ' ');
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String padRight(String value, int length) {
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        return value + " ".repeat(length - value.length());
    }

    /**
     * Appends text up to the first space (COBOL DELIMITED BY SPACE behaviour).
     */
    private static void appendDelimitedBySpace(StringBuilder sb, String text) {
        int spaceIdx = text.indexOf(' ');
        if (spaceIdx >= 0) {
            sb.append(text, 0, spaceIdx);
        } else {
            sb.append(text);
        }
    }
}
