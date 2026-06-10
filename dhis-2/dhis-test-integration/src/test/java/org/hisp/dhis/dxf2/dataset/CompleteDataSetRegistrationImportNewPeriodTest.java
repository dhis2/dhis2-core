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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
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
 * Regression test for DHIS2-21617.
 *
 * <p>Importing complete data set registrations for a period that does not yet exist in the database
 * must create the period and durably persist the registrations. The registrations are written by
 * the {@code quick} BatchHandler on its own pooled JDBC connection (autoCommit), so the
 * newly-created period must be committed in a separate transaction (see {@link
 * org.hisp.dhis.period.hibernate.HibernatePeriodStore}); otherwise the registration insert hits a
 * foreign key violation / is not persisted while the import still reports success.
 *
 * @author Claude
 */
class CompleteDataSetRegistrationImportNewPeriodTest extends PostgresIntegrationTestBase {

  @Autowired private CompleteDataSetRegistrationExchangeService exchangeService;

  @Autowired private CompleteDataSetRegistrationService registrationService;

  @Autowired private PeriodService periodService;

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private CategoryService categoryService;

  private DataSet dataSetA;

  private OrganisationUnit orgUnitA;

  private CategoryOptionCombo defaultAoc;

  /** A future period that is intentionally NOT added to the database during setup. */
  private static final String NEW_PERIOD_ISO = "209901";

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
  }

  @Test
  void testImportRegistrationForNonExistingPeriodIsPersisted() {
    // Guard: the period must not exist yet, so the import is forced to create it.
    assertNull(
        periodService.getPeriod(NEW_PERIOD_ISO),
        "Test pre-condition: period " + NEW_PERIOD_ISO + " must not exist before the import");

    String json =
        """
        {
          "completeDataSetRegistrations": [
            {
              "dataSet": "%s",
              "period": "%s",
              "organisationUnit": "%s",
              "attributeOptionCombo": "%s",
              "completed": true
            }
          ]
        }"""
            .formatted(
                dataSetA.getUid(), NEW_PERIOD_ISO, orgUnitA.getUid(), defaultAoc.getUid());

    ImportSummary summary =
        exchangeService.saveCompleteDataSetRegistrationsJson(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
            new ImportOptions());

    assertEquals(
        ImportStatus.SUCCESS,
        summary.getStatus(),
        "Import should succeed, conflicts: " + summary.getConflicts());
    assertEquals(1, summary.getImportCount().getImported(), "One registration should be imported");

    // The registration must be durably persisted (this is the actual DHIS2-21617 failure).
    List<CompleteDataSetRegistration> all = registrationService.getAllCompleteDataSetRegistrations();
    assertEquals(
        1, all.size(), "The imported registration must be persisted in completedatasetregistration");

    // The period must have been created as part of the import.
    assertTrue(
        periodService.getPeriod(NEW_PERIOD_ISO) != null,
        "The previously non-existing period should have been created and persisted");
  }
}
