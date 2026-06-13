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
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proves the {@code CdsrJdbcWriter} chunked-flush path is correct: importing a payload LARGER than
 * the writer's internal {@code BATCH_SIZE} (1000) forces more than one flush, and every
 * registration must still be persisted exactly once.
 *
 * <p>This closes the only behavioural gap between the JdbcTemplate-based writer and the quick
 * BatchHandler it replaces: the BatchHandler streamed its INSERT buffer every ~200&nbsp;KB, so it
 * never held the whole payload in memory. {@code CdsrJdbcWriter} now flushes every {@code
 * BATCH_SIZE} rows for the same bounded-memory behaviour; this test confirms the multi-batch path
 * produces a correct, complete result (no rows lost or double-inserted at the batch boundary).
 *
 * @author Claude (DHIS2-21617 batchHandler refactor — chunked-flush validation)
 */
class CdsrImportLargePayloadTest extends PostgresIntegrationTestBase {

  /** Deliberately > CdsrJdbcWriter.BATCH_SIZE (1000) so the import spans at least two flushes. */
  private static final int PAYLOAD_SIZE = 1200;

  @Autowired private CompleteDataSetRegistrationExchangeService exchangeService;

  @Autowired private CompleteDataSetRegistrationService registrationService;

  @Autowired private PeriodService periodService;

  @Autowired private CategoryService categoryService;

  @Autowired private IdentifiableObjectManager idObjectManager;

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

    User user = createAndAddUser(true, "cdsrbulk", orgUnitA, "ALL");
    injectSecurityContextUser(user);

    // PAYLOAD_SIZE distinct monthly ISO periods (100 years of months), pre-created so the test
    // exercises the write/flush path rather than period creation.
    periodIsos = new ArrayList<>(PAYLOAD_SIZE);
    int year = 1950;
    int month = 1;
    for (int i = 0; i < PAYLOAD_SIZE; i++) {
      periodIsos.add(String.format("%04d%02d", year, month));
      if (++month > 12) {
        month = 1;
        year++;
      }
      periodService.reloadIsoPeriod(periodIsos.get(i));
    }
  }

  @Test
  void importPayloadLargerThanBatchSize_persistsEveryRegistration() {
    String json = buildPayload();

    ImportSummary summary =
        exchangeService.saveCompleteDataSetRegistrationsJson(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), new ImportOptions());

    assertEquals(
        ImportStatus.SUCCESS, summary.getStatus(), "import failed: " + summary.getConflicts());
    assertEquals(
        PAYLOAD_SIZE,
        summary.getImportCount().getImported(),
        "summary should report every registration imported across all flush batches");

    // The decisive check: every row is durably persisted, none lost/duplicated at a batch boundary.
    List<CompleteDataSetRegistration> all = registrationService.getAllCompleteDataSetRegistrations();
    assertEquals(
        PAYLOAD_SIZE,
        all.size(),
        "all registrations from a multi-batch import must be persisted exactly once");
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
}
