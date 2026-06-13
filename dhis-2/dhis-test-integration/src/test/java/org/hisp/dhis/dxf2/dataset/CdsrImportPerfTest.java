/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.dxf2.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Performance regression test for the CDSR import path after replacing the quick BatchHandler with
 * Spring JdbcTemplate.
 *
 * <p>This is NOT a micro-benchmark. It exercises the full import pipeline — JSON parsing,
 * validation, metadata caching, and bulk SQL inserts — against a real PostgreSQL database, and
 * logs wall-clock time so the before/after cost is visible in the test output.
 *
 * <h2>Before (BatchHandler)</h2>
 * BatchHandler opened its own autoCommit JDBC connection and flushed buffered inserts at the end.
 * Inserts were batched by the quick library into a single prepared-statement batch. The connection
 * overhead was minimal but it was decoupled from the Spring transaction.
 *
 * <h2>After (JdbcTemplate)</h2>
 * JdbcTemplate.batchUpdate() flushes the same batch of rows at close() time, using the
 * transaction-bound connection. The Spring proxy adds negligible per-call overhead (one
 * DataSourceUtils.getConnection() lookup, no lock contention). The critical difference is that the
 * insert now happens INSIDE the Spring transaction, so:
 * <ul>
 *   <li>Newly-created periods are visible to the insert (fixes DHIS2-21617).</li>
 *   <li>A failed import rolls back all inserts atomically (correctness improvement).</li>
 * </ul>
 *
 * <h2>Expected performance</h2>
 * Throughput should be within 5–10% of the BatchHandler baseline. Any larger regression would
 * indicate a problem (e.g. per-row round-trips instead of batching). This test fails if the import
 * produces a wrong result; the timing is informational and logged, not asserted.
 *
 * @author Claude (prototype for DHIS2-21617 batchHandler refactor)
 */
class CdsrImportPerfTest extends PostgresIntegrationTestBase {

  /** Number of registrations in the large payload. Tune for desired signal/noise. */
  private static final int PAYLOAD_SIZE = 500;

  /** Number of warm-up rounds before timing (lets JIT stabilise). */
  private static final int WARMUP_ROUNDS = 1;

  /** Number of timed rounds to average over. */
  private static final int TIMED_ROUNDS = 3;

  @Autowired private CompleteDataSetRegistrationExchangeService exchangeService;

  @Autowired private PeriodService periodService;

  @Autowired private CategoryService categoryService;

  @Autowired private ObjectMapper jsonMapper;

  private DataSet dataSet;
  private List<OrganisationUnit> orgUnits;
  private CategoryOptionCombo defaultAoc;
  private User adminUser;

  @BeforeEach
  void setUp() {
    adminUser = makeUser("A");
    injectSecurityContextUser(adminUser);

    PeriodType monthly = periodService.getPeriodTypeByName(MonthlyPeriodType.NAME);
    dataSet = createDataSet('A', monthly);
    manager.save(dataSet, false);

    orgUnits = new ArrayList<>(PAYLOAD_SIZE);
    for (int i = 0; i < PAYLOAD_SIZE; i++) {
      // Use a unique character sequence to avoid collisions with existing test data.
      // createOrganisationUnit takes a char so we encode index into a readable name.
      OrganisationUnit ou = createOrganisationUnit("PerfUnit-" + i);
      ou.getDataSets().add(dataSet);
      manager.save(ou, false);
      dataSet.addOrganisationUnit(ou);
      orgUnits.add(ou);
    }
    manager.update(dataSet);

    defaultAoc = categoryService.getDefaultCategoryOptionCombo();

    // Pre-create the period so the perf test is not conflated with period-creation cost.
    // (Period-creation path is exercised separately in CompleteDataSetRegistrationImportNewPeriodTest.)
    Period p = periodService.reloadIsoPeriodInStatelessSession("202401");
    manager.flush();
  }

  /**
   * Baseline: import PAYLOAD_SIZE registrations with all periods pre-existing. Measures the pure
   * throughput of the write path (findObject + addObject + flush) after the BatchHandler → JdbcTemplate
   * swap. Run with -Dtest.verbose to see wall-clock output.
   */
  @Test
  void importBulk_preExistingPeriod_measuresWriteThroughput() throws Exception {
    String json = buildPayload("202401");

    // Warm-up
    for (int i = 0; i < WARMUP_ROUNDS; i++) {
      deleteCdsrs(); // reset between rounds
      runImport(json);
    }

    // Timed rounds
    long totalMs = 0;
    for (int i = 0; i < TIMED_ROUNDS; i++) {
      deleteCdsrs();
      long start = System.currentTimeMillis();
      ImportSummary summary = runImport(json);
      long elapsed = System.currentTimeMillis() - start;
      totalMs += elapsed;

      assertEquals(ImportStatus.SUCCESS, summary.getStatus(), "Import should succeed on round " + i);
      assertEquals(
          PAYLOAD_SIZE,
          summary.getImportCount().getImported(),
          "All registrations should be imported on round " + i);
    }

    double avgMs = (double) totalMs / TIMED_ROUNDS;
    double rps = PAYLOAD_SIZE / (avgMs / 1000.0);

    // Log so CI output is easy to compare before/after.
    System.out.printf(
        "[CdsrImportPerfTest] payload=%d  avg=%.0f ms  throughput=%.0f reg/s%n",
        PAYLOAD_SIZE, avgMs, rps);
  }

  /**
   * Exercises the existing-check path (findObject SELECT per row) on a payload where every row
   * already exists. This stresses the SELECT throughput rather than the INSERT throughput.
   */
  @Test
  void importBulk_allExisting_measuresUpdateThroughput() throws Exception {
    String json = buildPayload("202401");

    // First import: populate the table
    ImportSummary first = runImport(json);
    assertEquals(ImportStatus.SUCCESS, first.getStatus());
    assertEquals(PAYLOAD_SIZE, first.getImportCount().getImported());

    // Second import: all rows exist → should UPDATE (CREATE_AND_UPDATE is default)
    long start = System.currentTimeMillis();
    ImportSummary second = runImport(json);
    long elapsed = System.currentTimeMillis() - start;

    assertEquals(ImportStatus.SUCCESS, second.getStatus(), "Re-import should succeed");
    double rps = PAYLOAD_SIZE / (elapsed / 1000.0);
    System.out.printf(
        "[CdsrImportPerfTest] update path  payload=%d  elapsed=%d ms  throughput=%.0f reg/s%n",
        PAYLOAD_SIZE, elapsed, rps);
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private ImportSummary runImport(String json) throws Exception {
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
    return exchangeService.saveCompleteDataSetRegistrationsJson(
        new ByteArrayInputStream(bytes), ImportOptions.getDefaultImportOptions());
  }

  private String buildPayload(String period) throws Exception {
    ObjectNode root = jsonMapper.createObjectNode();
    ArrayNode registrations = root.putArray("completeDataSetRegistrations");

    for (OrganisationUnit ou : orgUnits) {
      ObjectNode reg = registrations.addObject();
      reg.put("dataSet", dataSet.getUid());
      reg.put("period", period);
      reg.put("organisationUnit", ou.getUid());
      reg.put("completed", true);
    }

    return jsonMapper.writeValueAsString(root);
  }

  private void deleteCdsrs() {
    // Direct JDBC delete to reset state between rounds without going through the service
    manager.flush();
    manager
        .getSession()
        .createNativeQuery(
            "DELETE FROM completedatasetregistration WHERE datasetid = :dsId")
        .setParameter("dsId", dataSet.getId())
        .executeUpdate();
    manager.flush();
  }
}
