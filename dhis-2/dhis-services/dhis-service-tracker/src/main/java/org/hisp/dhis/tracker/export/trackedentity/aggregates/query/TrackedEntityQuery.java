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
package org.hisp.dhis.tracker.export.trackedentity.aggregates.query;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * @author Luciano Fiandesio
 */
public class TrackedEntityQuery {
  public enum COLUMNS {
    UID,
    CREATED,
    CREATEDCLIENT,
    CREATED_BY,
    UPDATED,
    UPDATEDCLIENT,
    LAST_UPDATED_BY,
    INACTIVE,
    DELETED,
    GEOMETRY,
    TYPE_UID,
    ORGUNIT_UID,
    TRACKEDENTITYID,

    POTENTIALDUPLICATE
  }

  public static final Map<COLUMNS, ? extends QueryElement> columnMap =
      ImmutableMap.<COLUMNS, QueryElement>builder()
          .put(COLUMNS.UID, new TableColumn("te", "uid", "te_uid"))
          .put(COLUMNS.CREATED, new TableColumn("te", "created"))
          .put(COLUMNS.CREATEDCLIENT, new TableColumn("te", "createdatclient"))
          .put(COLUMNS.CREATED_BY, new TableColumn("te", "createdbyuserinfo"))
          .put(COLUMNS.UPDATED, new TableColumn("te", "lastupdated"))
          .put(COLUMNS.UPDATEDCLIENT, new TableColumn("te", "lastupdatedatclient"))
          .put(COLUMNS.LAST_UPDATED_BY, new TableColumn("te", "lastupdatedbyuserinfo"))
          .put(COLUMNS.INACTIVE, new TableColumn("te", "inactive"))
          .put(COLUMNS.DELETED, new TableColumn("te", "deleted"))
          .put(COLUMNS.GEOMETRY, new Function("ST_AsBinary", "te", "geometry", "geometry"))
          .put(COLUMNS.TYPE_UID, new TableColumn("tet", "uid", "type_uid"))
          .put(COLUMNS.ORGUNIT_UID, new TableColumn("o", "uid", "ou_uid"))
          .put(COLUMNS.TRACKEDENTITYID, new TableColumn("te", "trackedentityid", "trackedentityid"))
          .put(
              COLUMNS.POTENTIALDUPLICATE,
              new TableColumn("te", "potentialduplicate", "potentialduplicate"))
          .build();

  public static String getQuery() {
    return getSelect()
        + "FROM trackedentity te "
        + "join trackedentitytype tet on te.trackedentitytypeid = tet.trackedentitytypeid "
        + "join organisationunit o on te.organisationunitid = o.organisationunitid "
        + "where te.trackedentityid in (:ids)";
  }

  private static String getSelect() {
    return QueryUtils.getSelect(columnMap.values());
  }

  public static String getColumnName(COLUMNS columns) {
    return columnMap.get(columns).getResultsetValue();
  }
}
