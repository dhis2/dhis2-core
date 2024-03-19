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
package org.hisp.dhis.tracker.export.trackedentity.aggregates.mapper;

import static org.hisp.dhis.tracker.export.trackedentity.aggregates.mapper.JsonbToObjectHelper.setUserInfoSnapshot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.export.trackedentity.aggregates.query.TrackedEntityQuery;
import org.hisp.dhis.tracker.export.trackedentity.aggregates.query.TrackedEntityQuery.COLUMNS;
import org.springframework.jdbc.core.RowCallbackHandler;

/**
 * @author Luciano Fiandesio
 */
public class TrackedEntityRowCallbackHandler implements RowCallbackHandler {
  private final Map<String, TrackedEntity> items;

  public TrackedEntityRowCallbackHandler() {
    this.items = new LinkedHashMap<>();
  }

  private TrackedEntity getTei(ResultSet rs) throws SQLException {

    TrackedEntity te = new TrackedEntity();
    te.setUid(rs.getString(TrackedEntityQuery.getColumnName(COLUMNS.UID)));
    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid(rs.getString(TrackedEntityQuery.getColumnName(COLUMNS.TYPE_UID)));
    te.setTrackedEntityType(trackedEntityType);

    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setUid(rs.getString(TrackedEntityQuery.getColumnName(COLUMNS.ORGUNIT_UID)));
    te.setOrganisationUnit(orgUnit);

    te.setCreated(rs.getTimestamp(TrackedEntityQuery.getColumnName(COLUMNS.CREATED)));
    te.setCreatedAtClient(rs.getTimestamp(TrackedEntityQuery.getColumnName(COLUMNS.CREATEDCLIENT)));
    setUserInfoSnapshot(
        rs, TrackedEntityQuery.getColumnName(COLUMNS.CREATED_BY), te::setCreatedByUserInfo);
    te.setLastUpdated(rs.getTimestamp(TrackedEntityQuery.getColumnName(COLUMNS.UPDATED)));
    te.setLastUpdatedAtClient(
        rs.getTimestamp(TrackedEntityQuery.getColumnName(COLUMNS.UPDATEDCLIENT)));
    setUserInfoSnapshot(
        rs,
        TrackedEntityQuery.getColumnName(COLUMNS.LAST_UPDATED_BY),
        te::setLastUpdatedByUserInfo);
    te.setInactive(rs.getBoolean(TrackedEntityQuery.getColumnName(COLUMNS.INACTIVE)));
    te.setDeleted(rs.getBoolean(TrackedEntityQuery.getColumnName(COLUMNS.DELETED)));
    te.setPotentialDuplicate(
        rs.getBoolean(TrackedEntityQuery.getColumnName(COLUMNS.POTENTIALDUPLICATE)));
    MapperGeoUtils.resolveGeometry(rs.getBytes(TrackedEntityQuery.getColumnName(COLUMNS.GEOMETRY)))
        .ifPresent(te::setGeometry);

    return te;
  }

  @Override
  public void processRow(ResultSet rs) throws SQLException {
    this.items.put(rs.getString("te_uid"), getTei(rs));
  }

  public Map<String, TrackedEntity> getItems() {
    return this.items;
  }
}
