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
package org.hisp.dhis.tracker.export.trackedentity.aggregates;

import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.export.timeout.TrackerExportTimeoutConfig;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.model.TrackedEntityProgramOwner;
import org.hisp.dhis.user.UserDetails;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Luciano Fiandesio
 * @author Ameen Mohamed
 */
@Repository
@RequiredArgsConstructor
class TrackedEntityStore {
  // language=SQL
  private static final String GET_TE_ATTRIBUTES_WITHOUT_PROGRAM =
      """
      select teav.trackedentityid as te_id, teav.created, teav.lastupdated, teav.updatedby,
             teav.value, tea.uid as tea_uid, tea.code as tea_code, tea.name as tea_name,
             tea.attributevalues as tea_attributevalues, tea.valuetype as tea_valuetype,
             tea.skipsynchronization as tea_skipsynchronization
      from trackedentityattributevalue teav
      join trackedentityattribute tea on teav.trackedentityattributeid = tea.trackedentityattributeid
      where teav.trackedentityid in (:ids)
        and teav.trackedentityattributeid in (
          select teta.trackedentityattributeid from trackedentitytypeattribute teta
        )""";

  // language=SQL
  private static final String GET_TE_ATTRIBUTES_WITH_PROGRAM =
      """
      select teav.trackedentityid as te_id, teav.created, teav.lastupdated, teav.updatedby,
             teav.value, tea.uid as tea_uid, tea.code as tea_code, tea.name as tea_name,
             tea.attributevalues as tea_attributevalues, tea.valuetype as tea_valuetype,
             tea.skipsynchronization as tea_skipsynchronization
      from trackedentityattributevalue teav
      join trackedentityattribute tea on teav.trackedentityattributeid = tea.trackedentityattributeid
      where teav.trackedentityid in (:ids)
        and teav.trackedentityattributeid in (
          select teta.trackedentityattributeid from trackedentitytypeattribute teta
          union
          select pa.trackedentityattributeid from program_attributes pa where pa.programid = :programId
        )""";

  private static final String GET_PROGRAM_OWNERS =
      """
      select teop.trackedentityid as te_id, p.uid as prguid, o.uid as ouuid
      from trackedentityprogramowner teop
      join program p on teop.programid = p.programid
      join organisationunit o on teop.organisationunitid = o.organisationunitid
      where teop.trackedentityid in (:ids)
      and p.programid in (:programIds)
      """;

  /** True if the tracked entity has no enrollment in the program of the owner. */
  private static final String NO_ENROLLMENT =
      """
      not exists (select 1 from enrollment e
                  where e.trackedentityid = teop.trackedentityid
                    and e.programid = teop.programid)""";

  /**
   * True if the tracked entity has an enrollment in the program of the owner whose attribute option
   * combo the user can read.
   */
  private static final String READABLE_ENROLLMENT =
      """
      exists (select 1 from enrollment e
              where e.trackedentityid = teop.trackedentityid
                and e.programid = teop.programid
                and e.attributeoptioncomboid in (:readableAttributeOptionComboIds))""";

  /**
   * Returns the attribute option combos of the given tracked entities' enrollments the user can
   * read, so the sharing is evaluated once per attribute option combo instead of once per
   * enrollment.
   */
  private static final String GET_READABLE_ENROLLMENT_ATTRIBUTE_OPTION_COMBOS =
      """
      select cocco.categoryoptioncomboid
      from categoryoptioncombos_categoryoptions cocco
      join categoryoption co on co.categoryoptionid = cocco.categoryoptionid
      where cocco.categoryoptioncomboid in (
        select distinct e.attributeoptioncomboid from enrollment e
        where e.trackedentityid in (:ids)
      )
      group by cocco.categoryoptioncomboid
      having bool_and(coalesce((%s), false))
      """;

  @Qualifier(TrackerExportTimeoutConfig.TRACKER_EXPORT_JDBC_TEMPLATE)
  private final NamedParameterJdbcTemplate jdbcTemplate;

  Multimap<Long, TrackedEntityAttributeValue> getAttributes(
      List<Long> ids, @CheckForNull Long programId) {
    Multimap<Long, TrackedEntityAttributeValue> attributes = ArrayListMultimap.create();
    MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
    String sql;
    if (programId == null) {
      sql = GET_TE_ATTRIBUTES_WITHOUT_PROGRAM;
    } else {
      sql = GET_TE_ATTRIBUTES_WITH_PROGRAM;
      params.addValue("programId", programId);
    }
    jdbcTemplate.query(
        sql,
        params,
        (RowCallbackHandler) rs -> attributes.put(rs.getLong("te_id"), mapAttributeValue(rs)));
    return attributes;
  }

  /**
   * Returns the owners of programs the user has data read access to. Owners of programs in which
   * the tracked entity only has enrollments the user cannot read due to the attribute option combo
   * are left out, as they would reveal that the tracked entity is enrolled in the program.
   */
  Multimap<Long, TrackedEntityProgramOwner> getProgramOwners(
      TrackedEntityQueryParams queryParams, List<Long> ids) {
    Multimap<Long, TrackedEntityProgramOwner> programOwners = ArrayListMultimap.create();
    List<Long> programIds =
        queryParams.hasEnrolledInTrackerProgram()
            ? List.of(queryParams.getEnrolledInTrackerProgram().getId())
            : queryParams.getAccessibleTrackerPrograms().stream().map(Program::getId).toList();
    if (programIds.isEmpty()) {
      return programOwners;
    }

    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("ids", ids).addValue("programIds", programIds);
    String sql = GET_PROGRAM_OWNERS;
    UserDetails user = getCurrentUserDetails();
    if (!user.isSuper()) {
      List<Long> readableAttributeOptionComboIds =
          getReadableEnrollmentAttributeOptionCombos(ids, user);
      if (readableAttributeOptionComboIds.isEmpty()) {
        sql += " and " + NO_ENROLLMENT;
      } else {
        sql += " and (" + NO_ENROLLMENT + " or " + READABLE_ENROLLMENT + ")";
        params.addValue("readableAttributeOptionComboIds", readableAttributeOptionComboIds);
      }
    }

    jdbcTemplate.query(
        sql,
        params,
        (RowCallbackHandler) rs -> programOwners.put(rs.getLong("te_id"), mapProgramOwner(rs)));
    return programOwners;
  }

  private List<Long> getReadableEnrollmentAttributeOptionCombos(List<Long> ids, UserDetails user) {
    String sql =
        String.format(
            GET_READABLE_ENROLLMENT_ATTRIBUTE_OPTION_COMBOS,
            JpaQueryUtils.generateSQlQueryForSharingCheck(
                "co.sharing", user, AclService.LIKE_READ_DATA));
    return jdbcTemplate.queryForList(sql, new MapSqlParameterSource("ids", ids), Long.class);
  }

  private static TrackedEntityAttributeValue mapAttributeValue(ResultSet rs) throws SQLException {
    TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue();
    attributeValue.setCreated(rs.getTimestamp("created"));
    attributeValue.setLastUpdated(rs.getTimestamp("lastupdated"));
    attributeValue.setValue(rs.getString("value"));
    attributeValue.setUpdatedBy(rs.getString("updatedby"));

    TrackedEntityAttribute attribute = new TrackedEntityAttribute();
    attribute.setUid(rs.getString("tea_uid"));
    attribute.setCode(rs.getString("tea_code"));
    attribute.setName(rs.getString("tea_name"));
    attribute.setAttributeValues(AttributeValues.of(rs.getString("tea_attributevalues")));
    attribute.setValueType(ValueType.fromString(rs.getString("tea_valuetype")));
    attribute.setSkipSynchronization(rs.getBoolean("tea_skipsynchronization"));
    attributeValue.setAttribute(attribute);

    return attributeValue;
  }

  /**
   * The owning tracked entity is left unset: the query no longer joins {@code trackedentity}, so it
   * only knows the id. {@link TrackedEntityAggregate} holds the {@link TrackedEntity} the owner
   * belongs to and sets it.
   */
  private static TrackedEntityProgramOwner mapProgramOwner(ResultSet rs) throws SQLException {
    TrackedEntityProgramOwner programOwner = new TrackedEntityProgramOwner();

    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setUid(rs.getString("ouuid"));
    programOwner.setOrganisationUnit(orgUnit);

    Program program = new Program();
    program.setUid(rs.getString("prguid"));
    programOwner.setProgram(program);

    return programOwner;
  }
}
