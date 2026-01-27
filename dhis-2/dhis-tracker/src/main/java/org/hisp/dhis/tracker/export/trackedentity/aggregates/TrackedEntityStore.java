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
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.model.TrackedEntityProgramOwner;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class TrackedEntityStore {
  private static final int PARTITION_SIZE = 20000;

  private static final String GET_ATTRIBUTES =
      """
      SELECT te.uid as te_uid,
             teav.created,
             teav.lastupdated,
             teav.storedby,
             teav.value,
             t.uid as attr_uid,
             t.code as attr_code,
             t.name as attr_name,
             t.attributevalues as attr_attributevalues,
             t.valuetype as attr_valuetype,
             t.skipsynchronization as attr_skipsync
      FROM trackedentityattributevalue teav
      JOIN trackedentityattribute t ON teav.trackedentityattributeid = t.trackedentityattributeid
      JOIN trackedentity te ON teav.trackedentityid = te.trackedentityid
      WHERE teav.trackedentityid IN (:ids)
      """;

  private static final String GET_PROGRAM_OWNERS =
      """
      SELECT te.uid as te_uid, p.uid as program_uid, o.uid as orgunit_uid
      FROM trackedentityprogramowner teop
      JOIN program p ON teop.programid = p.programid
      JOIN organisationunit o ON teop.organisationunitid = o.organisationunitid
      JOIN trackedentity te ON teop.trackedentityid = te.trackedentityid
      WHERE teop.trackedentityid IN (:ids)
      """;

  private final NamedParameterJdbcTemplate jdbcTemplate;

  TrackedEntityStore(@Qualifier("readOnlyJdbcTemplate") JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
  }

  Multimap<String, TrackedEntityAttributeValue> getAttributes(List<Long> ids) {
    Multimap<String, TrackedEntityAttributeValue> results = ArrayListMultimap.create();
    for (List<Long> partition : Lists.partition(ids, PARTITION_SIZE)) {
      jdbcTemplate.query(
          GET_ATTRIBUTES,
          new MapSqlParameterSource("ids", partition),
          (RowCallbackHandler) rs -> results.put(rs.getString("te_uid"), mapAttribute(rs)));
    }
    return results;
  }

  Multimap<String, TrackedEntityProgramOwner> getProgramOwners(List<Long> ids) {
    Multimap<String, TrackedEntityProgramOwner> results = ArrayListMultimap.create();
    for (List<Long> partition : Lists.partition(ids, PARTITION_SIZE)) {
      jdbcTemplate.query(
          GET_PROGRAM_OWNERS,
          new MapSqlParameterSource("ids", partition),
          (RowCallbackHandler) rs -> results.put(rs.getString("te_uid"), mapProgramOwner(rs)));
    }
    return results;
  }

  private static TrackedEntityAttributeValue mapAttribute(ResultSet rs) throws SQLException {
    TrackedEntityAttributeValue av = new TrackedEntityAttributeValue();
    av.setCreated(rs.getTimestamp("created"));
    av.setLastUpdated(rs.getTimestamp("lastupdated"));
    av.setValue(rs.getString("value"));
    av.setStoredBy(rs.getString("storedby"));

    TrackedEntityAttribute attr = new TrackedEntityAttribute();
    attr.setUid(rs.getString("attr_uid"));
    attr.setCode(rs.getString("attr_code"));
    attr.setName(rs.getString("attr_name"));
    attr.setAttributeValues(AttributeValues.of(rs.getString("attr_attributevalues")));
    attr.setValueType(ValueType.fromString(rs.getString("attr_valuetype")));
    attr.setSkipSynchronization(rs.getBoolean("attr_skipsync"));
    av.setAttribute(attr);

    return av;
  }

  private static TrackedEntityProgramOwner mapProgramOwner(ResultSet rs) throws SQLException {
    TrackedEntityProgramOwner po = new TrackedEntityProgramOwner();

    TrackedEntity te = new TrackedEntity();
    te.setUid(rs.getString("te_uid"));
    po.setTrackedEntity(te);

    Program program = new Program();
    program.setUid(rs.getString("program_uid"));
    po.setProgram(program);

    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setUid(rs.getString("orgunit_uid"));
    po.setOrganisationUnit(orgUnit);

    return po;
  }
}
