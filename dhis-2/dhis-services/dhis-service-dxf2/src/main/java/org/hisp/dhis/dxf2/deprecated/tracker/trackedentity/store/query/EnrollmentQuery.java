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
package org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.query;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.Function;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.QueryElement;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.TableColumn;

/**
 * @author Luciano Fiandesio
 */
public class EnrollmentQuery {
  public enum COLUMNS {
    TEI_UID,
    ID,
    UID,
    CREATED,
    CREATEDCLIENT,
    CREATED_BY,
    UPDATED,
    UPDATEDCLIENT,
    LAST_UPDATED_BY,
    STATUS,
    GEOMETRY,
    ENROLLMENTDATE,
    INCIDENTDATE,
    FOLLOWUP,
    COMPLETED,
    COMPLETEDBY,
    STOREDBY,
    DELETED,
    PROGRAM_UID,
    PROGRAM_FEATURE_TYPE,
    TEI_TYPE_UID,
    ORGUNIT_UID,
    ORGUNIT_NAME
  }

  public static Map<COLUMNS, ? extends QueryElement> columnMap =
      ImmutableMap.<COLUMNS, QueryElement>builder()
          .put(COLUMNS.TEI_UID, new TableColumn("tei", "uid", "tei_uid"))
          .put(COLUMNS.GEOMETRY, new Function("ST_AsBinary", "pi", "geometry", "geometry"))
          .put(COLUMNS.ID, new TableColumn("pi", "programinstanceid"))
          .put(COLUMNS.UID, new TableColumn("pi", "uid"))
          .put(COLUMNS.CREATED, new TableColumn("pi", "created"))
          .put(COLUMNS.CREATEDCLIENT, new TableColumn("pi", "createdatclient"))
          .put(COLUMNS.CREATED_BY, new TableColumn("pi", "createdbyuserinfo"))
          .put(COLUMNS.UPDATED, new TableColumn("pi", "lastupdated"))
          .put(COLUMNS.UPDATEDCLIENT, new TableColumn("pi", "lastupdatedatclient"))
          .put(COLUMNS.LAST_UPDATED_BY, new TableColumn("pi", "lastupdatedbyuserinfo"))
          .put(COLUMNS.STATUS, new TableColumn("pi", "status"))
          .put(COLUMNS.ENROLLMENTDATE, new TableColumn("pi", "enrollmentdate"))
          .put(COLUMNS.INCIDENTDATE, new TableColumn("pi", "incidentdate"))
          .put(COLUMNS.FOLLOWUP, new TableColumn("pi", "followup"))
          .put(COLUMNS.COMPLETED, new TableColumn("pi", "enddate"))
          .put(COLUMNS.COMPLETEDBY, new TableColumn("pi", "completedby"))
          .put(COLUMNS.STOREDBY, new TableColumn("pi", "storedby"))
          .put(COLUMNS.DELETED, new TableColumn("pi", "deleted"))
          .put(COLUMNS.PROGRAM_UID, new TableColumn("p", "uid", "program_uid"))
          .put(
              COLUMNS.PROGRAM_FEATURE_TYPE,
              new TableColumn("p", "featuretype", "program_feature_type"))
          .put(COLUMNS.TEI_TYPE_UID, new TableColumn("tet", "uid", "type_uid"))
          .put(COLUMNS.ORGUNIT_UID, new TableColumn("o", "uid", "ou_uid"))
          .put(COLUMNS.ORGUNIT_NAME, new TableColumn("o", "name", "ou_name"))
          .build();

  public static String getQuery() {
    return getSelect()
        + "from programinstance pi "
        + "join program p on pi.programid = p.programid "
        + "join trackedentityinstance tei on pi.trackedentityinstanceid = tei.trackedentityinstanceid "
        + "join trackedentitytype tet on tei.trackedentitytypeid = tet.trackedentitytypeid "
        + "join organisationunit o on tei.organisationunitid = o.organisationunitid "
        + "where pi.trackedentityinstanceid in (:ids) ";
  }

  private static String getSelect() {
    return QueryUtils.getSelect(columnMap.values());
  }

  public static String getColumnName(COLUMNS columns) {
    return columnMap.get(columns).getResultsetValue();
  }
}
