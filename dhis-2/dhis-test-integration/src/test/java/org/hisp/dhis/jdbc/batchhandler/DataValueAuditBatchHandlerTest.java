/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.jdbc.batchhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.hisp.dhis.audit.AuditOperationType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditService;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.quick.BatchHandler;
import org.hisp.quick.BatchHandlerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class DataValueAuditBatchHandlerTest extends PostgresIntegrationTestBase {

  @Autowired private BatchHandlerFactory batchHandlerFactory;

  @Autowired private DataValueService dataValueService;

  @Autowired private DataValueAuditService auditService;

  @Autowired private DataElementService dataElementService;

  @Autowired private PeriodService periodService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private CategoryService categoryService;

  private DataElement dataElementA;

  private CategoryOptionCombo categoryOptionComboA;

  private PeriodType periodTypeA;

  private Period periodA;

  private Period periodB;

  private OrganisationUnit unitA;

  private OrganisationUnit unitB;

  private DataValue dataValueA;

  private DataValue dataValueB;

  private DataValueAudit auditA;

  private DataValueAudit auditB;

  private DataValueAudit auditC;

  private DataValueAudit auditD;

  private String storedBy = "johndoe";

  @BeforeAll
  void setUp() {
    dataElementA = createDataElement('A');
    dataElementService.addDataElement(dataElementA);
    categoryOptionComboA = categoryService.getDefaultCategoryOptionCombo();
    periodTypeA = PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY);
    periodA = createPeriod(periodTypeA, getDate(2000, 1, 1), getDate(2000, 1, 31));
    periodB = createPeriod(periodTypeA, getDate(2000, 2, 1), getDate(2000, 2, 28));
    periodService.addPeriod(periodA);
    periodService.addPeriod(periodB);
    unitA = createOrganisationUnit('A');
    unitB = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(unitA);
    organisationUnitService.addOrganisationUnit(unitB);
    dataValueA =
        createDataValue(
            dataElementA, periodA, unitA, categoryOptionComboA, categoryOptionComboA, "10");
    dataValueB =
        createDataValue(
            dataElementA, periodA, unitB, categoryOptionComboA, categoryOptionComboA, "10");
    dataValueService.addDataValue(dataValueA);
    dataValueService.addDataValue(dataValueB);
    auditA = new DataValueAudit(dataValueA, "11", storedBy, AuditOperationType.UPDATE);
    auditB = new DataValueAudit(dataValueA, "12", storedBy, AuditOperationType.UPDATE);
    auditC = new DataValueAudit(dataValueB, "21", storedBy, AuditOperationType.UPDATE);
    auditD = new DataValueAudit(dataValueB, "22", storedBy, AuditOperationType.UPDATE);
  }

  @Test
  void testAddObject() {
    BatchHandler<DataValueAudit> batchHandler;
    batchHandler = batchHandlerFactory.createBatchHandler(DataValueAuditBatchHandler.class).init();
    batchHandler.addObject(auditA);
    batchHandler.addObject(auditB);
    batchHandler.addObject(auditC);
    batchHandler.addObject(auditD);
    batchHandler.flush();
    List<DataValueAudit> auditsA = auditService.getDataValueAudits(dataValueA);
    assertNotNull(auditsA);
    assertEquals(2, auditsA.size());
    List<DataValueAudit> auditsB = auditService.getDataValueAudits(dataValueB);
    assertNotNull(auditsB);
    assertEquals(2, auditsB.size());
  }

  /** DataValueAudit can never equal another. */
  @Test
  void testObjectExists() {
    BatchHandler<DataValueAudit> batchHandler;
    batchHandler = batchHandlerFactory.createBatchHandler(DataValueAuditBatchHandler.class).init();
    auditService.addDataValueAudit(auditA);
    auditService.addDataValueAudit(auditB);
    assertFalse(batchHandler.objectExists(auditA));
    assertFalse(batchHandler.objectExists(auditB));
    assertFalse(batchHandler.objectExists(auditC));
    assertFalse(batchHandler.objectExists(auditD));
  }

  @Test
  void testUpdateObject() {
    BatchHandler<DataValueAudit> batchHandler;
    batchHandler = batchHandlerFactory.createBatchHandler(DataValueAuditBatchHandler.class).init();
    auditService.addDataValueAudit(auditA);
    auditA.setModifiedBy("bill");
    batchHandler.updateObject(auditA);
    List<DataValueAudit> audits = auditService.getDataValueAudits(dataValueA);
    assertEquals(1, audits.size());
    assertEquals("bill", audits.get(0).getModifiedBy());
  }
}
