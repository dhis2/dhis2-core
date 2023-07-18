/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;

import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.dataapproval.DataApprovalAuditService;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValueAuditService;
import org.hisp.dhis.merge.orgunit.DataMergeStrategy;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeRequest;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.validation.ValidationResultService;
import org.hisp.dhis.validation.ValidationResultsDeletionRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merge handler for data entities.
 *
 * @author Lars Helge Overland
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DataOrgUnitMergeHandler {
  private final SessionFactory sessionFactory;

  private final NamedParameterJdbcTemplate jdbcTemplate;

  private final DataValueAuditService dataValueAuditService;

  private final DataSetService dataSetService;

  private final DataApprovalAuditService dataApprovalAuditService;

  private final ValidationResultService validationResultService;

  private final MinMaxDataElementService minMaxDataElementService;

  public void mergeDataValueAudits(OrgUnitMergeRequest request) {
    request.getSources().forEach(ou -> dataValueAuditService.deleteDataValueAudits(ou));
  }

  @Transactional
  public void mergeDataValues(OrgUnitMergeRequest request) {
    final String sql =
        DataMergeStrategy.DISCARD == request.getDataValueMergeStrategy()
            ? getMergeDataValuesDiscardSql()
            : getMergeDataValuesLastUpdatedSql(request);

    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("source_ids", getIdentifiers(request.getSources()))
            .addValue("target_id", request.getTarget().getId());

    jdbcTemplate.update(sql, params);
  }

  private String getMergeDataValuesDiscardSql() {
    return "delete from datavalue where sourceid in (:source_ids);";
  }

  private String getMergeDataValuesLastUpdatedSql(OrgUnitMergeRequest request) {
    // @formatter:off
    return String.format(
        // Delete existing target data values
        "delete from datavalue where sourceid = :target_id; " +
        // Insert target data values for last modified source data values
        "with dv_rank as ( " +
            // Window over data value sources ranked by last modification
            "select dv.*, row_number() over (" +
                "partition by dv.dataelementid, dv.periodid, dv.categoryoptioncomboid, dv.attributeoptioncomboid " +
                "order by dv.lastupdated desc, dv.created desc) as lastupdated_rank " +
            "from datavalue dv " +
            "where dv.sourceid in (:source_ids) " +
            "and dv.deleted is false" +
        ") " +
        // Insert target data values
        "insert into datavalue (" +
            "dataelementid, periodid, sourceid, categoryoptioncomboid, attributeoptioncomboid, " +
            "value, storedby, created, lastupdated, comment, followup, deleted) " +
        "select dataelementid, periodid, %s, categoryoptioncomboid, attributeoptioncomboid, " +
            "value, storedby, created, lastupdated, comment, followup, false " +
        "from dv_rank " +
        "where dv_rank.lastupdated_rank = 1; " +
        // Delete source data values
        "delete from datavalue where sourceid in (:source_ids);",
        request.getTarget().getId() );
    // @formatter:on
  }

  public void mergeDataApprovalAudits(OrgUnitMergeRequest request) {
    request.getSources().forEach(ou -> dataApprovalAuditService.deleteDataApprovalAudits(ou));
  }

  @Transactional
  public void mergeDataApprovals(OrgUnitMergeRequest request) {
    final String sql =
        DataMergeStrategy.DISCARD == request.getDataApprovalMergeStrategy()
            ? getMergeDataApprovalsDiscardSql()
            : getMergeDataApprovalsLastUpdatedSql(request);

    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("source_ids", getIdentifiers(request.getSources()))
            .addValue("target_id", request.getTarget().getId());

    jdbcTemplate.update(sql, params);
  }

  private String getMergeDataApprovalsDiscardSql() {
    return "delete from dataapproval where organisationunitid in (:source_ids);";
  }

  private String getMergeDataApprovalsLastUpdatedSql(OrgUnitMergeRequest request) {
    // @formatter:off
    return String.format(
        // Delete existing target data approvals
        "delete from dataapproval where organisationunitid = :target_id; "
            +
            // Insert target data approvals for last created source data approvals
            "with da_rank as ( "
            +
            // Window over data approval sources ranked by last created
            "select da.*, row_number() over ("
            + "partition by da.dataapprovallevelid, da.workflowid, da.periodid, da.attributeoptioncomboid "
            + "order by da.created desc) as lastcreated_rank "
            + "from dataapproval da "
            + "where da.organisationunitid in (:source_ids) "
            + ") "
            +
            // Insert target data approvals
            "insert into dataapproval ("
            + "dataapprovalid, dataapprovallevelid, workflowid, periodid, "
            + "organisationunitid, attributeoptioncomboid, accepted, created, creator) "
            + "select nextval('hibernate_sequence'), dataapprovallevelid, workflowid, periodid, "
            + "%s, attributeoptioncomboid, accepted, created, creator "
            + "from da_rank "
            + "where da_rank.lastcreated_rank = 1; "
            +
            // Delete source data approvals
            "delete from dataapproval where organisationunitid in (:source_ids);",
        request.getTarget().getId());
    // @formatter:on
  }

  public void mergeLockExceptions(OrgUnitMergeRequest request) {
    request.getSources().forEach(ou -> dataSetService.deleteLockExceptions(ou));
  }

  public void mergeValidationResults(OrgUnitMergeRequest request) {
    ValidationResultsDeletionRequest deletionRequest = new ValidationResultsDeletionRequest();
    deletionRequest.setOu(IdentifiableObjectUtils.getUids(request.getSources()));

    validationResultService.deleteValidationResults(deletionRequest);
  }

  public void mergeMinMaxDataElements(OrgUnitMergeRequest request) {
    request.getSources().forEach(ou -> minMaxDataElementService.removeMinMaxDataElements(ou));
  }

  @Transactional
  public void mergeInterpretations(OrgUnitMergeRequest request) {
    migrate(
        "update Interpretation i "
            + "set i.organisationUnit = :target "
            + "where i.organisationUnit.id in (:sources)",
        request);
  }

  private void migrate(String hql, OrgUnitMergeRequest request) {
    sessionFactory
        .getCurrentSession()
        .createQuery(hql)
        .setParameter("target", request.getTarget())
        .setParameterList("sources", IdentifiableObjectUtils.getIdentifiers(request.getSources()))
        .executeUpdate();
  }
}
