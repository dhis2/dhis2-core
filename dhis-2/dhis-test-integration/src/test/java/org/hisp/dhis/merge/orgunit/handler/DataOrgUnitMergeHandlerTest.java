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
package org.hisp.dhis.merge.orgunit.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.Sets;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.DataApproval;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataDumpService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.merge.DataMergeStrategy;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeRequest;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeRequest.Builder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Transactional
class DataOrgUnitMergeHandlerTest extends PostgresIntegrationTestBase {

  @Autowired private CategoryService categoryService;

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private PeriodService periodService;

  @Autowired private DataDumpService dataDumpService;

  @Autowired private DataApprovalService dataApprovalService;

  @Autowired private DataOrgUnitMergeHandler handler;

  @Autowired private NamedParameterJdbcTemplate jdbcTemplate;

  private DataElement deA;

  private DataElement deB;

  private CategoryOptionCombo cocA;

  private Period peA;

  private Period peB;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  private User usA;

  private User usB;

  private DataApprovalLevel dlA;

  private DataApprovalWorkflow dwA;

  @BeforeEach
  void setUp() {
    cocA = categoryService.getDefaultCategoryOptionCombo();
    deA = createDataElement('A');
    deB = createDataElement('B');
    idObjectManager.save(deA);
    idObjectManager.save(deB);
    PeriodType monthly = periodService.getPeriodTypeByClass(MonthlyPeriodType.class);
    peA = monthly.createPeriod("202101");
    peB = monthly.createPeriod("202102");
    periodService.addPeriod(peA);
    periodService.addPeriod(peB);
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
    ouC = createOrganisationUnit('C');
    idObjectManager.save(ouA);
    idObjectManager.save(ouB);
    idObjectManager.save(ouC);
    usA = makeUser("A");
    userService.addUser(usA);
    usB = makeUser("B");
    userService.addUser(usB);
    dlA = new DataApprovalLevel("DataApprovalLevelA", 1);
    idObjectManager.save(dlA);
    CategoryCombo ccA = categoryService.getDefaultCategoryCombo();
    dwA = new DataApprovalWorkflow("DataApprovalWorkflowA", monthly, ccA, Sets.newHashSet(dlA));
    idObjectManager.save(dwA);
    // flush so the raw-SQL count/merge queries see the setup metadata
    entityManager.flush();
  }

  @Test
  void testMergeDataValues() {
    addDataValues(
        createDataValue(deA, peA, ouA, cocA, cocA, "10", date(2021, 1, 1), date(2021, 1, 1)),
        createDataValue(deA, peA, ouB, cocA, cocA, "11", date(2021, 2, 1), date(2021, 2, 1)),
        createDataValue(deB, peA, ouA, cocA, cocA, "12", date(2021, 3, 1), date(2021, 3, 1)),
        createDataValue(deB, peA, ouB, cocA, cocA, "13", date(2021, 4, 1), date(2021, 4, 1)));
    assertEquals(2, getDataValueCount(ouA));
    assertEquals(2, getDataValueCount(ouB));
    assertEquals(0, getDataValueCount(ouC));
    OrgUnitMergeRequest request =
        new Builder()
            .addSource(ouA)
            .addSource(ouB)
            .withTarget(ouC)
            .withDataValueMergeStrategy(DataMergeStrategy.LAST_UPDATED)
            .build();
    handler.mergeDataValues(request);
    assertEquals(0, getDataValueCount(ouA));
    assertEquals(0, getDataValueCount(ouB));
    assertEquals(2, getDataValueCount(ouC));
  }

  @Test
  void testMergeDataApprovals() {
    addDataApprovals(
        new DataApproval(dlA, dwA, peA, ouA, cocA, false, date(2021, 1, 1), usA),
        new DataApproval(dlA, dwA, peA, ouB, cocA, false, date(2021, 2, 1), usA),
        new DataApproval(dlA, dwA, peB, ouA, cocA, false, date(2021, 3, 1), usA),
        new DataApproval(dlA, dwA, peB, ouB, cocA, false, date(2021, 4, 1), usA));
    assertEquals(2, getDataApprovalCount(ouA));
    assertEquals(2, getDataApprovalCount(ouB));
    assertEquals(0, getDataApprovalCount(ouC));
    OrgUnitMergeRequest request =
        new Builder()
            .addSource(ouA)
            .addSource(ouB)
            .withTarget(ouC)
            .withDataApprovalMergeStrategy(DataMergeStrategy.LAST_UPDATED)
            .build();
    handler.mergeDataApprovals(request);
    assertEquals(0, getDataApprovalCount(ouA));
    assertEquals(0, getDataApprovalCount(ouB));
    assertEquals(2, getDataApprovalCount(ouC));
  }

  @Test
  void testMergeDataValuesKeepsTargetValues() {
    addDataValues(
        createDataValue(deA, peA, ouC, cocA, cocA, "20", date(2021, 1, 1), date(2021, 1, 1)),
        createDataValue(deB, peA, ouA, cocA, cocA, "12", date(2021, 2, 1), date(2021, 2, 1)));

    handler.mergeDataValues(lastUpdatedRequest());

    assertEquals(0, getDataValueCount(ouA));
    assertEquals(2, getDataValueCount(ouC));
    assertEquals("20", getDataValue(deA, peA, ouC));
    assertEquals("12", getDataValue(deB, peA, ouC));
  }

  @Test
  void testMergeDataValuesKeepsNewerTargetValue() {
    addDataValues(
        createDataValue(deA, peA, ouC, cocA, cocA, "20"),
        createDataValue(deA, peA, ouA, cocA, cocA, "10"));
    setTimestamps(deA, peA, ouC, date(2021, 1, 1), date(2021, 5, 1));
    setTimestamps(deA, peA, ouA, date(2021, 1, 1), date(2021, 3, 1));

    handler.mergeDataValues(lastUpdatedRequest());

    assertEquals(0, getDataValueCount(ouA));
    assertEquals(1, getDataValueCount(ouC));
    assertEquals("20", getDataValue(deA, peA, ouC));
  }

  @Test
  void testMergeDataValuesTakesNewerSourceValue() {
    addDataValues(
        createDataValue(deA, peA, ouC, cocA, cocA, "20"),
        createDataValue(deA, peA, ouA, cocA, cocA, "10"));
    setTimestamps(deA, peA, ouC, date(2021, 1, 1), date(2021, 3, 1));
    setTimestamps(deA, peA, ouA, date(2021, 1, 1), date(2021, 5, 1));

    handler.mergeDataValues(lastUpdatedRequest());

    assertEquals(0, getDataValueCount(ouA));
    assertEquals(1, getDataValueCount(ouC));
    assertEquals("10", getDataValue(deA, peA, ouC));
  }

  @Test
  void testMergeDataValuesKeepsTargetValueOnTie() {
    // the source row is written first, so the target only wins through the tie-breaker
    addDataValues(createDataValue(deA, peA, ouA, cocA, cocA, "10"));
    addDataValues(createDataValue(deA, peA, ouC, cocA, cocA, "20"));
    setTimestamps(deA, peA, ouA, date(2021, 1, 1), date(2021, 4, 1));
    setTimestamps(deA, peA, ouC, date(2021, 1, 1), date(2021, 4, 1));

    handler.mergeDataValues(lastUpdatedRequest());

    assertEquals(0, getDataValueCount(ouA));
    assertEquals(1, getDataValueCount(ouC));
    assertEquals("20", getDataValue(deA, peA, ouC));
  }

  @Test
  void testMergeDataApprovalsKeepsTargetApprovals() {
    addDataApprovals(
        new DataApproval(dlA, dwA, peA, ouC, cocA, false, date(2021, 1, 1), usA),
        approval(peB, ouA, true, date(2021, 2, 1), usA, date(2021, 2, 15), usB));

    handler.mergeDataApprovals(lastUpdatedRequest());

    assertEquals(0, getDataApprovalCount(ouA));
    assertEquals(2, getDataApprovalCount(ouC));
    assertLastUpdated(peB, ouC, date(2021, 2, 15), usB);
  }

  @Test
  void testMergeDataApprovalsKeepsNewerTargetApproval() {
    addDataApprovals(
        new DataApproval(dlA, dwA, peA, ouC, cocA, true, date(2021, 5, 1), usA),
        new DataApproval(dlA, dwA, peA, ouA, cocA, false, date(2021, 3, 1), usA));

    handler.mergeDataApprovals(lastUpdatedRequest());

    assertEquals(0, getDataApprovalCount(ouA));
    assertEquals(1, getDataApprovalCount(ouC));
    assertTrue(isDataApprovalAccepted(peA, ouC));
  }

  @Test
  void testMergeDataApprovalsTakesNewerSourceApproval() {
    addDataApprovals(
        new DataApproval(dlA, dwA, peA, ouC, cocA, true, date(2021, 3, 1), usA),
        approval(peA, ouA, false, date(2021, 5, 1), usA, date(2021, 6, 1), usB));

    handler.mergeDataApprovals(lastUpdatedRequest());

    assertEquals(0, getDataApprovalCount(ouA));
    assertEquals(1, getDataApprovalCount(ouC));
    assertFalse(isDataApprovalAccepted(peA, ouC));
    assertLastUpdated(peA, ouC, date(2021, 6, 1), usB);
  }

  @Test
  void testMergeDataApprovalsKeepsTargetApprovalOnTie() {
    // the source approval is written first, so the target only wins through the tie-breaker
    addDataApprovals(new DataApproval(dlA, dwA, peA, ouA, cocA, false, date(2021, 4, 1), usA));
    addDataApprovals(new DataApproval(dlA, dwA, peA, ouC, cocA, true, date(2021, 4, 1), usA));

    handler.mergeDataApprovals(lastUpdatedRequest());

    assertEquals(0, getDataApprovalCount(ouA));
    assertEquals(1, getDataApprovalCount(ouC));
    assertTrue(isDataApprovalAccepted(peA, ouC));
  }

  private OrgUnitMergeRequest lastUpdatedRequest() {
    return new Builder()
        .addSource(ouA)
        .withTarget(ouC)
        .withDataValueMergeStrategy(DataMergeStrategy.LAST_UPDATED)
        .withDataApprovalMergeStrategy(DataMergeStrategy.LAST_UPDATED)
        .build();
  }

  private String getDataValue(DataElement de, Period pe, OrganisationUnit ou) {
    final String sql =
        "select dv.value from datavalue dv where dv.dataelementid = :de and dv.periodid = :pe"
            + " and dv.sourceid = :ou and dv.deleted is false";
    return jdbcTemplate.queryForObject(
        sql,
        new MapSqlParameterSource()
            .addValue("de", de.getId())
            .addValue("pe", pe.getId())
            .addValue("ou", ou.getId()),
        String.class);
  }

  /**
   * The data value upsert used for test data stamps the timestamps itself, so set them afterwards.
   */
  private void setTimestamps(
      DataElement de, Period pe, OrganisationUnit ou, Date created, Date lastUpdated) {
    final String sql =
        "update datavalue set created = :created, lastupdated = :lastupdated"
            + " where dataelementid = :de and periodid = :pe and sourceid = :ou";
    int rows =
        jdbcTemplate.update(
            sql,
            new MapSqlParameterSource()
                .addValue("created", created)
                .addValue("lastupdated", lastUpdated)
                .addValue("de", de.getId())
                .addValue("pe", pe.getId())
                .addValue("ou", ou.getId()));
    assertEquals(1, rows);
  }

  /** An approval last changed (accepted or unaccepted) by another user after it was created. */
  private DataApproval approval(
      Period pe,
      OrganisationUnit ou,
      boolean accepted,
      Date created,
      User creator,
      Date lastUpdated,
      User lastUpdatedBy) {
    DataApproval approval = new DataApproval(dlA, dwA, pe, ou, cocA, accepted, created, creator);
    approval.setLastUpdated(lastUpdated);
    approval.setLastUpdatedBy(lastUpdatedBy);
    return approval;
  }

  private void assertLastUpdated(
      Period pe, OrganisationUnit ou, Date lastUpdated, User lastUpdatedBy) {
    final String sql =
        "select da.lastupdated, da.lastupdatedby from dataapproval da where da.periodid = :pe"
            + " and da.organisationunitid = :ou";
    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            sql, new MapSqlParameterSource().addValue("pe", pe.getId()).addValue("ou", ou.getId()));
    assertEquals(lastUpdated.getTime(), ((Date) row.get("lastupdated")).getTime());
    assertEquals(lastUpdatedBy.getId(), ((Number) row.get("lastupdatedby")).longValue());
  }

  private boolean isDataApprovalAccepted(Period pe, OrganisationUnit ou) {
    final String sql =
        "select da.accepted from dataapproval da where da.periodid = :pe"
            + " and da.organisationunitid = :ou";
    return jdbcTemplate.queryForObject(
        sql,
        new MapSqlParameterSource().addValue("pe", pe.getId()).addValue("ou", ou.getId()),
        Boolean.class);
  }

  private long getDataValueCount(OrganisationUnit target) {
    final String sql = "select count(*) from datavalue dv where dv.sourceid = :target_id";
    return jdbcTemplate.queryForObject(
        sql, new MapSqlParameterSource().addValue("target_id", target.getId()), Long.class);
  }

  private long getDataApprovalCount(OrganisationUnit target) {
    final String sql =
        "select count(*) from dataapproval da where da.organisationunitid = :target_id";
    return jdbcTemplate.queryForObject(
        sql, new MapSqlParameterSource().addValue("target_id", target.getId()), Long.class);
  }

  private void addDataValues(DataValue... values) {
    if (dataDumpService.upsertValues(values) < values.length) fail("Failed to upsert test data");
  }

  private void addDataApprovals(DataApproval... dataApprovals) {
    Stream.of(dataApprovals).forEach(dataApprovalService::addDataApproval);
    // flush so the raw-SQL data-approval count/merge queries see the session writes
    entityManager.flush();
  }
}
