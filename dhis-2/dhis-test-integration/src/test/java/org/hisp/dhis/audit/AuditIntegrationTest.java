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
package org.hisp.dhis.audit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.auth.ApiHeadersAuthScheme;
import org.hisp.dhis.common.auth.ApiQueryParamsAuthScheme;
import org.hisp.dhis.common.auth.ApiTokenAuthScheme;
import org.hisp.dhis.common.auth.AuthScheme;
import org.hisp.dhis.common.auth.HttpBasicAuthScheme;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.route.Route;
import org.hisp.dhis.test.config.PostgresDhisConfigurationProvider;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

@ActiveProfiles(profiles = {"test-audit"})
@ContextConfiguration(classes = {AuditIntegrationTest.DhisConfig.class})
class AuditIntegrationTest extends PostgresIntegrationTestBase {

  static class DhisConfig {
    @Bean
    public DhisConfigurationProvider dhisConfigurationProvider() {
      Properties override = new Properties();
      override.put("system.audit.enabled", "true");
      override.put("audit.database", "true");
      override.put("audit.metadata", "CREATE");
      override.put("audit.tracker", "CREATE");
      override.put("audit.aggregate", "CREATE");
      PostgresDhisConfigurationProvider postgresDhisConfigurationProvider =
          new PostgresDhisConfigurationProvider(null);
      postgresDhisConfigurationProvider.addProperties(override);
      return postgresDhisConfigurationProvider;
    }
  }

  private static final int TIMEOUT = 5;

  @Autowired private AuditService auditService;

  @Autowired private DataElementService dataElementService;

  @Autowired private TrackedEntityAttributeValueService attributeValueService;

  @Autowired private DataValueService dataValueService;

  @Autowired private PeriodService periodService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private TransactionTemplate transactionTemplate;

  @Autowired private DbmsManager dbmsManager;

  private static Stream<AuthScheme> provideAuthSchemes() {
    return Stream.of(
        new ApiTokenAuthScheme().setToken("passw0rd"),
        new ApiQueryParamsAuthScheme().setQueryParams(Map.of("secret", "passw0rd")),
        new ApiHeadersAuthScheme().setHeaders(Map.of("secret", "passw0rd")),
        new HttpBasicAuthScheme().setUsername("alice").setPassword("passw0rd"));
  }

  @Test
  void testSaveMetadata() {
    DataElement dataElement = createDataElement('A');
    dataElementService.addDataElement(dataElement);
    AuditQuery query = AuditQuery.builder().uid(Sets.newHashSet(dataElement.getUid())).build();
    await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> auditService.countAudits(query) >= 0);
    List<Audit> audits = auditService.getAudits(query);
    assertEquals(1, audits.size());
    Audit audit = audits.get(0);
    assertEquals(DataElement.class.getName(), audit.getKlass());
    assertEquals(dataElement.getUid(), audit.getAttributes().get("uid"));
    assertNotNull(audit.getData());
  }

  @ParameterizedTest
  @Execution(ExecutionMode.SAME_THREAD)
  @MethodSource("provideAuthSchemes")
  void testSaveRoute(AuthScheme authScheme) {
    Route route = new Route();
    route.setUid(BASE_UID);
    route.setName("foo");
    route.setAuth(authScheme);
    route.setUrl("http://stub");

    transactionTemplate.execute(
        status -> {
          manager.save(route);
          dbmsManager.clearSession();
          return null;
        });
    AuditQuery query = AuditQuery.builder().uid(Sets.newHashSet(route.getUid())).build();
    await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> auditService.countAudits(query) >= 0);
    List<Audit> audits = auditService.getAudits(query);
    assertEquals(1, audits.size());
    assertFalse(audits.get(0).getData().contains("passw0rd"));
  }

  @Test
  void testSaveTrackedEntity() {
    OrganisationUnit ou = createOrganisationUnit('A');
    TrackedEntityAttribute attribute = createTrackedEntityAttribute('A');
    manager.save(ou);
    manager.save(attribute);

    TrackedEntityType trackedEntityType = createTrackedEntityType('O');
    manager.save(trackedEntityType);
    TrackedEntity trackedEntity = createTrackedEntity('A', ou, attribute, trackedEntityType);
    manager.save(trackedEntity);
    AuditQuery query = AuditQuery.builder().uid(Sets.newHashSet(trackedEntity.getUid())).build();
    await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> auditService.countAudits(query) >= 0);
    List<Audit> audits = auditService.getAudits(query);
    assertEquals(1, audits.size());
    Audit audit = audits.get(0);
    assertEquals(TrackedEntity.class.getName(), audit.getKlass());
    assertEquals(trackedEntity.getUid(), audit.getUid());
    assertEquals(trackedEntity.getUid(), audit.getAttributes().get("uid"));
    assertNotNull(audit.getData());
  }

  @Test
  void testSaveTrackedAttributeValue() {
    OrganisationUnit ou = createOrganisationUnit('A');
    TrackedEntityAttribute attribute = createTrackedEntityAttribute('A');
    manager.save(ou);
    manager.save(attribute);

    TrackedEntityType trackedEntityType = createTrackedEntityType('O');
    manager.save(trackedEntityType);

    TrackedEntity trackedEntity = createTrackedEntity('A', ou, attribute, trackedEntityType);
    manager.save(trackedEntity);
    TrackedEntityAttributeValue dataValue =
        createTrackedEntityAttributeValue('A', trackedEntity, attribute);
    attributeValueService.addTrackedEntityAttributeValue(dataValue);
    AuditAttributes attributes = new AuditAttributes();
    attributes.put("attribute", attribute.getUid());
    attributes.put("trackedEntity", trackedEntity.getUid());
    AuditQuery query = AuditQuery.builder().auditAttributes(attributes).build();
    await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> auditService.countAudits(query) >= 0);
    List<Audit> audits = auditService.getAudits(query);
    assertEquals(1, audits.size());
    Audit audit = audits.get(0);
    assertEquals(TrackedEntityAttributeValue.class.getName(), audit.getKlass());
    assertEquals(attribute.getUid(), audit.getAttributes().get("attribute"));
    assertEquals(trackedEntity.getUid(), audit.getAttributes().get("trackedEntity"));
    assertNotNull(audit.getData());
  }

  @Test
  void testSaveAggregateDataValue() {
    // ---------------------------------------------------------------------
    // Add supporting data
    // ---------------------------------------------------------------------
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    DataElement dataElementC = createDataElement('C');
    DataElement dataElementD = createDataElement('D');
    dataElementService.addDataElement(dataElementA);
    dataElementService.addDataElement(dataElementB);
    dataElementService.addDataElement(dataElementC);
    dataElementService.addDataElement(dataElementD);
    Period periodA = createPeriod(getDay(5), getDay(6));
    Period periodB = createPeriod(getDay(6), getDay(7));
    Period periodC = createPeriod(getDay(7), getDay(8));
    Period periodD = createPeriod(getDay(8), getDay(9));
    periodService.addPeriod(periodA);
    periodService.addPeriod(periodB);
    periodService.addPeriod(periodC);
    periodService.addPeriod(periodD);
    OrganisationUnit orgUnitA = createOrganisationUnit('A');
    OrganisationUnit orgUnitB = createOrganisationUnit('B');
    OrganisationUnit orgUnitC = createOrganisationUnit('C');
    OrganisationUnit orgUnitD = createOrganisationUnit('D');
    manager.save(orgUnitA);
    manager.save(orgUnitB);
    manager.save(orgUnitC);
    manager.save(orgUnitD);
    CategoryOptionCombo optionCombo = categoryService.getDefaultCategoryOptionCombo();
    categoryService.addCategoryOptionCombo(optionCombo);
    DataValue dataValueA = createDataValue(dataElementA, periodA, orgUnitA, "1", optionCombo);
    DataValue dataValueB = createDataValue(dataElementB, periodB, orgUnitB, "2", optionCombo);
    DataValue dataValueC = createDataValue(dataElementC, periodC, orgUnitC, "3", optionCombo);
    DataValue dataValueD = createDataValue(dataElementD, periodD, orgUnitD, "4", optionCombo);
    dataValueService.addDataValue(dataValueA);
    dataValueService.addDataValue(dataValueB);
    dataValueService.addDataValue(dataValueC);
    dataValueService.addDataValue(dataValueD);
    AuditAttributes attributes = new AuditAttributes();
    attributes.put("dataElement", dataElementA.getUid());
    AuditQuery query = AuditQuery.builder().auditAttributes(attributes).build();
    await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> auditService.countAudits(query) >= 0);
    List<Audit> audits = auditService.getAudits(query);
    assertEquals(1, audits.size());
    Audit audit = audits.get(0);
    assertEquals(DataValue.class.getName(), audit.getKlass());
    assertEquals(dataElementA.getUid(), audit.getAttributes().get("dataElement"));
    assertNotNull(audit.getData());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testSaveProgram() throws IOException {
    Program program = createProgram('A');
    manager.save(program);
    DataElement dataElement = createDataElement('A');
    manager.save(dataElement);
    ProgramStage programStage = createProgramStage('A', program);
    programStage.addDataElement(dataElement, 0);
    manager.save(programStage);
    AuditQuery query = AuditQuery.builder().uid(Sets.newHashSet(programStage.getUid())).build();
    await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> auditService.countAudits(query) >= 0);
    List<Audit> audits = auditService.getAudits(query);
    assertEquals(1, audits.size());
    Audit audit = audits.get(0);
    assertEquals(ProgramStage.class.getName(), audit.getKlass());
    assertEquals(programStage.getUid(), audit.getUid());
    Map<String, Object> deserializeProgramStage =
        objectMapper.readValue(audit.getData(), Map.class);
    assertNotNull(deserializeProgramStage.get("programStageDataElements"));
    List<String> uids = (List<String>) deserializeProgramStage.get("programStageDataElements");
    assertEquals(1, uids.size());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testSaveDataSet() throws JsonProcessingException {
    DataElement dataElement = createDataElement('A');
    PeriodType periodType = PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY);
    DataSet dataSet = createDataSet('A');
    dataSet.setPeriodType(periodType);
    Period period = createPeriod(periodType, getDate(2000, 2, 1), getDate(2000, 2, 28));
    periodService.addPeriod(period);
    transactionTemplate.execute(
        status -> {
          manager.save(dataElement);
          dbmsManager.clearSession();
          return null;
        });
    transactionTemplate.execute(
        status -> {
          manager.save(dataSet);
          dataSet.addDataSetElement(dataElement);
          dbmsManager.clearSession();
          return null;
        });
    AuditQuery query = AuditQuery.builder().uid(Sets.newHashSet(dataSet.getUid())).build();
    await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> auditService.countAudits(query) >= 0);
    List<Audit> audits = auditService.getAudits(query);
    assertEquals(1, audits.size());
    Audit audit = audits.get(0);
    assertEquals(DataSet.class.getName(), audit.getKlass());
    assertEquals(dataSet.getUid(), audit.getUid());
    Map<String, Object> deserializeProgramStage =
        objectMapper.readValue(audit.getData(), Map.class);
    assertNotNull(deserializeProgramStage.get("dataSetElements"));
    List<String> uids = (List<String>) deserializeProgramStage.get("dataSetElements");
    assertEquals(1, uids.size());
  }
}
