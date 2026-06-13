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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
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
 * Performance comparison harness for the CDSR import write path, run through the REAL import
 * pipeline (full Spring context, Hibernate, Testcontainers PostgreSQL) — not an isolated JDBC
 * micro-benchmark.
 *
 * <p>Same payload is imported on whichever implementation of {@link
 * CompleteDataSetRegistrationExchangeService} is on the classpath:
 *
 * <ul>
 *   <li>on {@code master}: the quick {@code BatchHandler} (multi-row INSERT string, separate
 *       autoCommit connection);
 *   <li>on the refactor branch: {@code CdsrJdbcWriter} backed by {@code JdbcTemplate.batchUpdate}
 *       (PreparedStatement batch on the transaction-bound connection).
 * </ul>
 *
 * <p>The test only uses the public {@code saveCompleteDataSetRegistrationsJson} API, so the exact
 * same test source compiles and runs against both implementations. Run it on each branch and
 * compare the logged wall-clock numbers.
 *
 * <p>Design: one data set, one org unit (kept inside the importing user's hierarchy so every row
 * passes validation), and {@value #PAYLOAD_SIZE} distinct monthly periods pre-created in setUp.
 * Each registration therefore has a unique (dataSet, period, orgUnit, aoc) key — the same shape the
 * BatchHandler/JdbcWriter unique-key logic keys on. Periods are pre-created so this measures the
 * write path (findObject SELECT + INSERT), not period creation (covered separately by
 * CompleteDataSetRegistrationImportNewPeriodTest).
 *
 * @author Claude (DHIS2-21617 batchHandler refactor — perf validation)
 */
class CdsrImportPerfTest extends PostgresIntegrationTestBase {

  /** Registrations per import. 480 distinct monthly periods = 40 years, well within range. */
  private static final int PAYLOAD_SIZE = 480;

  private static final int WARMUP_ROUNDS = 2;
  private static final int TIMED_ROUNDS = 5;

  @Autowired private CompleteDataSetRegistrationExchangeService exchangeService;

  @Autowired private PeriodService periodService;

  @Autowired private CategoryService categoryService;

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private CompleteDataSetRegistrationService registrationService;

  private DataSet dataSetA;
  private OrganisationUnit orgUnitA;
  private CategoryOptionCombo defaultAoc;
  private List<String> periodIsos;

  @BeforeEach
  void setUp() {
    PeriodType monthly = PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY);

    orgUnitA = createOrganisationUnit('A');
    idObjectManager.save(orgUnitA);

    dataSetA = createDataSet('A', monthly);
    dataSetA.addOrganisationUnit(orgUnitA);
    idObjectManager.save(dataSetA);

    defaultAoc = categoryService.getDefaultCategoryOptionCombo();

    User user = createAndAddUser(true, "cdsruser", orgUnitA, "ALL");
    injectSecurityContextUser(user);

    // Generate PAYLOAD_SIZE distinct monthly ISO periods (e.g. 199001, 199002, ...) and
    // pre-create them so the timed import measures the write path, not period creation.
    periodIsos = new ArrayList<>(PAYLOAD_SIZE);
    int year = 1990;
    int month = 1;
    for (int i = 0; i < PAYLOAD_SIZE; i++) {
      periodIsos.add(String.format("%04d%02d", year, month));
      if (++month > 12) {
        month = 1;
        year++;
      }
    }
    for (String iso : periodIsos) {
      periodService.reloadIsoPeriod(iso);
    }
  }

  @Test
  void importBulkRegistrations_measuresThroughput() {
    String json = buildPayload();

    for (int i = 0; i < WARMUP_ROUNDS; i++) {
      deleteAllRegistrations();
      ImportSummary s = runImport(json);
      assertEquals(
          ImportStatus.SUCCESS, s.getStatus(), "warmup import failed: " + s.getConflicts());
    }

    long totalMs = 0;
    long minMs = Long.MAX_VALUE;
    for (int i = 0; i < TIMED_ROUNDS; i++) {
      deleteAllRegistrations();
      long start = System.currentTimeMillis();
      ImportSummary summary = runImport(json);
      long elapsed = System.currentTimeMillis() - start;
      totalMs += elapsed;
      minMs = Math.min(minMs, elapsed);

      assertEquals(
          ImportStatus.SUCCESS, summary.getStatus(), "import failed: " + summary.getConflicts());
      assertEquals(
          PAYLOAD_SIZE,
          summary.getImportCount().getImported(),
          "all registrations should import on round " + i);
    }

    double avgMs = (double) totalMs / TIMED_ROUNDS;
    double rps = PAYLOAD_SIZE / (avgMs / 1000.0);
    System.out.printf(
        "%n[CdsrImportPerfTest] payload=%d  avg=%.0fms  min=%dms  throughput=%.0f reg/s%n%n",
        PAYLOAD_SIZE, avgMs, minMs, rps);
  }

  private ImportSummary runImport(String json) {
    return exchangeService.saveCompleteDataSetRegistrationsJson(
        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), new ImportOptions());
  }

  private String buildPayload() {
    StringBuilder sb = new StringBuilder("{\"completeDataSetRegistrations\":[");
    for (int i = 0; i < periodIsos.size(); i++) {
      if (i > 0) sb.append(',');
      sb.append("{\"dataSet\":\"")
          .append(dataSetA.getUid())
          .append("\",\"period\":\"")
          .append(periodIsos.get(i))
          .append("\",\"organisationUnit\":\"")
          .append(orgUnitA.getUid())
          .append("\",\"attributeOptionCombo\":\"")
          .append(defaultAoc.getUid())
          .append("\",\"completed\":true}");
    }
    sb.append("]}");
    return sb.toString();
  }

  private void deleteAllRegistrations() {
    // Reset between rounds via the transactional service method (the test method itself is
    // not transactional, so a raw entityManager update would fail with TransactionRequired).
    registrationService.deleteCompleteDataSetRegistrations(dataSetA);
  }
}
