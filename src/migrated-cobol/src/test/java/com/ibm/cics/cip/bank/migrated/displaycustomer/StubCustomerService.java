package com.ibm.cics.cip.bank.migrated.displaycustomer;

/**
 * Test stub for CustomerService, replacing the CICS LINK calls to
 * INQCUST, DELCUS, and UPDCUST programs.
 */
class StubCustomerService implements CustomerService {

    private InqCustCommarea inqCustResponse;
    private DelCusCommarea delCusResponse;
    private UpdCustCommarea updCustResponse;

    private InqCustCommarea lastInquiryRequest;
    private DelCusCommarea lastDeleteRequest;
    private UpdCustCommarea lastUpdateRequest;

    @Override
    public InqCustCommarea inquireCustomer(InqCustCommarea commarea) {
        this.lastInquiryRequest = commarea;
        return inqCustResponse;
    }

    @Override
    public DelCusCommarea deleteCustomer(DelCusCommarea commarea) {
        this.lastDeleteRequest = commarea;
        return delCusResponse;
    }

    @Override
    public UpdCustCommarea updateCustomer(UpdCustCommarea commarea) {
        this.lastUpdateRequest = commarea;
        return updCustResponse;
    }

    void setInqCustResponse(InqCustCommarea response) {
        this.inqCustResponse = response;
    }

    void setDelCusResponse(DelCusCommarea response) {
        this.delCusResponse = response;
    }

    void setUpdCustResponse(UpdCustCommarea response) {
        this.updCustResponse = response;
    }

    InqCustCommarea getLastInquiryRequest() {
        return lastInquiryRequest;
    }

    DelCusCommarea getLastDeleteRequest() {
        return lastDeleteRequest;
    }

    UpdCustCommarea getLastUpdateRequest() {
        return lastUpdateRequest;
    }
}
