/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.transferfunds;

/**
 * Gateway interface for the transfer function (migrated from XFRFUN COBOL
 * program). Implementations perform the actual fund transfer between accounts
 * by looking up account data, updating balances, and recording transactions.
 */
@FunctionalInterface
public interface TransferFunctionGateway {

    TransferFunctionResponse execute(TransferFunctionRequest request);
}
