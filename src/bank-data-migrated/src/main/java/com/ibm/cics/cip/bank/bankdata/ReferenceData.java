/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from BANKDATA.cbl INITIALISE-ARRAYS section.
 * Contains all static reference data used for generating customer and account
 * records. Data values are preserved exactly as defined in the original COBOL.
 */
package com.ibm.cics.cip.bank.bankdata;

import java.math.BigDecimal;
import java.util.List;

public final class ReferenceData {

    private ReferenceData() {
    }

    public static final String SORT_CODE = "987654";

    public static final List<String> TITLES = List.of(
            "Mr", "Mrs", "Miss", "Ms",
            "Mr", "Mrs", "Miss", "Ms",
            "Mr", "Mrs", "Miss", "Ms",
            "Mr", "Mrs", "Miss", "Ms",
            "Mr", "Mrs", "Miss", "Ms",
            "Dr", "Drs", "Dr", "Ms",
            "Dr", "Ms", "Dr", "Ms",
            "Professor", "Professor", "Professor",
            "Lord", "Sir", "Sir", "Lady", "Lady");

    public static final List<String> FORENAMES = List.of(
            "Michael", "Will", "Geoff", "Chris", "Dave",
            "Luke", "Adam", "Giuseppe", "James", "Jon",
            "Andy", "Lou", "Robert", "Sam", "Frederick",
            "Buford", "William", "Howard", "Anthony", "Bruce",
            "Peter", "Stephen", "Donald", "Dennis", "Harold",
            "Amy", "Belinda", "Charlotte", "Donna", "Felicia",
            "Gretchen", "Henrietta", "Imogen", "Josephine", "Kimberley",
            "Lucy", "Monica", "Natalie", "Ophelia", "Patricia",
            "Querida", "Rachel", "Samantha", "Tanya", "Ulrika",
            "Virginia", "Wendy", "Xaviera", "Yvonne", "Zsa Zsa");

    public static final String INITIALS_STRING = "ABCDEFGHIJLKMNOPQRSTUVWXYZ    ";

    public static final List<String> SURNAMES = List.of(
            "Jones", "Davidson", "Baker", "Smith", "Taylor",
            "Evans", "Roberts", "Wright", "Walker", "Green",
            "Price", "Downton", "Gatting", "Robinson", "Justice",
            "Tell", "Stark", "Strange", "Parker", "Blake",
            "Jackson", "Groves", "Palmer", "Lloyd", "Hughes",
            "Briggs", "Higins", "Goodwin", "Valmont", "Brown",
            "Hopkins", "Bonney", "Jenkins", "Lloyd", "Wilmore",
            "Franklin", "Renton", "Seward", "Morris", "Johnson",
            "Brennan", "Thomson", "Barker", "Corbett", "Weber",
            "Leigh", "Croft", "Walken", "Dubois", "Stephens");

    public static final List<String> STREET_NAMES_TREE = List.of(
            "Acacia", "Birch", "Cypress", "Douglas", "Elm",
            "Fir", "Gorse", "Holly", "Ironwood", "Joshua",
            "Kapok", "Laburnam", "Maple", "Nutmeg", "Oak",
            "Pine", "Quercine", "Rowan", "Sycamore", "Thorn",
            "Ulmus", "Viburnum", "Willow", "Xylophone", "Yew",
            "Zebratree");

    public static final List<String> STREET_NAMES_ROAD = List.of(
            "Avenue", "Boulevard", "Close", "Crescent", "Drive",
            "Escalade", "Frontage", "Lane", "Mews", "Rise",
            "Court", "Opening", "Loke", "Square", "Houses",
            "Gate", "Street", "Grove", "March");

    public static final List<String> TOWNS = List.of(
            "Norwich", "Acle", "Aylsham", "Wymondham", "Attleborough",
            "Cromer", "Cambridge", "Peterborough", "Weobley", "Wembley",
            "Hereford", "Ross-on-Wye", "Hay-on-Wye", "Nottingham", "Northampton",
            "Nuneaton", "Oxford", "Oswestry", "Ormskirk", "Royston",
            "Chilcomb", "Winchester", "Wrexham", "Crewe", "Plymouth",
            "Portsmouth", "Forfar", "Fife", "Aberdeen", "Glasgow",
            "Birmingham", "Bolton", "Whitby", "Manchester", "Chester",
            "Leicester", "Lowestoft", "Ipswich", "Colchester", "Dover",
            "Brighton", "Salisbury", "Bristol", "Bath", "Gloucester",
            "Cheltenham", "Durham", "Carlisle", "York", "Exeter");

    public static final List<String> ACCOUNT_TYPES = List.of(
            "ISA", "SAVING", "CURRENT", "LOAN", "MORTGAGE");

    public static final List<BigDecimal> ACCOUNT_INTEREST_RATES = List.of(
            new BigDecimal("2.10"),
            new BigDecimal("1.75"),
            new BigDecimal("0.00"),
            new BigDecimal("17.90"),
            new BigDecimal("5.25"));

    public static final List<Integer> ACCOUNT_OVERDRAFT_LIMITS = List.of(
            0, 0, 100, 0, 0);

    public static final String LAST_STATEMENT_DATE = "01.07.2021";
    public static final String NEXT_STATEMENT_DATE = "01.08.2021";
}
