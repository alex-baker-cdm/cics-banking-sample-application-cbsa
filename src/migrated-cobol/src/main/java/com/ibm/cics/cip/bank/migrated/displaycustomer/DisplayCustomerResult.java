package com.ibm.cics.cip.bank.migrated.displaycustomer;

/**
 * Encapsulates the outcome of a display customer transaction.
 * Replaces the CICS SEND MAP / RETURN mechanism.
 */
public class DisplayCustomerResult {

    public enum Action {
        SEND_MAP,
        RETURN_TO_MENU,
        SESSION_ENDED,
        CLEAR_SCREEN,
        ABEND
    }

    private Action action;
    private DisplayCustomerScreenData screenData;
    private DisplayCustomerCommarea commarea;
    private boolean eraseScreen;
    private boolean alarm;
    private boolean fieldsUnprotected;
    private String abendMessage;

    public DisplayCustomerResult(Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public DisplayCustomerScreenData getScreenData() {
        return screenData;
    }

    public void setScreenData(DisplayCustomerScreenData screenData) {
        this.screenData = screenData;
    }

    public DisplayCustomerCommarea getCommarea() {
        return commarea;
    }

    public void setCommarea(DisplayCustomerCommarea commarea) {
        this.commarea = commarea;
    }

    public boolean isEraseScreen() {
        return eraseScreen;
    }

    public void setEraseScreen(boolean eraseScreen) {
        this.eraseScreen = eraseScreen;
    }

    public boolean isAlarm() {
        return alarm;
    }

    public void setAlarm(boolean alarm) {
        this.alarm = alarm;
    }

    public boolean isFieldsUnprotected() {
        return fieldsUnprotected;
    }

    public void setFieldsUnprotected(boolean fieldsUnprotected) {
        this.fieldsUnprotected = fieldsUnprotected;
    }

    public String getAbendMessage() {
        return abendMessage;
    }

    public void setAbendMessage(String abendMessage) {
        this.abendMessage = abendMessage;
    }
}
