/*
 *
 *    Copyright IBM Corp. 2023
 *
 *
 */

package com.ibm.cics.cip.bank.migrated.creditagency;

/**
 * Represents the ABNDINFO-REC structure from the COBOL copybook ABNDINFO.cpy.
 * Used for recording abend (abnormal end) information for error handling.
 *
 * <pre>
 * COBOL layout (ABNDINFO-REC):
 *   03 ABND-VSAM-KEY
 *      05 ABND-UTIME-KEY     PIC S9(15) COMP-3
 *      05 ABND-TASKNO-KEY    PIC 9(4)
 *   03 ABND-APPLID           PIC X(8)
 *   03 ABND-TRANID           PIC X(4)
 *   03 ABND-DATE             PIC X(10)
 *   03 ABND-TIME             PIC X(8)
 *   03 ABND-CODE             PIC X(4)
 *   03 ABND-PROGRAM          PIC X(8)
 *   03 ABND-RESPCODE         PIC S9(8) DISPLAY SIGN LEADING SEPARATE
 *   03 ABND-RESP2CODE        PIC S9(8) DISPLAY SIGN LEADING SEPARATE
 *   03 ABND-SQLCODE          PIC S9(8) DISPLAY SIGN LEADING SEPARATE
 *   03 ABND-FREEFORM         PIC X(600)
 * </pre>
 */
public class AbendInfo {

    private long utimeKey;
    private int taskNoKey;
    private String applId;
    private String tranId;
    private String date;
    private String time;
    private String code;
    private String program;
    private int respCode;
    private int resp2Code;
    private int sqlCode;
    private String freeform;

    public AbendInfo() {
        this.utimeKey = 0;
        this.taskNoKey = 0;
        this.applId = "";
        this.tranId = "";
        this.date = "";
        this.time = "";
        this.code = "";
        this.program = "";
        this.respCode = 0;
        this.resp2Code = 0;
        this.sqlCode = 0;
        this.freeform = "";
    }

    public long getUtimeKey() {
        return utimeKey;
    }

    public void setUtimeKey(long utimeKey) {
        this.utimeKey = utimeKey;
    }

    public int getTaskNoKey() {
        return taskNoKey;
    }

    public void setTaskNoKey(int taskNoKey) {
        this.taskNoKey = taskNoKey;
    }

    public String getApplId() {
        return applId;
    }

    public void setApplId(String applId) {
        this.applId = applId;
    }

    public String getTranId() {
        return tranId;
    }

    public void setTranId(String tranId) {
        this.tranId = tranId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public int getRespCode() {
        return respCode;
    }

    public void setRespCode(int respCode) {
        this.respCode = respCode;
    }

    public int getResp2Code() {
        return resp2Code;
    }

    public void setResp2Code(int resp2Code) {
        this.resp2Code = resp2Code;
    }

    public int getSqlCode() {
        return sqlCode;
    }

    public void setSqlCode(int sqlCode) {
        this.sqlCode = sqlCode;
    }

    public String getFreeform() {
        return freeform;
    }

    public void setFreeform(String freeform) {
        this.freeform = freeform;
    }

    @Override
    public String toString() {
        return "AbendInfo["
                + "utimeKey=" + utimeKey
                + ", taskNoKey=" + taskNoKey
                + ", applId=" + applId
                + ", tranId=" + tranId
                + ", date=" + date
                + ", time=" + time
                + ", code=" + code
                + ", program=" + program
                + ", respCode=" + respCode
                + ", resp2Code=" + resp2Code
                + ", sqlCode=" + sqlCode
                + ", freeform=" + freeform
                + "]";
    }
}
