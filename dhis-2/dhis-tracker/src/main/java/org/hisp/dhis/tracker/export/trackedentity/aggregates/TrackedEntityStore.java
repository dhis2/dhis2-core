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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.export.Geometries;
import org.hisp.dhis.tracker.export.UserInfoSnapshots;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.model.TrackedEntityProgramOwner;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Luciano Fiandesio
 * @author Ameen Mohamed
 */
@Repository
class TrackedEntityStore {
  // language=SQL
  private static final String GET_TE_SQL =
      """
      select te.uid as te_uid, te.created, te.createdatclient, te.createdbyuserinfo,
             te.lastupdated, te.lastupdatedatclient, te.lastupdatedbyuserinfo,
             te.inactive, te.deleted, ST_AsBinary(te.geometry) as geometry,
             tet.trackedentitytypeid as type_id, tet.uid as type_uid, tet.code as type_code,
             tet.name as type_name, tet.attributevalues as tet_attributevalues,
             tet.allowauditlog as type_allowauditlog, tet.enableChangeLog as type_enableChangeLog,
             o.uid as ou_uid, o.code as ou_code, o.name as ou_name, o.path as ou_path,
             o.attributevalues as ou_attributevalues, te.trackedentityid as trackedentityid,
             te.potentialduplicate as potentialduplicate
      from trackedentity te
      join trackedentitytype tet on te.trackedentitytypeid = tet.trackedentitytypeid
      join organisationunit o on te.organisationunitid = o.organisationunitid
      where te.trackedentityid in (:ids)""";

  // language=SQL
  private static final String GET_TE_ATTRIBUTES_WITHOUT_PROGRAM =
      """
      select te.uid as te_uid, teav.created, teav.lastupdated, teav.storedby, teav.value,
             tea.uid as tea_uid, tea.code as tea_code, tea.name as tea_name,
             tea.attributevalues as tea_attributevalues, tea.valuetype as tea_valuetype
      from trackedentityattributevalue teav
      join trackedentityattribute tea on teav.trackedentityattributeid = tea.trackedentityattributeid
      join trackedentity te on teav.trackedentityid = te.trackedentityid
      where teav.trackedentityid in (:ids)
        and teav.trackedentityattributeid in (
          select teta.trackedentityattributeid from trackedentitytypeattribute teta
        )""";

  // language=SQL
  private static final String GET_TE_ATTRIBUTES_WITH_PROGRAM =
      """
      select te.uid as te_uid, teav.created, teav.lastupdated, teav.storedby, teav.value,
             tea.uid as tea_uid, tea.code as tea_code, tea.name as tea_name,
             tea.attributevalues as tea_attributevalues, tea.valuetype as tea_valuetype
      from trackedentityattributevalue teav
      join trackedentityattribute tea on teav.trackedentityattributeid = tea.trackedentityattributeid
      join trackedentity te on teav.trackedentityid = te.trackedentityid
      where teav.trackedentityid in (:ids)
        and teav.trackedentityattributeid in (
          select teta.trackedentityattributeid from trackedentitytypeattribute teta
          union
          select pa.trackedentityattributeid from program_attributes pa where pa.programid = :programId
        )""";

  // language=SQL
  private static final String GET_PROGRAM_OWNERS =
      """
      select te.uid as key, p.uid as prguid, o.uid as ouuid
      from trackedentityprogramowner teop
      join program p on teop.programid = p.programid
      join organisationunit o on teop.organisationunitid = o.organisationunitid
      join trackedentity te on teop.trackedentityid = te.trackedentityid
      where teop.trackedentityid in (:ids)""";

  private final NamedParameterJdbcTemplate jdbcTemplate;

  TrackedEntityStore(@Qualifier("readOnlyJdbcTemplate") JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
  }

  Map<String, TrackedEntity> getTrackedEntities(List<Long> ids) {
    Map<String, TrackedEntity> trackedEntities = new LinkedHashMap<>();
    jdbcTemplate.query(
        applySortOrder(GET_TE_SQL, StringUtils.join(ids, ",")),
        new MapSqlParameterSource("ids", ids),
        (RowCallbackHandler)
            rs -> trackedEntities.put(rs.getString("te_uid"), mapTrackedEntity(rs)));
    return trackedEntities;
  }

  Multimap<String, TrackedEntityAttributeValue> getAttributes(
      List<Long> ids, @CheckForNull Long programId) {
    Multimap<String, TrackedEntityAttributeValue> attributes = ArrayListMultimap.create();
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
        (RowCallbackHandler) rs -> attributes.put(rs.getString("te_uid"), mapAttributeValue(rs)));
    return attributes;
  }

  Multimap<String, TrackedEntityProgramOwner> getProgramOwners(List<Long> ids) {
    Multimap<String, TrackedEntityProgramOwner> programOwners = ArrayListMultimap.create();
    jdbcTemplate.query(
        GET_PROGRAM_OWNERS,
        new MapSqlParameterSource("ids", ids),
        (RowCallbackHandler) rs -> programOwners.put(rs.getString("key"), mapProgramOwner(rs)));
    return programOwners;
  }

  private static TrackedEntity mapTrackedEntity(ResultSet rs) throws SQLException {
    TrackedEntity te = new TrackedEntity();
    te.setUid(rs.getString("te_uid"));

    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setId(rs.getLong("type_id"));
    trackedEntityType.setUid(rs.getString("type_uid"));
    trackedEntityType.setCode(rs.getString("type_code"));
    trackedEntityType.setName(rs.getString("type_name"));
    trackedEntityType.setAttributeValues(AttributeValues.of(rs.getString("tet_attributevalues")));
    trackedEntityType.setAllowAuditLog(rs.getBoolean("type_allowauditlog"));
    trackedEntityType.setEnableChangeLog(rs.getBoolean("type_enableChangeLog"));
    te.setTrackedEntityType(trackedEntityType);

    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setUid(rs.getString("ou_uid"));
    orgUnit.setCode(rs.getString("ou_code"));
    orgUnit.setName(rs.getString("ou_name"));
    orgUnit.setPath(rs.getString("ou_path"));
    orgUnit.setAttributeValues(AttributeValues.of(rs.getString("ou_attributevalues")));
    te.setOrganisationUnit(orgUnit);

    te.setCreated(rs.getTimestamp("created"));
    te.setCreatedAtClient(rs.getTimestamp("createdatclient"));
    te.setCreatedByUserInfo(UserInfoSnapshots.fromJson(rs.getString("createdbyuserinfo")));
    te.setLastUpdated(rs.getTimestamp("lastupdated"));
    te.setLastUpdatedAtClient(rs.getTimestamp("lastupdatedatclient"));
    te.setLastUpdatedByUserInfo(UserInfoSnapshots.fromJson(rs.getString("lastupdatedbyuserinfo")));
    te.setInactive(rs.getBoolean("inactive"));
    te.setDeleted(rs.getBoolean("deleted"));
    te.setPotentialDuplicate(rs.getBoolean("potentialduplicate"));
    te.setGeometry(Geometries.fromWkb(rs.getBytes("geometry")));

    return te;
  }

  private static TrackedEntityAttributeValue mapAttributeValue(ResultSet rs) throws SQLException {
    TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue();
    attributeValue.setCreated(rs.getTimestamp("created"));
    attributeValue.setLastUpdated(rs.getTimestamp("lastupdated"));
    attributeValue.setValue(rs.getString("value"));
    attributeValue.setStoredBy(rs.getString("storedby"));

    TrackedEntityAttribute attribute = new TrackedEntityAttribute();
    attribute.setUid(rs.getString("tea_uid"));
    attribute.setCode(rs.getString("tea_code"));
    attribute.setName(rs.getString("tea_name"));
    attribute.setAttributeValues(AttributeValues.of(rs.getString("tea_attributevalues")));
    attribute.setValueType(ValueType.fromString(rs.getString("tea_valuetype")));
    attributeValue.setAttribute(attribute);

    return attributeValue;
  }

  private static TrackedEntityProgramOwner mapProgramOwner(ResultSet rs) throws SQLException {
    TrackedEntityProgramOwner programOwner = new TrackedEntityProgramOwner();

    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setUid(rs.getString("ouuid"));
    programOwner.setOrganisationUnit(orgUnit);

    Program program = new Program();
    program.setUid(rs.getString("prguid"));
    programOwner.setProgram(program);

    TrackedEntity trackedEntity = new TrackedEntity();
    trackedEntity.setUid(rs.getString("key"));
    programOwner.setTrackedEntity(trackedEntity);

    return programOwner;
  }

  private static String applySortOrder(String sql, String sortOrderIds) {
    String trackedentityid = "trackedentityid";
    return "select * from ("
        + sql
        + ") as t JOIN unnest('{"
        + sortOrderIds
        + "}'::bigint[]) WITH ORDINALITY s("
        + trackedentityid
        + ", sortorder) USING ("
        + trackedentityid
        + ")ORDER  BY s.sortorder";
  }
}
