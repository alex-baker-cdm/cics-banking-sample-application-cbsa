/*
 *
 *    Copyright IBM Corp. 2023
 *
 *    Migrated from COBOL to Java 21.
 *    Original source: src/base/cobol_src/GETCOMPY.cbl
 *    Original copybook: src/base/cobol_copy/GETCOMPY.cpy
 *
 */

package com.ibm.cics.cip.bank.getcompany;

import java.util.logging.Logger;

/**
 * Java 21 migration of the GETCOMPY COBOL program.
 *
 * <p>The original COBOL program performs a single operation:
 * it populates the COMMAREA's company-name field with the
 * constant value "CICS Bank Sample Application" and returns.
 *
 * <p>This Java equivalent provides the same functionality without
 * any dependency on CICS infrastructure, making it suitable for
 * standalone deployment, unit testing, and modern service architectures.
 *
 * <h2>Original COBOL Logic (PROCEDURE DIVISION):</h2>
 * <pre>
 *   PREMIERE SECTION.
 *   A010.
 *       move 'CICS Bank Sample Application' to COMPANY-NAME.
 *       EXEC CICS RETURN
 *       END-EXEC.
 *       GOBACK.
 * </pre>
 *
 * <h2>Migration Notes:</h2>
 * <ul>
 *   <li>The EXEC CICS RETURN is replaced by a normal method return</li>
 *   <li>The COMMAREA is replaced by the {@link GetCompanyRequest} DTO</li>
 *   <li>The company name constant is configurable for flexibility</li>
 *   <li>Thread-safe: the service is stateless and can be shared across threads</li>
 * </ul>
 */
public class GetCompanyService {

    private static final Logger logger = Logger.getLogger(
            GetCompanyService.class.getName());

    /**
     * Default company name matching the original COBOL literal:
     * {@code move 'CICS Bank Sample Application' to COMPANY-NAME}
     */
    public static final String DEFAULT_COMPANY_NAME = "CICS Bank Sample Application";

    private final String companyName;

    /**
     * Creates a service instance with the default company name,
     * matching the original COBOL program behavior.
     */
    public GetCompanyService() {
        this(DEFAULT_COMPANY_NAME);
    }

    /**
     * Creates a service instance with a custom company name.
     * This constructor supports configuration-driven deployments
     * where the company name may differ across environments.
     *
     * @param companyName the company name to return; must not be null
     * @throws IllegalArgumentException if companyName is null or exceeds 40 characters
     */
    public GetCompanyService(String companyName) {
        if (companyName == null) {
            throw new IllegalArgumentException("Company name must not be null");
        }
        if (companyName.length() > GetCompanyRequest.COMPANY_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Company name must not exceed " + GetCompanyRequest.COMPANY_NAME_LENGTH + " characters");
        }
        this.companyName = companyName;
    }

    /**
     * Executes the migrated GETCOMPY program logic.
     *
     * <p>Equivalent to the original COBOL PROCEDURE DIVISION:
     * populates the request's company-name field with the configured
     * company name and returns.
     *
     * @param request the communication area (COMMAREA equivalent);
     *                must not be null
     * @return the same request object with company name populated
     * @throws IllegalArgumentException if request is null
     */
    public GetCompanyRequest getCompanyInfo(GetCompanyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        logger.fine(() -> "GETCOMPY: Setting company name to '" + companyName + "'");

        request.setCompanyName(companyName);

        return request;
    }

    /**
     * Convenience method that creates a new request, populates it,
     * and returns it. Equivalent to allocating a fresh COMMAREA
     * and linking to the GETCOMPY program.
     *
     * @return a new {@link GetCompanyRequest} with the company name set
     */
    public GetCompanyRequest getCompanyInfo() {
        return getCompanyInfo(new GetCompanyRequest());
    }

    /**
     * Returns the configured company name.
     *
     * @return the company name this service will return
     */
    public String getCompanyName() {
        return companyName;
    }
}
