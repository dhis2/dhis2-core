/**
 * This package contains DHIS2 related test code. Only add code that is reusable across modules.
 *
 * <p>This guide helps you set up your test.
 *
 * <ul>
 *   <li>Do you need a DB to test your code?
 *       <ul>
 *         <li>No: write a unit test.
 *         <li>Yes: move on to the next question.
 *       </ul>
 *   <li>Do you need to test a controller?
 *       <ul>
 *         <li>Yes: create a test class in the maven module {@code dhis-test-web-api} extending one
 *             of {@code org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase} or {@code
 *             org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase} depending on the DB you
 *             want to test against.
 *         <li>No: create a test class in the maven module {@code dhis-test-integration} extending
 *             {@code org.hisp.dhis.test.integration.PostgresIntegrationTestBase}.
 *       </ul>
 *   <li>Do you need to test code running in another thread?
 *       <ul>
 *         <li>Yes: <b>do not</b> annotate your test class with {@link
 *             org.springframework.transaction.annotation.Transactional} so that the changes to the
 *             DB during testing are visible in other threads. {@link
 *             org.hisp.dhis.test.junit.SpringIntegrationTestExtension} will do the DB cleanup.
 *         <li>No: annotate your test class with {@link
 *             org.springframework.transaction.annotation.Transactional} so that the changes to the
 *             DB during testing are rolled back automatically by Spring.
 *       </ul>
 *   <li>Can you setup your test data once? By default JUnit tests have a {@link
 *       org.junit.jupiter.api.TestInstance.Lifecycle} of method meaning every test method will get
 *       their own instance of the test class. You will then not be able to setup DB related data in
 *       a {@link org.junit.jupiter.api.BeforeAll} method as they need to be static.
 *       <ul>
 *         <li>Yes(with caveat): you might need to benchmark this to be certain but if you need the
 *             same data for lots of tests it is likely that doing this is going to speedup the test
 *             duration. Annotate your test class with {@link
 *             org.junit.jupiter.api.TestInstance.Lifecycle} and set it to {@link
 *             org.junit.jupiter.api.TestInstance.Lifecycle#PER_CLASS}.Then setup your data in a
 *             method annotated with {@link org.junit.jupiter.api.BeforeAll}.
 *       </ul>
 * </ul>
 *
 * Read through {@link org.hisp.dhis.test.junit.SpringIntegrationTestExtension} if you have any
 * question about the main test data setup and tear down which is done for every integration test
 * based on {@link org.hisp.dhis.test.IntegrationTestBase}.
 */
package org.hisp.dhis.test;
