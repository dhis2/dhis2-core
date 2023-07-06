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
public class TrackedEntityInstanceQuery {
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
    TRACKEDENTITYINSTANCEID,

    POTENTIALDUPLICATE
  }

  public static Map<COLUMNS, ? extends QueryElement> columnMap =
      ImmutableMap.<COLUMNS, QueryElement>builder()
          .put(COLUMNS.UID, new TableColumn("tei", "uid", "tei_uid"))
          .put(COLUMNS.CREATED, new TableColumn("tei", "created"))
          .put(COLUMNS.CREATEDCLIENT, new TableColumn("tei", "createdatclient"))
          .put(COLUMNS.CREATED_BY, new TableColumn("tei", "createdbyuserinfo"))
          .put(COLUMNS.UPDATED, new TableColumn("tei", "lastupdated"))
          .put(COLUMNS.UPDATEDCLIENT, new TableColumn("tei", "lastupdatedatclient"))
          .put(COLUMNS.LAST_UPDATED_BY, new TableColumn("tei", "lastupdatedbyuserinfo"))
          .put(COLUMNS.INACTIVE, new TableColumn("tei", "inactive"))
          .put(COLUMNS.DELETED, new TableColumn("tei", "deleted"))
          .put(COLUMNS.GEOMETRY, new Function("ST_AsBinary", "tei", "geometry", "geometry"))
          .put(COLUMNS.TYPE_UID, new TableColumn("tet", "uid", "type_uid"))
          .put(COLUMNS.ORGUNIT_UID, new TableColumn("o", "uid", "ou_uid"))
          .put(
              COLUMNS.TRACKEDENTITYINSTANCEID,
              new TableColumn("tei", "trackedentityinstanceid", "trackedentityinstanceid"))
          .put(
              COLUMNS.POTENTIALDUPLICATE,
              new TableColumn("tei", "potentialduplicate", "potentialduplicate"))
          .build();

  public static String getQuery() {
    return getSelect()
        + "FROM trackedentityinstance tei "
        + "join trackedentitytype tet on tei.trackedentitytypeid = tet.trackedentitytypeid "
        + "join organisationunit o on tei.organisationunitid = o.organisationunitid "
        + "where tei.trackedentityinstanceid in (:ids)";
  }

  private static String getSelect() {
    return QueryUtils.getSelect(columnMap.values());
  }

  public static String getColumnName(COLUMNS columns) {
    return columnMap.get(columns).getResultsetValue();
  }
}
