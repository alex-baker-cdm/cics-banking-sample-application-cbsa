# API design & security standards baseline — alex-baker-cdm/cics-banking-sample-application-cbsa (2026-10-06)

## Summary

| | |
|---|---|
| Repo / branch | `alex-baker-cdm/cics-banking-sample-application-cbsa` @ `main` (6014f74) |
| Classification | provider |
| Surface | Liberty JAX-RS JSON API (`src/webui`, 34 routes); Spring Boot z/OS Connect Customer Services (18 routes) and Payment (3 routes) interfaces; 10 z/OS Connect API definitions |
| Spec present | Yes for the 10 z/OS Connect APIs (Swagger 2.0); none for the JAX-RS or Spring routes |
| Auth model | None in the runnable code: the Spring Boot interfaces have no Spring Security and accept anonymous requests; the Liberty JAX-RS web.xml declares no security constraints and disables XSRF protection; the checked-in z/OS Connect server.xml defines a basicRegistry (ibmuser/SYS1) but sets requireAuth=false and requireSecure=false. Any enforcement would have to come from CICS/Liberty SAF configuration outside this repo (needs confirmation). |
| Runtime verified | Yes for the two Spring Boot modules (built with Maven 3.6.3/JDK 17, started on :19080 and :19081, probed with curl). No for the Liberty WAR and z/OS Connect server (need CICS/Db2/VSAM/z/OS). |
| Findings | 19 — critical 1, high 9, medium 3, low 6 |

### How it was run

- `mvn -q -DskipTests package` in `src/Z-OS-Connect-Payment-Interface` and `src/Z-OS-Connect-Customer-Services-Interface` (the bundled `./mvnw` could not download Maven: Maven Central returned HTTP 429, so Maven 3.6.3 was installed via apt). Both WARs built.
- Started with `java -jar target/paymentinterface-1.1.war` (port 19080, context `/paymentinterface-1.1`) and `java -jar target/customerservices-1.0.war` (port 19081, context `/customerservices-1.0`) — ports and context paths as checked in; no files were modified.
- Backend host `winmvsb0.hursley.ibm.com:30701` (hard-coded in `ConnectionInfo.java`) does not resolve, so every outbound call fails with `WebClientRequestException`; this exercised the error-handling path.
- Probes: anonymous GET/POST on every Spring route, malformed/blank/NaN/negative amounts, 30-char and `../../` identifiers, 3 MB body, unknown routes, OPTIONS preflight from a foreign origin, response headers, `/actuator`, and log inspection for PII.
- Dependency scan: `mvn dependency:list` for both modules fed to the OSV API (`api.osv.dev/v1/querybatch` + per-vuln lookup); only HIGH/CRITICAL advisories with a fixed version are reported.
- Liberty WAR (`src/webui`) and z/OS Connect were reviewed at code level only.

## Route inventory

### Liberty JAX-RS JSON API (`src/webui`, WAR root `/webui`; backed by Db2 via `HBankDataAccess` and VSAM, counters via COBOL NEWACCNO/NEWCUSNO)

| Verb | Path | Resource | Line |
|---|---|---|---|
| POST | `/account` | AccountsResource.java | 159 |
| GET | `/account/{accountNumber}` | AccountsResource.java | 362 |
| GET | `/account/retrieveByCustomerNumber/{customerNumber}` | AccountsResource.java | 471 |
| PUT | `/account/{id}` | AccountsResource.java | 619 |
| PUT | `/account/debit/{id}` | AccountsResource.java | 789 |
| PUT | `/account/credit/{id}` | AccountsResource.java | 855 |
| PUT | `/account/transfer/{id}` | AccountsResource.java | 922 |
| DELETE | `/account/{accountNumber}` | AccountsResource.java | 1224 |
| GET | `/account` | AccountsResource.java | 1342 |
| GET | `/account/balance` | AccountsResource.java | 1462 |
| GET | `/companyName` | CompanyNameResource.java | 62 |
| GET | `/counter/account` | CounterResource.java | 84 |
| GET | `/counter/customer` | CounterResource.java | 130 |
| POST | `/counter/customer` | CounterResource.java | 177 |
| DELETE | `/counter/customer` | CounterResource.java | 225 |
| POST | `/counter/account` | CounterResource.java | 277 |
| DELETE | `/counter/account` | CounterResource.java | 324 |
| POST | `/customer` | CustomerResource.java | 108 |
| PUT | `/customer/{id}` | CustomerResource.java | 309 |
| GET | `/customer/{id}` | CustomerResource.java | 460 |
| DELETE | `/customer/{id}` | CustomerResource.java | 547 |
| GET | `/customer/all/town/{town}` | CustomerResource.java | 733 |
| GET | `/customer/all/surname/{surname}` | CustomerResource.java | 796 |
| GET | `/customer/all/age/{age}` | CustomerResource.java | 861 |
| GET | `/customer` | CustomerResource.java | 940 |
| GET | `/customer/name` | CustomerResource.java | 1057 |
| GET | `/processedTransaction` | ProcessedTransactionResource.java | 94 |
| POST | `/processedTransaction/debitCreditAccount` | ProcessedTransactionResource.java | 257 |
| POST | `/processedTransaction/transferLocal` | ProcessedTransactionResource.java | 306 |
| POST | `/processedTransaction/deleteCustomer` | ProcessedTransactionResource.java | 339 |
| POST | `/processedTransaction/createCustomer` | ProcessedTransactionResource.java | 375 |
| POST | `/processedTransaction/deleteAccount` | ProcessedTransactionResource.java | 412 |
| POST | `/processedTransaction/createAccount` | ProcessedTransactionResource.java | 449 |
| GET | `/sortCode` | SortCodeResource.java | 54 |

### Spring Boot z/OS Connect interfaces (browser forms + one parameter API)

| Verb | Path | Module | Controller | Line | Drives z/OS Connect API → COBOL |
|---|---|---|---|---|---|
| GET | `/customerservices-1.0/enqacct` | Customer Services | WebController.java | 124 | — (renders form) |
| POST | `/customerservices-1.0/enqacct` | Customer Services | WebController.java | 137 | — |
| GET | `/customerservices-1.0/enqcust` | Customer Services | WebController.java | 234 | — (renders form) |
| POST | `/customerservices-1.0/enqcust` | Customer Services | WebController.java | 241 | — |
| GET | `/customerservices-1.0/listacc` | Customer Services | WebController.java | 303 | — (renders form) |
| POST | `/customerservices-1.0/listacc` | Customer Services | WebController.java | 310 | — |
| GET | `/customerservices-1.0/createacc` | Customer Services | WebController.java | 372 | — (renders form) |
| POST | `/customerservices-1.0/createacc` | Customer Services | WebController.java | 381 | — |
| GET | `/customerservices-1.0/createcust` | Customer Services | WebController.java | 482 | — (renders form) |
| POST | `/customerservices-1.0/createcust` | Customer Services | WebController.java | 490 | — |
| GET | `/customerservices-1.0/updateacc` | Customer Services | WebController.java | 576 | — (renders form) |
| POST | `/customerservices-1.0/updateacc` | Customer Services | WebController.java | 587 | — |
| GET | `/customerservices-1.0/updatecust` | Customer Services | WebController.java | 679 | — (renders form) |
| POST | `/customerservices-1.0/updatecust` | Customer Services | WebController.java | 688 | — |
| GET | `/customerservices-1.0/delacct` | Customer Services | WebController.java | 784 | — (renders form) |
| POST | `/customerservices-1.0/delacct` | Customer Services | WebController.java | 791 | — |
| GET | `/customerservices-1.0/delcust` | Customer Services | WebController.java | 849 | — (renders form) |
| POST | `/customerservices-1.0/delcust` | Customer Services | WebController.java | 856 | — |
| POST | `/paymentinterface-1.1/submit` | Payment | ParamsController.java | 35 | — |
| GET | `/paymentinterface-1.1` | Payment | WebController.java | 51 | — (renders form) |
| POST | `/paymentinterface-1.1/paydbcr` | Payment | WebController.java | 58 | — |

### z/OS Connect API definitions (`src/zosconnect_artefacts/apis`, Swagger 2.0) → service archive → COBOL program

| Verb | Path | Service | COBOL |
|---|---|---|---|
| POST | `/creacc/insert` | CSacccre | CREACC |
| POST | `/crecust/insert` | CScustcre | CRECUST |
| DELETE | `/delacc/remove/{accno}` | CSaccdel | DELACC |
| DELETE | `/delcus/remove/{custno}` | CScustdel | DELCUS |
| GET | `/inqacccz/list/{custno}` | CScustacc | INQACCCU |
| GET | `/inqaccz/enquiry/{accno}` | CSaccenq | INQACC |
| GET | `/inqcustz/enquiry/{custno}` | CScustenq | INQCUST |
| PUT | `/makepayment/dbcr` | Pay | DBCRFUN |
| PUT | `/updacc/update` | CSaccupd | UPDACC |
| PUT | `/updcust/update` | CScustupd | UPDCUST |

## Findings by area

### A. Contract & design

| id | severity | title | file:lines | effort |
|---|---|---|---|---|
| CBSA-A-01 | medium | No machine-readable API spec for the Liberty JAX-RS JSON API or the Spring Boot interfaces; Swagger 2.0 exists only for the 10 z/OS Connect APIs | `src/zosconnect_artefacts/apis/*/api-docs/swagger.json`:basePath/paths of the 10 API definitions | M |
| CBSA-A-02 | high | Money-moving POST/PUT endpoints have no idempotency mechanism | `src/Z-OS-Connect-Payment-Interface/src/main/java/com/ibm/cics/cip/bank/springboot/paymentinterface/controllers/ParamsController.java`:35-40 (also src/Z-OS-Connect-Payment-Interface/src/main/java/com/ibm/cics/cip/bank/springboot/paymentinterface/controllers/WebController.java:58-76, src/webui/src/main/java/com/ibm/cics/cip/bankliberty/api/json/AccountsResource.java:790,856,923) | M |
| CBSA-A-03 | low | API version is hard-coded in the servlet context path and bumped in bulk commits; JAX-RS routes are unversioned | `src/Z-OS-Connect-Payment-Interface/src/main/resources/application.properties`:1-2 (and Customer Services application.properties:1-2) | S |

### B. Authentication & authorization

| id | severity | title | file:lines | effort |
|---|---|---|---|---|
| CBSA-B-01 | high | Spring Boot Payment and Customer Services interfaces expose state-changing routes with no authentication | `src/Z-OS-Connect-Payment-Interface/src/main/java/com/ibm/cics/cip/bank/springboot/paymentinterface/controllers/ParamsController.java`:22-76 | M |
| CBSA-B-02 | high | Liberty JAX-RS JSON API declares no security constraints, disables XSRF protection, and authorises nothing at object level | `src/webui/WebContent/WEB-INF/web.xml`:9-12 | L |

### C. Input validation

| id | severity | title | file:lines | effort |
|---|---|---|---|---|
| CBSA-C-01 | high | Payment API binds amounts as float and accepts unbounded or non-finite values and oversize identifiers | `src/Z-OS-Connect-Payment-Interface/src/main/java/com/ibm/cics/cip/bank/springboot/paymentinterface/controllers/ParamsController.java`:35-40 | M |
| CBSA-C-02 | high | Bean Validation annotations are silently ignored (javax.validation on Spring Boot 3), so unvalidated identifiers are concatenated into the backend URL path | `src/Z-OS-Connect-Customer-Services-Interface/src/main/java/com/ibm/cics/cip/bank/springboot/customerservices/controllers/WebController.java`:9, 137-153 (AccountEnquiryForm.java:6-16) | S |
| CBSA-C-03 | low | No explicit request-size policy; oversized bodies are handled only by Tomcat defaults | `src/Z-OS-Connect-Payment-Interface/src/main/resources/application.properties`:1-2 | S |

### D. Error handling

| id | severity | title | file:lines | effort |
|---|---|---|---|---|
| CBSA-D-01 | high | Backend connection failure is converted into HTTP 200 with an empty body | `src/Z-OS-Connect-Payment-Interface/src/main/java/com/ibm/cics/cip/bank/springboot/paymentinterface/controllers/ParamsController.java`:53-75 | S |
| CBSA-D-02 | medium | Blank amount on the browser payment form produces an unhandled HTTP 500 (NullPointerException) | `src/Z-OS-Connect-Payment-Interface/src/main/java/com/ibm/cics/cip/bank/springboot/paymentinterface/controllers/WebController.java`:58-76 | S |
| CBSA-D-03 | high | Local transfer applies debit and credit as two independent updates and ignores their results; rollback only covers the audit-record write | `src/webui/src/main/java/com/ibm/cics/cip/bankliberty/api/json/AccountsResource.java`:1077-1117 | M |
| CBSA-D-04 | low | No correlation/request id is returned to callers or written to logs | `src/Z-OS-Connect-Payment-Interface/src/main/java/com/ibm/cics/cip/bank/springboot/paymentinterface/controllers/ParamsController.java`:42-50 | S |

### E. Logging & observability

| id | severity | title | file:lines | effort |
|---|---|---|---|---|
| CBSA-E-01 | high | Application logs contain account numbers, amounts, organisation and the full outbound payment JSON | `src/Z-OS-Connect-Payment-Interface/src/main/java/com/ibm/cics/cip/bank/springboot/paymentinterface/controllers/ParamsController.java`:42-50, 63-67 | M |
| CBSA-E-02 | low | Customer Services module has no health/readiness endpoint and neither module exposes metrics | `src/Z-OS-Connect-Customer-Services-Interface/pom.xml`:1-170 (no actuator dependency) | S |

### F. Security hygiene

| id | severity | title | file:lines | effort |
|---|---|---|---|---|
| CBSA-F-01 | critical | Checked-in z/OS Connect server.xml ships default credentials, HTTP-only endpoint, wildcard CORS with credentials, and requireAuth/requireSecure=false | `etc/install/base/zosconnectserver/server.xml`:16-22, 30-43, 75 | M |
| CBSA-F-02 | high | Spring Boot 3.3.7 dependency tree carries 18 critical and 67 high OSV advisories with fixes available | `src/Z-OS-Connect-Payment-Interface/pom.xml`:10 (and Customer Services pom.xml:11) | M |
| CBSA-F-03 | low | No security response headers on the Spring Boot interfaces | `src/Z-OS-Connect-Payment-Interface/src/main/resources/application.properties`:1-2 | S |
| CBSA-F-04 | low | Payment WAR bundles spring-boot-admin-server 3.2.3 that is never enabled | `src/Z-OS-Connect-Payment-Interface/pom.xml`:126-131 | S |

### G. Lifecycle

| id | severity | title | file:lines | effort |
|---|---|---|---|---|
| CBSA-G-01 | medium | No CI quality gates, contract tests, spec linting, changelog or automated deploy/rollback for the HTTP surfaces | `etc/install/springBootUI/doc/CBSA_Deploying_the_Payment_Customer_Services_Springboot_apps.md`:10-24 | M |

## Detailed findings

### CBSA-A-01 — No machine-readable API spec for the Liberty JAX-RS JSON API or the Spring Boot interfaces; Swagger 2.0 exists only for the 10 z/OS Connect APIs

**Severity:** medium · **Effort:** M · **File:** `src/zosconnect_artefacts/apis/*/api-docs/swagger.json` lines basePath/paths of the 10 API definitions

**Evidence.** `ls src/zosconnect_artefacts/apis/` lists exactly 10 API projects (creacc, crecust, delacc, delcus, inqacccz, inqaccz, inqcustz, makepayment, updacc, updcust), each with a Swagger 2.0 `api-docs/swagger.json`. No OpenAPI/Swagger document, `@Operation`/`@OpenAPIDefinition` annotation, or springdoc/MicroProfile OpenAPI dependency exists for the 34 JAX-RS routes under `src/webui/src/main/java/com/ibm/cics/cip/bankliberty/api/json/` (class roots `/account`, `/customer`, `/counter`, `/processedTransaction`, `/companyName`, `/sortCode`) or the 21 Spring MVC routes in the two z/OS Connect interface modules (`find . -iname '*openapi*' -o -iname 'swagger*'` returns only the zosconnect_artefacts files). Route-vs-spec diff: the JAX-RS API is therefore 100% unspecified; the 10 z/OS Connect specs match the service archives in `src/zosconnect_artefacts/services/` one-to-one.

**Remediation.** Generate and commit an OpenAPI 3 document for the Liberty JSON API with MicroProfile OpenAPI annotations (Liberty `mpOpenAPI` feature) and for the Spring modules with springdoc-openapi; lint all specs with Spectral in CI and fail the build when mounted routes and spec paths diverge (e.g. springdoc + `openapi-diff` in a contract-test stage).

### CBSA-A-02 — Money-moving POST/PUT endpoints have no idempotency mechanism

**Severity:** high · **Effort:** M · **File:** `src/Z-OS-Connect-Payment-Interface/src/main/java/com/ibm/cics/cip/bank/springboot/paymentinterface/controllers/ParamsController.java` lines 35-40 (also src/Z-OS-Connect-Payment-Interface/src/main/java/com/ibm/cics/cip/bank/springboot/paymentinterface/controllers/WebController.java:58-76, src/webui/src/main/java/com/ibm/cics/cip/bankliberty/api/json/AccountsResource.java:790,856,923)

**Evidence.** `grep -rni idempot src --include=*.java` returns no matches. `POST /paymentinterface-1.1/submit` and `POST /paymentinterface-1.1/paydbcr` forward a PAYDBCR debit/credit to z/OS Connect `PUT /makepayment/dbcr` (COBOL DBCRFUN) and the JAX-RS `PUT /account/debit/{id}`, `/account/credit/{id}` and `/account/transfer/{id}` (AccountsResource lines 790, 856, 923) apply balance changes directly; none reads an `Idempotency-Key` header or any client request id. Runtime: repeated anonymous `curl -s -i -X POST 'http://localhost:19080/paymentinterface-1.1/submit?acctnum=12345678&amount=10.50&organisation=ACME'` requests were each accepted with HTTP 200 and each produced a new outbound backend attempt in the log; no header is required to distinguish retries from new payments.

**Remediation.** Require an `Idempotency-Key` header on every money-moving POST/PUT, persist key + request hash + response in a keyed store (Db2 table or Redis) and replay the stored response on duplicates; propagate the key to the z/OS Connect/CICS call as the PROCTRAN reference so retries are detectable end to end.

### CBSA-A-03 — API version is hard-coded in the servlet context path and bumped in bulk commits; JAX-RS routes are unversioned

**Severity:** low · **Effort:** S · **File:** `src/Z-OS-Connect-Payment-Interface/src/main/resources/application.properties` lines 1-2 (and Customer Services application.properties:1-2)

**Evidence.** `server.servlet.context-path=/paymentinterface-1.1` and `/customerservices-1.0` are the only versioning mechanism; the Liberty JAX-RS paths (`/account`, `/customer`, ...) carry no version. `git log -S"paymentinterface-1.1" -- src/Z-OS-Connect-Payment-Interface` shows the 1.0 to 1.1 bump landed in commit 840b982 (2024-07-23, "Update to source") with no changelog or deprecation note. The deployment guide (etc/install/springBootUI/doc) requires editing `ConnectionInfo.java` source to change the backend host/port.

**Remediation.** Adopt URI-path versioning (`/api/v1/...`) for the JAX-RS and Spring APIs, document a deprecation policy (Sunset/Deprecation headers) and record route changes in a CHANGELOG; externalise host/port with Spring `@ConfigurationProperties` instead of source edits.

### CBSA-B-01 — Spring Boot Payment and Customer Services interfaces expose state-changing routes with no authentication

**Severity:** high · **Effort:** M · **File:** `src/Z-OS-Connect-Payment-Interface/src/main/java/com/ibm/cics/cip/bank/springboot/paymentinterface/controllers/ParamsController.java` lines 22-76

**Evidence.** Neither module declares Spring Security (no `spring-boot-starter-security` in either pom.xml, no `SecurityFilterChain`, no `@PreAuthorize`). Runtime transcript: `curl -s -i -X POST 'http://localhost:19080/paymentinterface-1.1/submit?acctnum=12345678&amount=10.50&organisation=ACME'` returned `HTTP/1.1 200` (empty body, see CBSA-D-01) without any credential; `curl -s -o /dev/null -w '%{http_code}' -X POST http://localhost:19081/customerservices-1.0/delcust --data-urlencode 'custNumber=1'` and `/createcust`, `/updateacc` likewise returned 200 anonymously and attempted the backend call. Actual ledger mutation could not be confirmed because the z/OS Connect backend host `winmvsb0.hursley.ibm.com` does not resolve from the VM.

**Remediation.** Add Spring Security with OAuth 2.0 resource-server JWT validation (or mTLS for service clients), deny-all by default, and require a dedicated scope/role for payment and customer-mutation routes; pass the caller identity to z/OS Connect instead of a shared server credential.

### CBSA-B-02 — Liberty JAX-RS JSON API declares no security constraints, disables XSRF protection, and authorises nothing at object level

**Severity:** high · **Effort:** L · **File:** `src/webui/WebContent/WEB-INF/web.xml` lines 9-12

**Evidence.** web.xml contains only `disable-xsrf-protection=true` and `syncId=false`; there is no `<security-constraint>`, `<login-config>` or `<security-role>`, no `@RolesAllowed`/`@DenyAll` and no `SecurityContext` use anywhere under `src/webui/src/main/java/com/ibm/cics/cip/bankliberty/api/json/` (grep returns nothing). All 34 routes, including `PUT /account/debit/{id}`, `PUT /account/credit/{id}`, `PUT /account/transfer/{id}`, `DELETE /account/{accountNumber}`, `DELETE /customer/{id}`, `POST/DELETE /counter/*` and the six `POST /processedTransaction/*` audit-record writers, select the target purely from the path/body identifier. Code-level only: the WAR cannot run on the VM (needs CICS, Db2, VSAM); whether the CICS Liberty JVM server enforces SAF `appSecurity` in front of it is listed under needs_confirmation.

**Remediation.** Enable Liberty `appSecurity-5.0` with SAF/LDAP registry and `<security-constraint>` per route group, annotate resources with `@RolesAllowed`, and implement object-level checks (caller may only act on accounts owned by their customer id) in a shared filter; move `/processedTransaction` and `/counter` writers behind an internal-only role.

### CBSA-C-01 — Payment API binds amounts as float and accepts unbounded or non-finite values and oversize identifiers

**Severity:** high · **Effort:** M · **File:** `src/Z-OS-Connect-Payment-Interface/src/main/java/com/ibm/cics/cip/bank/springboot/paymentinterface/controllers/ParamsController.java` lines 35-40

**Evidence.** `@RequestParam("amount") float amount` with no Bean Validation. Runtime: `curl -s -i -X POST 'http://localhost:19080/paymentinterface-1.1/submit?acctnum=123456789012345678901234567890&amount=-99999999999&organisation=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'` returned HTTP 200 and the log recorded `Amount -9.9999998E10` (float rounding visible). `amount=NaN` was also accepted and serialised into the outbound PAYDBCR JSON as `"COMM_AMT":"NaN"`. `TransferForm.amount` in the browser path is a boxed `Float` with no range constraint.

**Remediation.** Replace float with `BigDecimal` (or integer minor units) in a validated request DTO using Jakarta Bean Validation (`@NotNull @Digits(integer=10,fraction=2) @DecimalMin("0.01")`), bound `acctnum`/`organisation` with `@Pattern`/`@Size`, and reject non-finite input before building the z/OS Connect request.

### CBSA-C-02 — Bean Validation annotations are silently ignored (javax.validation on Spring Boot 3), so unvalidated identifiers are concatenated into the backend URL path

**Severity:** high · **Effort:** S · **File:** `src/Z-OS-Connect-Customer-Services-Interface/src/main/java/com/ibm/cics/cip/bank/springboot/customerservices/controllers/WebController.java` lines 9, 137-153 (AccountEnquiryForm.java:6-16)

**Evidence.** `WebController` imports `javax.validation.Valid` (line 9) and `AccountEnquiryForm` declares `@NotNull @Size(max = 8)` from `javax.validation.constraints` (lines 6-16), but Spring Boot 3.3.7 only honours `jakarta.validation`; no `spring-boot-starter-validation` dependency is present. Line 152-153 builds `ConnectionInfo.getAddressAndPort() + "/inqaccz/enquiry/" + accountEnquiryForm.getAcctNumber()` by string concatenation. Runtime: `curl -s -o /dev/null -w '%{http_code}' -X POST http://localhost:19081/customerservices-1.0/enqacct --data-urlencode 'acctNumber=../../zosConnect/apis'` -> 200 and `--data-urlencode 'acctNumber=1234567890123456789012345678901234567890'` -> 200; the WebClientRequestException count in the application log rose from 5 to 7, proving both values passed validation and reached the outbound WebClient. The same pattern is used for `/delcus/remove/`, `/inqcustz/enquiry/` and `/inqacccz/list/`.

**Remediation.** Add `spring-boot-starter-validation`, migrate all constraint imports to `jakarta.validation`, use `@Pattern(regexp="\\d{1,8}")` on identifiers, and build backend URLs with `UriBuilder`/`WebClient.uri("/inqaccz/enquiry/{acc}", acc)` so path segments are encoded rather than concatenated.

### CBSA-C-03 — No explicit request-size policy; oversized bodies are handled only by Tomcat defaults

**Severity:** low · **Effort:** S · **File:** `src/Z-OS-Connect-Payment-Interface/src/main/resources/application.properties` lines 1-2

**Evidence.** Both application.properties files set only `server.port` and `server.servlet.context-path`. Runtime: posting a 3 MB form body to `/paymentinterface-1.1/submit` returned HTTP 400 after 1,835,008 bytes were uploaded (Tomcat's default `max-swallow-size`/parameter limits), not a configured 413.

**Remediation.** Set `server.tomcat.max-http-form-post-size`, `server.tomcat.max-swallow-size` and `spring.servlet.multipart.max-request-size` explicitly, enforce the same limit at the ingress/WAF, and return RFC 9457 `413` problem+json.

### CBSA-D-01 — Backend connection failure is converted into HTTP 200 with an empty body

**Severity:** high · **Effort:** S · **File:** `src/Z-OS-Connect-Payment-Interface/src/main/java/com/ibm/cics/cip/bank/springboot/paymentinterface/controllers/ParamsController.java` lines 53-75

**Evidence.** The handler catches `Exception`, logs it at INFO and `return null`. Runtime: `curl -s -i -X POST 'http://localhost:19080/paymentinterface-1.1/submit?acctnum=12345678&amount=10.50&organisation=ACME'` -> `HTTP/1.1 200`, `Content-Length: 0`; application log: `WebClientRequestException: Failed to resolve 'winmvsb0.hursley.ibm.com'`. Customer Services `WebController` uses the same pattern (catch Exception -> log -> render page with HTTP 200) for every POST route.

**Remediation.** Introduce a `@RestControllerAdvice` mapping `WebClientRequestException`/`WebClientResponseException` to 502/504 RFC 9457 `application/problem+json` responses with a correlation id, and never return null from a handler.

### CBSA-D-02 — Blank amount on the browser payment form produces an unhandled HTTP 500 (NullPointerException)

**Severity:** medium · **Effort:** S · **File:** `src/Z-OS-Connect-Payment-Interface/src/main/java/com/ibm/cics/cip/bank/springboot/paymentinterface/controllers/WebController.java` lines 58-76

**Evidence.** `TransferForm.amount` is a nullable `Float`; after `@Valid` (javax, ignored) the controller calls `transferForm.getAmount().floatValue()` indirectly via `PaymentInterfaceJson`. Runtime: `curl -s -i -X POST http://localhost:19080/paymentinterface-1.1/paydbcr --data-urlencode 'acctNumber=1' --data-urlencode 'amount=' --data-urlencode 'organisation=ACME'` -> `HTTP/1.1 500` with the default Spring error envelope `{"timestamp":...,"status":500,"error":"Internal Server Error","path":"/paymentinterface-1.1/paydbcr"}`; log: `java.lang.NullPointerException: Cannot invoke "java.lang.Float.floatValue()" because the return value of ...TransferForm.getAmount() is null`.

**Remediation.** Fix validation activation (see CBSA-C-02), check `BindingResult` before use, and add a global exception handler so unexpected exceptions return a stable problem-details envelope.

### CBSA-D-03 — Local transfer applies debit and credit as two independent updates and ignores their results; rollback only covers the audit-record write

**Severity:** high · **Effort:** M · **File:** `src/webui/src/main/java/com/ibm/cics/cip/bankliberty/api/json/AccountsResource.java` lines 1077-1117

**Evidence.** `transferLocalInternal` calls `db2Account.debitCredit(negativeAmount)` (line 1081), swaps the account number (1083) and calls `db2Account.debitCredit(amount)` (1085) without checking either boolean return value; `Task.getTask().rollback()` is invoked only when the subsequent PROCTRAN write fails (lines 1098-1107). A failed or rejected credit after a successful debit therefore leaves the debit committed at the end of the CICS unit of work. Code-level only: the Liberty WAR needs CICS/Db2 and could not be run on the VM.

**Remediation.** Check both `debitCredit` results and roll back the CICS unit of work (`Task.getTask().rollback()`) on any failure, or perform the transfer as a single SQL transaction/COBOL program (DBCRFUN-style) with an explicit balance-check-and-update; add a contract test for the debit-ok/credit-fail path.

### CBSA-D-04 — No correlation/request id is returned to callers or written to logs

**Severity:** low · **Effort:** S · **File:** `src/Z-OS-Connect-Payment-Interface/src/main/java/com/ibm/cics/cip/bank/springboot/paymentinterface/controllers/ParamsController.java` lines 42-50

**Evidence.** Runtime: `curl -s -i http://localhost:19080/paymentinterface-1.1/does-not-exist` returned the default envelope `{"timestamp","status":404,"error","path"}` and no `X-Request-Id`/`traceparent` header; application log lines carry thread name only (`[io-19080-exec-5]`). No Micrometer Tracing/Sleuth dependency or MDC usage exists in either module.

**Remediation.** Add `micrometer-tracing-bridge-otel` (or a servlet filter generating `X-Request-Id`), put the id in the logback pattern via MDC, and echo it in the problem+json envelope.

### CBSA-E-01 — Application logs contain account numbers, amounts, organisation and the full outbound payment JSON

**Severity:** high · **Effort:** M · **File:** `src/Z-OS-Connect-Payment-Interface/src/main/java/com/ibm/cics/cip/bank/springboot/paymentinterface/controllers/ParamsController.java` lines 42-50, 63-67

**Evidence.** Runtime log (INFO level, default pattern): `AcctNumber: 12345678, Amount 10.5, Organisation ACME` followed by the serialised PAYDBCR object containing `COMM_ACCNO` and `COMM_AMT`. Customer Services logs `Json to be sent:` plus the full `CreateCustomerJson` including name, address and date of birth (`WebController` lines 500-520). Logging is unstructured printf-style (logback default pattern, `logger.info(obj.toString())`).

**Remediation.** Switch to structured JSON logging (logstash-logback-encoder), log a masked account reference and correlation id only, and add a log-redaction unit test; define the org data-classification list of fields that must never be logged.

### CBSA-E-02 — Customer Services module has no health/readiness endpoint and neither module exposes metrics

**Severity:** low · **Effort:** S · **File:** `src/Z-OS-Connect-Customer-Services-Interface/pom.xml` lines 1-170 (no actuator dependency)

**Evidence.** Runtime: `curl -s -o /dev/null -w '%{http_code}' http://localhost:19081/customerservices-1.0/actuator/health` -> 404. Payment exposes `/actuator` with only `health` (`{"status":"UP"}`) via the transitively included actuator; no `prometheus`/`metrics` endpoint is enabled and `management.endpoints.*` is unset in both modules. 5xx responses are only detectable from free-text ERROR stack traces.

**Remediation.** Add `spring-boot-starter-actuator` to both modules, expose `health,info,prometheus` with liveness/readiness groups, and alert on the `http_server_requests_seconds_count{status=~"5.."}` metric.

### CBSA-F-01 — Checked-in z/OS Connect server.xml ships default credentials, HTTP-only endpoint, wildcard CORS with credentials, and requireAuth/requireSecure=false

**Severity:** critical · **Effort:** M · **File:** `etc/install/base/zosconnectserver/server.xml` lines 16-22, 30-43, 75

**Evidence.** `<user name="ibmuser" password="SYS1" />` in a `basicRegistry` (lines 16-22), `<httpEndpoint ... httpPort="30701">` (30-43), `<cors allowedOrigins="*" allowCredentials="true" ...>` and `<zosconnect_zosConnectManager requireAuth="false" requireSecure="false" ...>` (line 75). These are the APIs that the Spring interfaces call for every customer/account/payment operation (DBCRFUN, CREACC, DELCUS, ...). Code-level: the z/OS Connect server cannot run on the VM.

**Remediation.** Remove credentials from source control (SAF registry or Liberty `<variable>` injected from a secret store), set `requireAuth="true" requireSecure="true"`, serve only HTTPS with a keystore, restrict `allowedOrigins` to the two UI hosts, and rotate `ibmuser`.

### CBSA-F-02 — Spring Boot 3.3.7 dependency tree carries 18 critical and 67 high OSV advisories with fixes available

**Severity:** high · **Effort:** M · **File:** `src/Z-OS-Connect-Payment-Interface/pom.xml` lines 10 (and Customer Services pom.xml:11)

**Evidence.** Resolved trees (`mvn dependency:list`) were queried against api.osv.dev (query + per-vuln severity/fix lookup). 85 HIGH/CRITICAL advisories with a fixed version across 29 artifacts, e.g. `org.apache.tomcat.embed:tomcat-embed-core` 10.1.34 (7 critical incl. CVE-2025-24813, 12 high), `org.thymeleaf:thymeleaf` 3.1.2/3.1.3 and `thymeleaf-spring6` 3.1.3 (3 critical each, fixed 3.1.5.RELEASE), `io.netty:netty-handler` 4.1.116.Final (1 critical, fixed 4.1.137.Final), `org.springframework:spring-webmvc` 6.1.16 (1 critical CVE-2026-47884). Full table in the report appendix; `de.codecentric:spring-boot-admin-server` 3.2.3 (Payment pom line 128) pulls the extra netty-codec-* artifacts.

**Remediation.** Upgrade the parent to the current Spring Boot 3.5.x line (brings Tomcat 10.1.5x, Netty 4.1.13x, Framework 6.2.x), bump `thymeleaf` to 3.1.5.RELEASE, drop the unused `spring-boot-admin-server`, and gate CI with `osv-scanner`/Dependabot on high+critical.

### CBSA-F-03 — No security response headers on the Spring Boot interfaces

**Severity:** low · **Effort:** S · **File:** `src/Z-OS-Connect-Payment-Interface/src/main/resources/application.properties` lines 1-2

**Evidence.** Runtime: `curl -s -D - -o /dev/null http://localhost:19080/paymentinterface-1.1/` returned only Content-Type/Content-Language/Transfer-Encoding/Date; no `X-Content-Type-Options`, `Content-Security-Policy`, `Strict-Transport-Security`, `Referrer-Policy` or `Cache-Control`. (Positive: an `OPTIONS /submit` preflight from `Origin: https://evil.example` was rejected with 403, i.e. no permissive CORS.)

**Remediation.** Add Spring Security's headers DSL (or a `OncePerRequestFilter`) emitting nosniff, CSP, HSTS, Referrer-Policy and `Cache-Control: no-store` for API responses.

### CBSA-F-04 — Payment WAR bundles spring-boot-admin-server 3.2.3 that is never enabled

**Severity:** low · **Effort:** S · **File:** `src/Z-OS-Connect-Payment-Interface/pom.xml` lines 126-131

**Evidence.** `de.codecentric:spring-boot-admin-server` is declared at compile scope; `grep -rn EnableAdminServer src/` finds no usage, yet `unzip -l target/paymentinterface-1.1.war` contains `spring-boot-admin-server-3.2.3.jar`. It enlarges the attack surface (extra netty codecs, httpcore5 with high advisories, see CBSA-F-02) for no function.

**Remediation.** Remove the dependency; if an admin console is wanted, run it as a separate secured service with `spring-boot-admin-starter-client` only.

### CBSA-G-01 — No CI quality gates, contract tests, spec linting, changelog or automated deploy/rollback for the HTTP surfaces

**Severity:** medium · **Effort:** M · **File:** `etc/install/springBootUI/doc/CBSA_Deploying_the_Payment_Customer_Services_Springboot_apps.md` lines 10-24

**Evidence.** The repository has no `.github/` directory, no `Jenkinsfile`/`.gitlab-ci.yml`, no `CHANGELOG*`, and no test sources in either Spring module (`src/test` absent); the deployment guide is a manual procedure (edit `ConnectionInfo.java`, `mvn package`, copy WAR to the Liberty `apps` directory, restart the JVM server) with no rollback step. No `@Deprecated` or `Deprecation`/`Sunset` header use exists in the route classes.

**Remediation.** Add a GitHub Actions workflow running `mvn verify` with Spring MockMvc contract tests, Spectral lint of the OpenAPI documents, `osv-scanner`, and a versioned WAR artifact publish; document rollback as redeploying the previous artifact version.

## Positives

- Both Spring Boot modules build cleanly with `mvn -q -DskipTests package` (Maven 3.6.3, JDK 17) and start without a backend, which made local probing possible.
- Swagger 2.0 definitions exist for all 10 z/OS Connect APIs under `src/zosconnect_artefacts/apis/*/api-docs/swagger.json` and map one-to-one to service archives and COBOL programs (CREACC, CRECUST, DELACC, DELCUS, INQACCCU, INQACC, INQCUST, DBCRFUN, UPDACC, UPDCUST).
- The Spring MVC CORS default is deny: an `OPTIONS /paymentinterface-1.1/submit` preflight from a foreign origin returned 403.
- The JAX-RS transfer path validates business rules before writing: rejects same-account transfers (400), non-positive amounts (400), unknown source/target accounts (404), and calls `Task.getTask().rollback()` when the PROCTRAN audit write fails.
- Db2 access in the Liberty API uses `PreparedStatement` with bound parameters for the inspected account/customer queries, limiting SQL-injection surface.
- Request models in the Spring modules already carry Bean Validation annotations (`@NotNull`, `@Size`); only the javax to jakarta migration is missing to make them effective.
- Default Spring error envelope does not leak stack traces (`server.error.include-stacktrace` default `never`); 404/500 bodies contain only timestamp/status/error/path.
- The Payment module exposes `/actuator/health` returning `{"status":"UP"}` and the actuator index lists only `health`.

## Needs confirmation

- Liberty JAX-RS WAR (`src/webui`) could not be started: it needs a CICS TS region with JCICS, Db2 and VSAM; CBSA-B-02 and CBSA-D-03 are therefore code-level and the runtime behaviour (actual IDOR, debit-without-credit) is unconfirmed.
- Whether the CICS Liberty JVM server configuration (not in this repo) enables `appSecurity` / SAF and adds `<security-constraint>`s in front of the JSON API.
- Whether deployed z/OS Connect servers override the checked-in `etc/install/base/zosconnectserver/server.xml` (`requireAuth=false`, `ibmuser/SYS1`).
- Whether the Spring Boot UIs are deployed behind an authenticating reverse proxy/WAF in any environment; the repo and deployment guide do not mention one.
- Actual ledger effect of anonymous `/submit` and `/paydbcr` calls (backend host `winmvsb0.hursley.ibm.com` does not resolve from the VM).
- Exploitability of the OSV advisories in CBSA-F-02 in this deployment (e.g. Tomcat DIGEST/FORM authenticator CVEs require those authenticators to be configured); severities are as published by OSV/GHSA, not re-scored.
- Carbon React front-end (`src/bank-application-frontend`) calls into the Liberty JSON API; its own auth/session handling was out of scope and not reviewed.

## Appendix A — dependency advisories (HIGH/CRITICAL with fix, OSV, 2026-10-06)

| artifact | version | module(s) | critical | high | fixed in |
|---|---|---|---|---|---|
| `com.fasterxml.jackson.core:jackson-core` | 2.17.3 | cust+paym | 0 | 3 | 2.18.11, 2.21.4, 2.22.3 |
| `com.fasterxml.jackson.core:jackson-databind` | 2.17.3 | cust+paym | 0 | 5 | 2.21.4, 2.22.2, 2.22.3, 3.1.4 |
| `io.micrometer:micrometer-core` | 1.13.9 | paym | 0 | 1 | 1.15.12 |
| `io.netty:netty-codec` | 4.1.116.Final | cust+paym | 0 | 2 | 4.1.133.Final, 4.1.136.Final |
| `io.netty:netty-codec-dns` | 4.1.116.Final | cust+paym | 0 | 1 | 4.1.133.Final |
| `io.netty:netty-codec-haproxy` | 4.1.116.Final | paym | 0 | 3 | 4.1.135.Final, 4.1.136.Final |
| `io.netty:netty-codec-http` | 4.1.116.Final | cust+paym | 0 | 6 | 4.1.133.Final, 4.1.136.Final, 4.2.10.Final |
| `io.netty:netty-codec-http2` | 4.1.100.Final | cust+paym | 0 | 4 | 4.1.124.Final, 4.1.133.Final, 4.1.136.Final, 4.2.11.Final |
| `io.netty:netty-codec-redis` | 4.1.116.Final | paym | 0 | 4 | 4.1.135.Final |
| `io.netty:netty-codec-smtp` | 4.1.116.Final | paym | 0 | 1 | 4.1.128.Final |
| `io.netty:netty-codec-stomp` | 4.1.116.Final | paym | 0 | 1 | 4.1.136.Final |
| `io.netty:netty-codec-xml` | 4.1.116.Final | paym | 0 | 2 | 4.1.136.Final |
| `io.netty:netty-handler` | 4.1.116.Final | cust+paym | 1 | 4 | 4.1.118.Final, 4.1.135.Final, 4.1.137.Final |
| `io.netty:netty-handler-ssl-ocsp` | 4.1.116.Final | paym | 0 | 3 | 4.1.136.Final |
| `io.netty:netty-resolver-dns` | 4.1.116.Final | cust+paym | 0 | 2 | 4.1.135.Final |
| `io.netty:netty-transport-sctp` | 4.1.116.Final | paym | 0 | 2 | 4.1.135.Final, 4.1.137.Final |
| `org.apache.httpcomponents.core5:httpcore5` | 5.2.5 | paym | 0 | 1 | 5.5-beta2 |
| `org.apache.httpcomponents.core5:httpcore5-h2` | 5.2.5 | paym | 0 | 1 | 5.5-beta2 |
| `org.apache.tomcat.embed:tomcat-embed-core` | 10.1.34 | cust+paym | 7 | 12 | 11.0.20, 11.0.21, 11.0.22, 11.0.9, 9.0.106, 9.0.107, 9.0.108, 9.0.109, 9.0.115, 9.0.121, 9.0.99 |
| `org.springframework.boot:spring-boot` | 3.3.7 | cust+paym | 0 | 2 | 3.4.5, 3.5.14 |
| `org.springframework.boot:spring-boot-devtools` | 3.3.7 | cust | 0 | 1 | 3.5.14 |
| `org.springframework.boot:spring-boot-starter-actuator` | 3.3.7 | paym | 0 | 1 | 3.5.12 |
| `org.springframework:spring-core` | 6.1.16 | cust+paym | 0 | 1 | 6.2.11 |
| `org.springframework:spring-expression` | 6.1.16 | cust+paym | 0 | 1 | 6.2.19 |
| `org.springframework:spring-webflux` | 6.1.16 | cust+paym | 0 | 1 | 6.2.19 |
| `org.springframework:spring-webmvc` | 6.1.16 | cust+paym | 1 | 2 | 6.2.19, 7.0.9 |
| `org.thymeleaf:thymeleaf` | 3.1.2.RELEASE | paym | 3 | 0 | 3.1.4.RELEASE, 3.1.5.RELEASE |
| `org.thymeleaf:thymeleaf` | 3.1.3.RELEASE | cust | 3 | 0 | 3.1.4.RELEASE, 3.1.5.RELEASE |
| `org.thymeleaf:thymeleaf-spring6` | 3.1.3.RELEASE | cust+paym | 3 | 0 | 3.1.4.RELEASE, 3.1.5.RELEASE |

## Appendix B — commands and tool versions

```
git clone https://github.com/alex-baker-cdm/cics-banking-sample-application-cbsa && git checkout main   # 6014f74
sudo apt-get install -y maven                      # ./mvnw wrapper download hit HTTP 429 from repo.maven.apache.org
(cd src/Z-OS-Connect-Payment-Interface && mvn -q -DskipTests package && mvn dependency:list -DoutputFile=/tmp/pay-deps.txt)
(cd src/Z-OS-Connect-Customer-Services-Interface && mvn -q -DskipTests package && mvn dependency:list -DoutputFile=/tmp/cust-deps.txt)
java -jar src/Z-OS-Connect-Payment-Interface/target/paymentinterface-1.1.war            # :19080
java -jar src/Z-OS-Connect-Customer-Services-Interface/target/customerservices-1.0.war  # :19081
curl -s -i -X POST 'http://localhost:19080/paymentinterface-1.1/submit?acctnum=12345678&amount=10.50&organisation=ACME'
curl -s -i -X POST 'http://localhost:19080/paymentinterface-1.1/submit?acctnum=123456789012345678901234567890&amount=-99999999999&organisation=AAAA...'
curl -s -i -X POST 'http://localhost:19080/paymentinterface-1.1/submit?acctnum=1&amount=NaN&organisation=X'
curl -s -i -X POST http://localhost:19080/paymentinterface-1.1/paydbcr --data-urlencode 'acctNumber=1' --data-urlencode 'amount=' --data-urlencode 'organisation=ACME'
head -c 3000000 /dev/zero | tr '\0' 'a' | curl -s -i -X POST http://localhost:19080/paymentinterface-1.1/submit --data-binary @-
curl -s -i http://localhost:19080/paymentinterface-1.1/does-not-exist
curl -s -i http://localhost:19080/paymentinterface-1.1/actuator ; curl -s -i http://localhost:19080/paymentinterface-1.1/actuator/health
curl -s -o /dev/null -w '%{http_code}' http://localhost:19081/customerservices-1.0/actuator/health
curl -s -D - -o /dev/null http://localhost:19080/paymentinterface-1.1/
curl -s -D - -o /dev/null -X OPTIONS http://localhost:19080/paymentinterface-1.1/submit -H 'Origin: https://evil.example' -H 'Access-Control-Request-Method: POST'
curl -s -o /dev/null -w '%{http_code}' -X POST http://localhost:19081/customerservices-1.0/enqacct --data-urlencode 'acctNumber=../../zosConnect/apis'
curl -s -o /dev/null -w '%{http_code}' -X POST http://localhost:19081/customerservices-1.0/enqacct --data-urlencode 'acctNumber=1234567890123456789012345678901234567890'
curl -s -o /dev/null -w '%{http_code}' -X POST http://localhost:19081/customerservices-1.0/delcust --data-urlencode 'custNumber=1'
python3 osv_query.py /tmp/pay-deps.txt /tmp/cust-deps.txt   # api.osv.dev/v1/querybatch + /v1/vulns/{id}
grep -rni idempot src --include=*.java ; grep -rn -E 'security-constraint|RolesAllowed|SecurityContext' src/webui
git log --format='%h %ad %s' --date=short -S'paymentinterface-1.1' -- src/Z-OS-Connect-Payment-Interface

Tool versions: openjdk 17.0.13, Apache Maven 3.6.3, curl 7.81.0, git 2.x, Python 3.10, OSV API (api.osv.dev) queried 2026-10-06.
No application code, configuration or dependency files were modified; no temporary patches were needed.
```
