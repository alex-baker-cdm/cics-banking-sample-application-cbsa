/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: ABNDINFO.cpy - Abend information record
 */
package com.ibm.cics.cip.bank.migrated.bnkmenu;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AbndInfo {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private long utimeKey;
    private int taskNoKey;
    private String applId = "";
    private String tranId = "";
    private String date = "";
    private String time = "";
    private String code = "";
    private String program = "";
    private int respCode;
    private int resp2Code;
    private int sqlCode;
    private String freeform = "";

    public void initialize() {
        utimeKey = 0;
        taskNoKey = 0;
        applId = "";
        tranId = "";
        date = "";
        time = "";
        code = "";
        program = "";
        respCode = 0;
        resp2Code = 0;
        sqlCode = 0;
        freeform = "";
    }

    public void populateTimeDate(LocalDateTime dateTime) {
        this.date = dateTime.format(DATE_FORMAT);
        this.time = dateTime.format(TIME_FORMAT);
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
        return "AbndInfo{" +
                "utimeKey=" + utimeKey +
                ", taskNoKey=" + taskNoKey +
                ", applId='" + applId + '\'' +
                ", tranId='" + tranId + '\'' +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", code='" + code + '\'' +
                ", program='" + program + '\'' +
                ", respCode=" + respCode +
                ", resp2Code=" + resp2Code +
                ", sqlCode=" + sqlCode +
                ", freeform='" + freeform + '\'' +
                '}';
    }
}
